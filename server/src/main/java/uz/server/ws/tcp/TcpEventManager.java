package uz.server.ws.tcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import uz.server.config.Settings;
import uz.server.domain.entity.TcpTunnel;
import uz.server.domain.entity.User;
import uz.server.domain.enums.RequestType;
import uz.server.domain.exception.BaseException;
import uz.server.domain.model.Request;
import uz.server.domain.model.TunnelData;
import uz.server.service.tcp.TcpTunnelService;
import uz.server.service.user.UserService;
import uz.server.ws.Sender;
import uz.server.ws.SessionHolder;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TcpEventManager {
    private final TcpHandlerHolder tcpHandlerHolder;
    private final TcpTunnelService tcpTunnelService;
    private final UserService userService;
    private final SessionHolder sessionHolder;
    private final Sender sender;
    private final ObjectMapper objectMapper;

    public void onConnectionEstablished(WebSocketSession session) {
        try {
            sessionHolder.add(session.getId(), session);
            String authToken = extractAuthHeader(session);
            User user = userService.authorizeSessionWithToken(authToken);

            TcpTunnel tunnel = tcpTunnelService.createTunnel(session.getId(), user);
            sendConnectionConfirmationAndTunnelData(session.getId(), tunnel);
        } catch (BaseException e) {
            sender.sendError(session.getId(), e.getMessage(), true);
        }
    }

    private void sendConnectionConfirmationAndTunnelData(String sessionId, TcpTunnel tunnel) {
        Request request = Request.builder()
                .type(RequestType.CREATED)
                .tunnelData(new TunnelData(String.format(
                        "tcp://%s:%d", Settings.HOST, tunnel.getPort()
                )))
                .build();

        sender.sendText(sessionId, "Tunnel data: " + parseToJson(request));
    }

    public void onMessageReceived(BinaryMessage message, String id) {
    }

    public void onConnectionClosed(WebSocketSession session) {

    }

    private String extractAuthHeader(WebSocketSession session) {
        List<String> strings =
                session.getHandshakeHeaders().get("Authorization");

        if (strings == null || strings.isEmpty()) {
            throw new BaseException("Authorization header not found", true);
        }

        String authHeader = strings.get(0);
        if (authHeader == null || authHeader.isEmpty()) {
            throw new BaseException("Authorization header is empty", true);
        }

        if (!authHeader.startsWith("Bearer ")) {
            throw new BaseException("Invalid Authorization header format", true);
        }

        return authHeader.substring(7);
    }

    private String parseToJson(Request request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload");
            throw new BaseException("JSON serialization error");
        }
    }
}
