package uz.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.server.domain.enums.RequestType;
import uz.server.domain.exception.BaseException;
import uz.server.domain.model.ForwardInfo;
import uz.server.domain.model.Request;
import uz.server.domain.model.Response;
import uz.server.ws.EventManager;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ForwardController {

  private final EventManager eventManager;

  private static final Set<String> FORBIDDEN_HEADERS = Set.of(
      "host",
      "content-length",
      "transfer-encoding",
      "connection",
      "keep-alive",
      "proxy-authenticate",
      "proxy-authorization",
      "te",
      "trailer",
      "upgrade"
  );

    @RequestMapping(value = "/**", headers = {"Upgrade!=websocket"})
    public ResponseEntity<String> handleRequest(
            @RequestBody(required = false) String body,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        String host = servletRequest.getHeader("Host");
        String subdomain = getSubdomain(host);
        String requestUri = servletRequest.getRequestURI();

        if (subdomain.isEmpty() || Objects.equals(subdomain, "www")) {
            try {
                servletResponse.sendRedirect("https://tarmoqchi.uz/front/");
                return ResponseEntity.status(302).body("Redirecting...");
            } catch (IOException e) {
                throw new BaseException("Error while redirecting to main page");
            }
        }

        String queryString = servletRequest.getQueryString();

        if (queryString != null) {
            requestUri += "?" + queryString;
            log.info("Request URI: {}", requestUri);
        }

        String method = servletRequest.getMethod();
        Map<String, String> headers = getHeaders(servletRequest);

        log.info("Forwarding [{}] request to path: [{}], domain[{}]", method, requestUri, subdomain);

        Response response = eventManager.sendRequestToAgent(subdomain, Request.builder()
                .forwardInfo(ForwardInfo.builder()
                        .headers(headers)
                        .body(body)
                        .method(method)
                        .path(requestUri)
                        .build())
                .type(RequestType.FORWARD)
                .build());

        log.info("Response received with status: {}", response.getStatus());

        HttpHeaders httpHeaders = new HttpHeaders();
        response.getHeaders().forEach(httpHeaders::add);
        return ResponseEntity.status(response.getStatus()).headers(httpHeaders).body(response.getBody());
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
        if (FORBIDDEN_HEADERS.contains(headerName.toLowerCase())) {
          continue;
        }
        headers.put(headerName, request.getHeader(headerName));
      }

      return headers;
    }
}