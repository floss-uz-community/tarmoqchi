package uz.server.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import uz.server.config.Settings;
import uz.server.domain.entity.Tunnel;
import uz.server.domain.entity.User;
import uz.server.domain.enums.RequestType;
import uz.server.domain.enums.ResponseType;
import uz.server.domain.exception.BaseException;
import uz.server.domain.model.Request;
import uz.server.domain.model.Response;
import uz.server.domain.model.TunnelInfo;
import uz.server.service.TunnelService;
import uz.server.service.UserService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventManager {
    private final SessionHolder sessionHolder;
    private final UserService userService;
    private final Sender sender;
    private final ObjectMapper objectMapper;
    private final RequestHolder requestHolder;
    private final TunnelService tunnelService;
    private final ResponseHolder responseHolder;

    public void onConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connected: sessionId={}", session.getId());

        sessionHolder.addSession(session.getId(), session);

        try {
            User user = authorizeSession(session);
            String subdomain = createTunnel(session, user);
            sendConnectionConfirmation(session, subdomain);

            log.info("User authorized and tunnel created: userId={}, subdomain={}", user.getId(), subdomain);
        } catch (BaseException e) {
            log.error("Authorization or tunnel creation failed: sessionId={}", session.getId(), e);
            sendErrorMessage(session.getId(), e.getMessage());
        }
    }

    public void onConnectionClosed(WebSocketSession session) {
        log.warn("WebSocket disconnected: sessionId={}", session.getId());

        Tunnel tunnel = tunnelService.getTunnelBySessionId(session.getId());
        responseHolder.remove(session.getId());
        requestHolder.remove(session.getId());
        tunnelService.deactivate(tunnel.getId());
        sessionHolder.removeSession(session.getId());
    }

    public void onResponseReceived(TextMessage message, String sessionId) {
        log.info("Response received: sessionId={}", sessionId);

        try {
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
        } catch (IOException e) {
            log.error("Failed to parse response: sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    public Response sendRequestToCLI(String subdomain, Request request) {
        request.setId(UUID.randomUUID().toString());
        log.info("Sending request to CLI: subdomain={}, requestId={}", subdomain, request.getId());

        CompletableFuture<Response> future = new CompletableFuture<>();
        requestHolder.add(request.getId(), future);

        Tunnel tunnel = tunnelService.getTunnelBySubdomain(subdomain);
        User user = tunnel.getUser();

        if (user.getRemainingRequests() <= 0) {
            log.error("User request limit exceeded: userId={}", user.getId());
            throw new BaseException("You have reached the limit of requests!");
        }

        decrementUserRequests(user);
        sendMessage(tunnel.getSessionId(), request);

        try {
            return future.get(60, TimeUnit.SECONDS);
        } catch (CancellationException e) {
            log.error("Request cancelled: requestId={}", request.getId());
            throw new BaseException("Request cancelled");
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

    private User authorizeSession(WebSocketSession session) {
        String token = extractToken(session);
        return userService.authorizeSessionWithToken(token);
    }

    private String extractToken(WebSocketSession session) {
        List<String> authorization = session.getHandshakeHeaders().get("Authorization");

        if (authorization == null || authorization.isEmpty()) {
            throw new BaseException("Authorization required");
        }

        return authorization.get(0).replaceFirst("Bearer ", "");
    }

    private String createTunnel(WebSocketSession session, User user) {
        return tunnelService.create(session.getId(), user);
    }

    private void sendConnectionConfirmation(WebSocketSession session, String subdomain) {
        Request confirmation = Request.builder()
                .id(UUID.randomUUID().toString())
                .type(RequestType.CREATED)
                .tunnelInfo(new TunnelInfo(String.format("https://%s.tarmoqchi.uz/", subdomain)))
                .build();

        sendMessage(session.getId(), confirmation);
    }

    private Response parseAndValidateResponse(String sessionId, String payload) throws JsonProcessingException {
        Response response = objectMapper.readValue(payload, Response.class);

        if (response.getResponseType() == null || response.getRequestId() == null || response.getStatus() == null) {
            sendErrorMessage(sessionId, "Invalid response: Missing fields");
            return null;
        }

        if (response.getBody() == null) {
            response.setBody("");
        }

        return response;
    }

    private void sendMessage(String sessionId, Request payload) {
        try {
            String message = objectMapper.writeValueAsString(payload);
            sender.send(sessionId, message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload: sessionId={}, payload={}", sessionId, payload, e);
            throw new BaseException("JSON serialization error");
        }
    }

    private void sendErrorMessage(String sessionId, String errorMessage) {
        Request errorRequest = Request.builder()
                .type(RequestType.ERROR)
                .error(errorMessage)
                .build();

        sendMessage(sessionId, errorRequest);
    }

    @Async
    public void decrementUserRequests(User user) {
        user.setRemainingRequests(user.getRemainingRequests() - 1);
        userService.update(user);
        log.info("User request count decremented: userId={}, remainingRequests={}", user.getId(), user.getRemainingRequests());
    }
}