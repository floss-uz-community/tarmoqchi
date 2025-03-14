package uz.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.server.domain.entity.Tunnel;
import uz.server.domain.entity.User;

import java.util.Optional;

public interface TunnelRepository extends JpaRepository<Tunnel, Long> {
    Integer countByUser(User user);

    Optional<Tunnel> findBySubdomainIgnoreCase(String subdomain);

    Optional<Tunnel> findBySessionId(String id);
}