package uz.server.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import uz.server.domain.enums.RequestType;
import uz.server.domain.exception.BaseException;
import uz.server.domain.model.Request;

import java.io.IOException;
import java.nio.ByteBuffer;

@Component
@RequiredArgsConstructor
@Slf4j
public class Sender {
    private final SessionHolder sessionHolder;
    private final ObjectMapper objectMapper;

    public void sendText(String id, String message) {
        log.info("Sending message to sessionHolder with id: {}", id);

        existSession(id);

        try {
            WebSocketSession session = sessionHolder.get(id);
            synchronized (session){
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            log.error("Error while sending message to sessionHolder with id: {}", id, e);
            throw new BaseException("Error while sending message to sessionHolder with id: " + id);
        }
    }
    
    public void sendBinary(String id, ByteBuffer buffer) {
        log.info("Sending binary message to sessionHolder with id: {}", id);

        existSession(id);

        try {
            WebSocketSession session = sessionHolder.get(id);
            synchronized (session){
                session.sendMessage(new BinaryMessage(buffer));
            }
        } catch (IOException e) {
            log.error("Error while sending binary message to sessionHolder with id: {}", id, e);
            throw new BaseException("Error while sending binary message to sessionHolder with id: " + id);
        }
    }

    public void sendError(String id, String message, boolean shutDown) {
        existSession(id);

        log.info("Sending error message to sessionHolder with id: {}", id);
        Request errorRequest = Request.builder()
                .type(RequestType.ERROR)
                .error(message)
                .shutDown(shutDown)
                .build();

        String parsed = parseToJson(errorRequest);
        this.sendText(id, parsed);

        try {
            WebSocketSession session = sessionHolder.get(id);
            synchronized (session){
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            log.error("Error while sending error message to sessionHolder with id: {}", id, e);
            throw new BaseException("Error while sending error message to sessionHolder with id: " + id);
        }
    }

    private String parseToJson(Request request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload");
            throw new BaseException("JSON serialization error");
        }
    }


    private void existSession(String id) {
        if (!sessionHolder.has(id)) {
            log.error("Session not found with id: {}", id);
            throw new BaseException("Session not found with id: " + id);
        }
    }
}
