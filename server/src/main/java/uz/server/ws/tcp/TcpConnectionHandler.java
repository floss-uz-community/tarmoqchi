package uz.server.ws.tcp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

@Component
@RequiredArgsConstructor
public class TcpConnectionHandler extends BinaryWebSocketHandler {
    private final TcpEventManager tcpEventManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session){
        tcpEventManager.onConnectionEstablished(session);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        tcpEventManager.onMessageReceived(message, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        tcpEventManager.onConnectionClosed(session);
    }
}
