package uz.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.server.domain.enums.RequestType;
import uz.server.domain.exception.BaseException;
import uz.server.domain.model.ForwardInfo;
import uz.server.domain.model.Request;
import uz.server.domain.model.Response;
import uz.server.ws.Forwarder;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ForwardController {
    private final Forwarder forwarder;

    @RequestMapping(value = "/**", headers = {"Upgrade!=websocket"})
    public ResponseEntity<String> handleRequest(
            @RequestBody(required = false) String body,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String host = servletRequest.getHeader("Host");

        String subdomain = getSubdomain(host);

        if (subdomain.isEmpty()){
            try {
                servletResponse.sendRedirect("https://tarmoqchi.uz/main");
                return ResponseEntity.status(302).body("Redirecting...");
            } catch (IOException e) {
                throw new BaseException("Error while redirecting to main page");
            }
        }

        String requestUri = servletRequest.getRequestURI();
        String queryString = servletRequest.getQueryString();

        if (queryString != null) {
            requestUri += "?" + queryString;
            log.info("Request URI: {}", requestUri);
        }

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

    private static String getSubdomain(String host) {
        log.info("Host: {}", host);

        if (host != null) {
            String[] split = host.split("\\.");

            if (split.length == 3){
                return split[0];
            }
        }

        return "";
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
}