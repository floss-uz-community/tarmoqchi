package uz.server.ws.tcp;

import org.springframework.stereotype.Component;
import uz.server.service.tcp.netty.TcpHandler;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class TcpHandlerHolder {
    private final ConcurrentHashMap<Long, TcpHandler> handlers = new ConcurrentHashMap<>();

    public void add(Long tunnelId, TcpHandler handler) {
        handlers.put(tunnelId, handler);
    }

    public TcpHandler get(Long tunnelId) {
        return handlers.get(tunnelId);
    }

    public void remove(Long tunnelId) {
        handlers.remove(tunnelId);
    }
}
