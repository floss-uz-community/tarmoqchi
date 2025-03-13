package uz.server.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConnectionHandler extends BinaryWebSocketHandler {
    private final EventManager eventManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Connection established: {}", session.getId());
        eventManager.onConnectionEstablished(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("Connection closed: {}", session.getId());
        eventManager.onConnectionClosed(session);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        log.info("Message received: {}", message.getPayload());
        eventManager.onResponseReceived(message);
    }
}
