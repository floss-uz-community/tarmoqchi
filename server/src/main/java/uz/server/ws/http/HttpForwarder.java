package uz.server.ws.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uz.server.domain.model.Request;
import uz.server.domain.model.Response;

@Component
@RequiredArgsConstructor
@Slf4j
public class HttpForwarder {
    private final HttpEventManager httpEventManager;

    public Response forward(String subdomain, Request request) {
        log.info("Forwarding request to subdomain: {}", subdomain);
        return httpEventManager.sendRequestToCLI(subdomain, request);
    }
}
