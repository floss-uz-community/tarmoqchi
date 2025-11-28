package uz.server.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import uz.server.utils.Utils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventManager {
    private final SessionHolder sessionHolder;
    private final UserService userService;
    private final Sender sender;
    private final RequestHolder requestHolder;
    private final TunnelService tunnelService;
    private final ResponseHolder responseHolder;
    private final Utils utils;

    public void onConnectionEstablished(WebSocketSession session) {
        sessionHolder.addSession(session.getId(), session);

        try {
            User user = userService.authorizeWithToken(extractToken(session));
            String customSubdomain = session.getHandshakeHeaders().getFirst("Custom-Subdomain");
            String subdomain = tunnelService.create(session.getId(), user, customSubdomain);
            sendConnectionConfirmation(session.getId(), subdomain);

            log.info("User authorized and tunnel created: userId={}, subdomain={}", user.getId(), subdomain);
        } catch (BaseException e) {
            log.error("Authorization or tunnel creation failed: sessionId={}", session.getId(), e);
            sendErrorMessage(session.getId(), e.getMessage());
        }
    }

    public void onConnectionClosed(WebSocketSession session) {
        Tunnel tunnel = tunnelService.getTunnelBySessionId(session.getId());
        responseHolder.remove(session.getId());
        requestHolder.remove(session.getId());
        tunnelService.deactivate(tunnel.getId());
        sessionHolder.removeSession(session.getId());
    }

    public void onResponseReceived(TextMessage message, String sessionId) {
        log.info("Response received: sessionId={}", sessionId);

        try {
            Response response = utils.parseAndValidateResponse(sessionId, message.getPayload());

            if (response == null) {
                sendErrorMessage(sessionId, "Invalid response: Missing fields");
                return;
            }

            switch (response.getResponseType()){
                case RESPONSE_CHUNK -> {
                    responseHolder.add(sessionId, response.getBody());

                    if (!response.isLast()) {
                        return;
                    }

                    response.setBody(responseHolder.get(sessionId));
                    responseHolder.remove(sessionId);
                }
                case NOT_RUNNING_APP_OF_CLIENT -> {
                    requestHolder.complete(new Response(
                            response.getRequestId(),
                            500,
                            Settings.NOT_RUNNING_APP_OF_CLIENT_HTML,
                            false,
                            ResponseType.NOT_RUNNING_APP_OF_CLIENT,
                            Collections.emptyMap()
                    ));
                    return;
                }
            }

            log.info("Response handled: requestId={}, status={}, bodyLength={}",
                    response.getRequestId(), response.getStatus(),
                    response.getBody() != null ? response.getBody().length() : 0);

            requestHolder.complete(response);
        } catch (IOException e) {
            log.error("Failed to parse response: sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    public Response sendRequestToAgent(String subdomain, Request request) {
        request.setId(UUID.randomUUID().toString());
        log.info("Sending request to CLI: subdomain={}, requestId={}", subdomain, request.getId());

        CompletableFuture<Response> future = new CompletableFuture<>();
        requestHolder.add(request.getId(), future);

        Tunnel tunnel = tunnelService.getTunnelBySubdomain(subdomain);

        sender.send(tunnel.getSessionId(), utils.parseToJson(request));

        try {
            return future.get(60, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            throw new BaseException(Settings.TIMEOUT_HTML);
        } finally {
            requestHolder.remove(request.getId());
            log.info("Request removed from holder: requestId={}", request.getId());
        }
    }

    private String extractToken(WebSocketSession session) {
        List<String> authHeaders = session.getHandshakeHeaders().get("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            throw new BaseException("Authorization required");
        }
        return authHeaders.get(0).replace("Bearer ", "");
    }

    private void sendConnectionConfirmation(String sessionId, String subdomain) {
        Request confirmation = Request.builder()
                .id(UUID.randomUUID().toString())
                .type(RequestType.CREATED)
                .tunnelInfo(new TunnelInfo(String.format("https://%s.tarmoqchi.uz/", subdomain)))
                .build();

        sender.send(sessionId, utils.parseToJson(confirmation));
    }

    private void sendErrorMessage(String sessionId, String errorMessage) {
        Request errorRequest = Request.builder()
                .type(RequestType.ERROR)
                .error(errorMessage)
                .build();

        sender.send(sessionId, utils.parseToJson(errorRequest));
    }
}