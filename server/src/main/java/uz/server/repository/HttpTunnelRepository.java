package uz.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.server.domain.entity.HttpTunnel;
import uz.server.domain.entity.User;

import java.util.Optional;

public interface HttpTunnelRepository extends JpaRepository<HttpTunnel, Long> {

    int countByUser(User user);

    boolean existsBySubdomain(String subdomain);

    Optional<HttpTunnel> findBySubdomain(String subdomain);

    boolean existsBySessionId(String id);

    void deleteHttpTunnelBySessionId(String id);
}
