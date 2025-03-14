package uz.server.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ResponseHolder {
    private static final ConcurrentHashMap<String, StringBuilder> responses = new ConcurrentHashMap<>();

    public void add(String id, String response) {
        log.info("Adding response with id: {}", id);
        if (responses.containsKey(id)) {
            log.info("Appending response with id: {}", id);
            StringBuilder builder = responses.get(id);
            builder.append(response);

            return;
        }

        log.info("Creating response with id: {}", id);
        responses.put(id, new StringBuilder(response));
    }

    public String get(String id) {
        log.info("Getting response with id: {}", id);
        return responses.get(id).toString();
    }

    public void remove(String id) {
        log.info("Removing response with id: {}", id);
        responses.remove(id);
    }
}
