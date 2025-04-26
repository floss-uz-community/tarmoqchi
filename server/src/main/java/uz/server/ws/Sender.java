package uz.server.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import uz.server.domain.exception.BaseException;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class Sender {
    private final SessionHolder sessionHolder;

    public void send(String id, String message) throws BaseException {
        log.info("Sending message to sessionHolder with id: {}", id);

        if (!sessionHolder.hasSession(id)) {
            log.error("Session not found with id: {}", id);
            throw new BaseException("Session not found with id: " + id);
        }

        try {
            WebSocketSession session = sessionHolder.getSession(id);
            synchronized (session){
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            log.error("Error while sending message to sessionHolder with id: {}", id, e);
            throw new BaseException("Error while sending message to sessionHolder with id: " + id);
        }
    }
}
