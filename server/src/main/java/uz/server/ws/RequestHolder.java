package uz.server.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uz.server.domain.model.Response;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestHolder {
    private static final ConcurrentHashMap<String, CompletableFuture<Response>> requests = new ConcurrentHashMap<>();

    public void add(String id, CompletableFuture<Response> future) {
        log.info("Adding request with id: {}", id);
        requests.put(id, future);
    }

    public void remove(String id) {
        log.info("Removing request with id: {}", id);
        requests.remove(id);
    }

    public void complete(Response response) {
        log.info("Completing request with response: {}", response);

        CompletableFuture<Response> remove = requests.remove(response.getRequestId());

        if (remove != null) {
            log.info("Completing request with id: {}", response.getRequestId());
            remove.complete(response);
        }
    }
}