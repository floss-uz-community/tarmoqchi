package uz.server.ws.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import uz.server.config.Settings;
import uz.server.domain.basemodels.Tunnel;
import uz.server.domain.entity.HttpTunnel;
import uz.server.domain.entity.User;
import uz.server.domain.enums.RequestType;
import uz.server.domain.enums.ResponseType;
import uz.server.domain.exception.BaseException;
import uz.server.domain.model.Request;
import uz.server.domain.model.Response;
import uz.server.domain.model.TunnelData;
import uz.server.service.http.HttpTunnelService;
import uz.server.service.user.UserService;
import uz.server.ws.Sender;
import uz.server.ws.SessionHolder;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpEventManager {
    private final SessionHolder sessionHolder;
    private final UserService userService;
    private final Sender sender;
    private final ObjectMapper objectMapper;
    private final HttpRequestHolder requestHolder;
    private final HttpTunnelService tunnelService;
    private final HttpResponseHolder responseHolder;

    public void onConnectionEstablishedWithText(WebSocketSession session) {
        log.info("WebSocket connected: sessionId={}", session.getId());

        sessionHolder.add(session.getId(), session);

        try {
            Map<String, String> headers = extractHeaders(session);

            User user = userService.authorizeSessionWithToken(headers.get("Authorization"));
            HttpTunnel tunnel = tunnelService.create(session.getId(), user, headers.get("Subdomain"));

            sendConnectionConfirmationAndTunnelData(session, tunnel);

            log.info("User authorized and tunnel created: userId={}", user.getId());
        } catch (BaseException e) {
            log.error("Error during connection establishment: sessionId={}, error={}", session.getId(), e.getMessage());
            sender.sendError(session.getId(), e.getMessage(), e.isShutDown());
        }
    }

    private void sendConnectionConfirmationAndTunnelData(WebSocketSession session, HttpTunnel tunnel) {
        Request confirmation = Request.builder()
                .id(UUID.randomUUID().toString())
                .type(RequestType.CREATED)
                .build();

        confirmation.setTunnelData(new TunnelData(String.format("https://%s.%s.uz/", tunnel.getSubdomain(), Settings.HOST)));

        String parsed = parseToJson(confirmation);
        sender.sendText(session.getId(), parsed);
    }

    private String parseToJson(Request request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload");
            throw new BaseException("JSON serialization error");
        }
    }

    public void onConnectionClosed(WebSocketSession session) {
        log.warn("WebSocket disconnected: sessionId={}", session.getId());

        tunnelService.deactivateWithWSSessionId(session.getId());
        responseHolder.remove(session.getId());
        requestHolder.remove(session.getId());
        sessionHolder.remove(session.getId());
    }

    public void onResponseReceived(TextMessage message, String sessionId) {
        log.info("Response received: sessionId={}", sessionId);

        Response response = parseAndValidateResponse(sessionId, message.getPayload());
        if (response == null) return;

        if (response.getResponseType() == ResponseType.RESPONSE_CHUNK) {
            responseHolder.add(sessionId, response.getBody());

            if (response.isLast()) {
                response.setBody(responseHolder.get(sessionId));
                responseHolder.remove(sessionId);
            } else {
                log.info("Chunk received but not last: sessionId={}", sessionId);
                return;
            }
        } else if (response.getResponseType() == ResponseType.NOT_RUNNING_APP_OF_CLIENT) {
            requestHolder.complete(new Response(response.getRequestId(), 500, Settings.NOT_RUNNING_APP_OF_CLIENT_HTML, false, ResponseType.NOT_RUNNING_APP_OF_CLIENT));
            return;
        }

        log.info("Response handled: requestId={}, status={}, bodyLength={}",
                response.getRequestId(), response.getStatus(),
                response.getBody() != null ? response.getBody().length() : 0);

        requestHolder.complete(response);
    }

    public Response sendRequestToCLI(String subdomain, Request request) {
        request.setId(UUID.randomUUID().toString());
        log.info("Sending request to CLI: subdomain={}, requestId={}", subdomain, request.getId());

        CompletableFuture<Response> future = new CompletableFuture<>();
        requestHolder.add(request.getId(), future);

        Tunnel tunnel = tunnelService.getBySubdomain(subdomain);
        sender.sendText(tunnel.getSessionId(), parseToJson(request));

        try {
            return future.get(60, TimeUnit.SECONDS);
        } catch (CancellationException e) {
            log.error("Request cancelled: requestId={}", request.getId());
            throw new BaseException("Request cancelled, Waited too long for response");
        } catch (TimeoutException e) {
            log.error("Request timeout: requestId={}", request.getId());
            throw new BaseException("Timeout: No response from CLI");
        } catch (Exception e) {
            log.error("Unexpected error in request: requestId={}", request.getId(), e);
            throw new BaseException("Error in CLI request");
        } finally {
            requestHolder.remove(request.getId());
            log.info("Request removed from holder: requestId={}", request.getId());
        }
    }

    private Map<String, String> extractHeaders(WebSocketSession session) {
        String[] expectedHeaders = new String[]{"Authorization", "Subdomain"};

        Map<String, String> map = new HashMap<>();

        for (String s : expectedHeaders) {
            List<String> headers = session.getHandshakeHeaders().get(s);

            if (headers != null && !headers.isEmpty()) {
                map.put(s, headers.get(0));
            }
        }

        String authHeader = map.get("Authorization");

        if (authHeader == null) {
            throw new BaseException("Authorization header is missing");
        }

        return map;
    }

    private Response parseAndValidateResponse(String sessionId, String payload) {
        try {
            Response response = objectMapper.readValue(payload, Response.class);

            if (response.getResponseType() == null || response.getRequestId() == null || response.getStatus() == null) {
                sender.sendError(sessionId, "Invalid response: Missing fields", false);
                return null;
            }

            response.setBody(Objects.requireNonNullElse(response.getBody(), ""));

            return response;
        } catch (JsonProcessingException e){
            log.error("Failed to parse response: sessionId={}, error={}", sessionId, e.getMessage());
            sender.sendError(sessionId, "Invalid response format", false);
        }

        return null;
    }
}