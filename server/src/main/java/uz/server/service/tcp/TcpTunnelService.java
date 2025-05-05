package uz.server.service.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.server.domain.entity.TcpTunnel;
import uz.server.domain.entity.User;
import uz.server.domain.enums.TunnelType;
import uz.server.domain.exception.BaseException;
import uz.server.service.tcp.netty.TcpHandler;
import uz.server.ws.tcp.TcpChannelHolder;
import uz.server.ws.tcp.TcpHandlerHolder;
import uz.server.repository.TcpTunnelRepository;
import uz.server.ws.Sender;

@Slf4j
@Service
@RequiredArgsConstructor
public class TcpTunnelService {
    private final static int LIMIT_TUNNEL = 1;

    private final TcpTunnelRepository repo;
    private final TcpPortService tcpPortService;
    private final TcpHandlerHolder tcpHandlerHolder;
    private final Sender sender;
    private final TcpChannelHolder tcpChannelHolder;

    private final EventLoopGroup boss = new NioEventLoopGroup();
    private final EventLoopGroup worker = new NioEventLoopGroup();

    @Transactional
    public TcpTunnel createTunnel(String sessionId, User user){
        if(repo.countByUser(user) == LIMIT_TUNNEL){
            throw new BaseException("You cannot create more than " + LIMIT_TUNNEL + " TCP tunnels.", true);
        }

        Integer availablePort = tcpPortService.getAvailablePort();

        TcpTunnel tcpTunnel = TcpTunnel.builder()
                .port(availablePort)
                .sessionId(sessionId)
                .user(user)
                .build();

        TcpTunnel tunnel = repo.save(tcpTunnel);

        bindPort(tunnel, availablePort);

        return tcpTunnel;
    }

    private void bindPort(TcpTunnel tunnel, Integer availablePort) {
        TcpHandler tcpHandler = new TcpHandler(tunnel, sender, tcpChannelHolder);

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(tcpHandler);

        serverBootstrap.bind(availablePort).addListener(
            future -> {
                if (!future.isSuccess()) {
                    log.error("Failed to bind TCP tunnel to port: {}", availablePort);
                    tcpPortService.releasePort(availablePort);
                    repo.delete(tunnel);
                }
            }
        );

        tcpHandlerHolder.add(tunnel.getId(), tcpHandler);
    }

    public boolean existSessionId(String id) {
        return repo.existsBySessionId(id);
    }

    public void deactivateWithWSSessionId(String id) {
        TcpTunnel tcpTunnel = repo.findBySessionId(id)
                .orElseThrow(() -> new BaseException("TCP tunnel not found"));

        tcpPortService.releasePort(tcpTunnel.getPort());
        repo.delete(tcpTunnel);
        log.info("TCP tunnel deactivated: sessionId={}", id);
    }
}
