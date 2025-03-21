package uz.server.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ResponseHolder {
    private final ConcurrentHashMap<String, String> responses = new ConcurrentHashMap<>();

    public void add(String sessionId, String value) {
        log.info("Adding response: sessionId={}", sessionId);
        String old = responses.get(sessionId);

        if (old != null){
            log.info("Response already exists: sessionId={}", sessionId);
            responses.put(sessionId, old + value);
        } else {
            responses.put(sessionId, value);
        }
    }

    public String get(String sessionId) {
        log.info("Getting response: sessionId={}", sessionId);
        return responses.get(sessionId);
    }

    public void remove(String sessionId) {
        log.info("Removing response: sessionId={}", sessionId);
        responses.remove(sessionId);
    }
}
