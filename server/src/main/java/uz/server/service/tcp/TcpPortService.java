package uz.server.service.tcp;

import org.springframework.stereotype.Service;
import uz.server.domain.exception.BaseException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class TcpPortService {
    private static final int MIN_PORT = 50000;
    private static final int MAX_PORT = 51000;

    private final BlockingQueue<Integer> portQueue = new LinkedBlockingQueue<>();

    public TcpPortService() {
        for (int port = MIN_PORT; port <= MAX_PORT; port++) {
            portQueue.offer(port);
        }
    }

    public Integer getAvailablePort() {
        Integer port = portQueue.poll();

        if (port == null) {
            throw new BaseException("No available ports, please wait and try again later");
        }

        return port;
    }

    public void releasePort(Integer port) {
        portQueue.offer(port);
    }
}
