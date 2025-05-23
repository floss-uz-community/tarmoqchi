package uz.server.config;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes
    ) {
        String authHeader = request.getHeaders().getFirst("Authorization");
        String customSubdomain = request.getHeaders().getFirst("Custom-Subdomain");

        if (authHeader != null) {
            attributes.put("Authorization", authHeader);
        }

        if (customSubdomain != null){
            attributes.put("Custom-Subdomain", customSubdomain);
        }

        return true;
    }

    @Override
    public void afterHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Exception exception
    ) {}
}
