package uz.server.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import uz.server.ws.http.HttpConnectionHandler;
import uz.server.ws.tcp.TcpConnectionHandler;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final HttpConnectionHandler httpConnectionHandler;
    private final AuthHandshakeInterceptor authHandshakeInterceptor;
    private final TcpConnectionHandler tcpConnectionHandler;


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(tcpConnectionHandler, "/server/tcp")
                .setAllowedOrigins("*")
                .addInterceptors(authHandshakeInterceptor);

        registry.addHandler(httpConnectionHandler, "/server/http")
                .setAllowedOrigins("*")
                .addInterceptors(authHandshakeInterceptor);
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(100 * 1024 * 1024);
        container.setMaxBinaryMessageBufferSize(100 * 1024 * 1024);
        container.setAsyncSendTimeout(60000L);
        return container;
    }
}
