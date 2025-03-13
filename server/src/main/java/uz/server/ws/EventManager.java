package uz.server.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import uz.server.domain.entity.Tunnel;
import uz.server.domain.entity.User;
import uz.server.domain.enums.RequestType;
import uz.server.domain.exception.BaseException;
import uz.server.domain.model.Request;
import uz.server.domain.model.Response;
import uz.server.domain.model.TunnelInfo;
import uz.server.service.TunnelService;
import uz.server.service.UserService;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventManager {
    private final SessionHolder sessionHolder;
    private final UserService userService;
    private final Sender sender;
    private final ObjectMapper objectMapper;
    private final RequestHolder requestHolder;
    private final TunnelService tunnelService;

    public void onConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connection established: sessionId={}", session.getId());

        sessionHolder.addSession(session.getId(), session);

        String token = extractToken(session);
        User user;
        try {
            user = userService.authorizeSessionWithToken(token);
        } catch (BaseException e) {
            log.error("Error while authorizing user: sessionId={}", session.getId(), e);
            sendErrorMessage(session.getId(), e.getMessage());
            return;
        }

        log.info("User authorized: userId={}", user.getId());

        String subdomain = createTunnel(session, user);
        if (subdomain == null) return;

        log.info("Tunnel created: subdomain={}", subdomain);
        sendConnectionConfirmation(session, subdomain);
    }

    private String extractToken(WebSocketSession session) {
        log.info("Extracting token from session: sessionId={}", session.getId());
        List<String> authorization = session.getHandshakeHeaders().get("Authorization");

        if (authorization == null || authorization.isEmpty()) {
            log.error("Authorization header not found: sessionId={}", session.getId());
            throw new BaseException("Authorization required");
        }

        log.info("Token extracted: sessionId={}", session.getId());
        return authorization.get(0).replaceFirst("Bearer ", "");
    }

    private String createTunnel(WebSocketSession session, User user) {
        try {
            log.info("Creating tunnel: userId={}", user.getId());
            return tunnelService.create(session.getId(), user);
        } catch (BaseException e) {
            log.error("Error while creating tunnel: userId={}", user.getId(), e);
            sendErrorMessage(session.getId(), e.getMessage());
            return null;
        }
    }

    private void sendConnectionConfirmation(WebSocketSession session, String subdomain) {
        log.info("Sending connection confirmation: sessionId={}, subdomain={}", session.getId(), subdomain);

        Request response = Request.builder()
                .id(UUID.randomUUID().toString())
                .type(RequestType.CREATED)
                .tunnelInfo(new TunnelInfo(String.format("https://%s.tarmoqchi.uz/", subdomain)))
                .build();

        sendMessage(session.getId(), response);
    }

    public void onConnectionClosed(WebSocketSession session) {
        log.warn("WebSocket connection closed: sessionId={}", session.getId());

        Tunnel tunnel = tunnelService.getTunnelBySessionId(session.getId());
        tunnelService.deactivate(tunnel.getId());

        sessionHolder.removeSession(session.getId());
    }

    public void onResponseReceived(BinaryMessage message) {
        ByteBuffer payload = message.getPayload();

        byte[] bytes = new byte[payload.remaining()];
        payload.get(bytes);

        ObjectMapper objectMapper = new ObjectMapper();
        Response response;

        try {
            response = objectMapper.readValue(bytes, Response.class);
        } catch (IOException e) {
            log.error("Error while deserializing response: {}", e.getMessage());
            return;
        }

        log.info("Response received: reqId={}, status={}, bodyLength={}",
                response.getRequestId(), response.getStatus(),
                response.getBody() != null ? response.getBody().length() : 0);

        requestHolder.complete(response);
    }

    public Response sendRequestToCLI(String subdomain, Request request) {
        log.info("Sending request to CLI: subdomain={}, request={}", subdomain, request);
        request.setId(UUID.randomUUID().toString());

        CompletableFuture<Response> future = new CompletableFuture<>();
        requestHolder.add(request.getId(), future);

        log.info("Request added to requestHolder: id={}", request.getId());

        Tunnel tunnel = tunnelService.getTunnelBySubdomain(subdomain);
        User user = tunnel.getUser();

        log.info("User found: userId={}", user.getId());

        if (user.getRemainingRequests() == 0) {
            log.error("User has reached the limit of requests: userId={}", user.getId());
            throw new BaseException("You have reached the limit of requests!");
        }

        user.setRemainingRequests(user.getRemainingRequests() - 1);
        userService.update(user);

        log.info("User updated: userId={}", user.getId());

        sendMessage(tunnel.getSessionId(), request);

        try {
            log.info("Waiting for response: id={}", request.getId());
            return future.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("Timeout: No response from CLI: id={}", request.getId());
            throw new BaseException("Timeout: No response from CLI");
        } catch (Exception e) {
            log.error("Error in CLI request: id={}", request.getId(), e);
            throw new BaseException("Error in CLI request");
        } finally {
            log.info("Removing request from requestHolder: id={}", request.getId());
            requestHolder.remove(request.getId());
        }
    }

    private void sendMessage(String sessionId, Request payload) {
        try {
            log.info("Sending message: sessionId={}, payload={}", sessionId, payload);
            String message = objectMapper.writeValueAsString(payload);
            log.info("Request: {}", message);
            sender.send(sessionId, message);
        } catch (JsonProcessingException e) {
            log.error("Error while sending message: sessionId={}, payload={}", sessionId, payload, e);
            throw new BaseException("JSON serialization error");
        }
    }

    private void sendErrorMessage(String sessionId, String errorMessage) {
        log.error("Sending error message: sessionId={}, errorMessage={}", sessionId, errorMessage);
        sendMessage(sessionId, Request.builder()
                .type(RequestType.ERROR)
                .error(errorMessage)
                .build());
    }
}