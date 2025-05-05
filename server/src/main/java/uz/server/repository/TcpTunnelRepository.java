package uz.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.server.domain.entity.TcpTunnel;
import uz.server.domain.entity.User;

import java.util.Optional;

public interface TcpTunnelRepository extends JpaRepository<TcpTunnel, Long> {
    int countByUser(User user);

    boolean existsBySessionId(String id);

    Optional<TcpTunnel> findBySessionId(String id);
}
