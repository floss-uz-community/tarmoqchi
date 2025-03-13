package uz.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.server.domain.enums.RequestType;
import uz.server.domain.model.ForwardInfo;
import uz.server.domain.model.Request;
import uz.server.domain.model.Response;
import uz.server.ws.Forwarder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ForwardController {
    private final Forwarder forwarder;

    @RequestMapping(value = "/**")
    public ResponseEntity<String> handleRequest(
            @RequestBody(required = false) String body,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        if ("websocket".equalsIgnoreCase(servletRequest.getHeader("Upgrade")) &&
                "Upgrade".equalsIgnoreCase(servletRequest.getHeader("Connection"))) {

            log.info("WebSocket handshake detected");

            servletResponse.setStatus(101); // Switching Protocols
            servletResponse.setHeader("Upgrade", "websocket");
            servletResponse.setHeader("Connection", "Upgrade");

            String key = servletRequest.getHeader("Sec-WebSocket-Key");
            if (key != null) {
                String acceptKey = calculateAcceptKey(key);
                servletResponse.setHeader("Sec-WebSocket-Accept", acceptKey);
            }

            return null;
        }

        String subdomain = servletRequest.getHeader("x-subdomain");

        String requestUri = servletRequest.getRequestURI();

        String method = servletRequest.getMethod();
        Map<String, String> headers = getHeaders(servletRequest);

        log.info("Forwarding [{}] request to path: [{}], domain[{}]", method, requestUri, subdomain);
        log.info("Headers: {}", headers);
        log.info("Forward body: {}", body);

        Response response = forwarder.forward(subdomain, Request.builder()
                .forwardInfo(ForwardInfo.builder()
                        .headers(headers)
                        .body(body)
                        .method(method)
                        .path(requestUri)
                        .build())
                .type(RequestType.FORWARD)
                .build());

        log.info("Response received with status: {}", response.getStatus());
        return ResponseEntity.status(response.getStatus()).body(response.getBody());
    }

    private Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }

        return headers;
    }

    private String calculateAcceptKey(String key) {
        String MAGIC_STRING = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest((key + MAGIC_STRING).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to calculate WebSocket accept key", e);
            return "";
        }
    }
}