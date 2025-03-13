package uz.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.server.domain.entity.Tunnel;
import uz.server.domain.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface TunnelRepository extends JpaRepository<Tunnel, Long> {
    Integer countByUserAndActiveTrue(User user);

    Optional<Tunnel> findBySubdomain(String subdomain);

    Optional<Tunnel> findBySessionId(String id);
}