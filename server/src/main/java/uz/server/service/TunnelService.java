package uz.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.server.domain.entity.Tunnel;
import uz.server.domain.entity.User;
import uz.server.domain.exception.BaseException;
import uz.server.repository.TunnelRepository;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TunnelService {
    private final TunnelRepository repo;

    public String create(String sessionId, User user) {
        log.info("Creating tunnel: userId={}", user.getId());
        Integer count = repo.countByUserAndActiveTrue(user);

        if (count == 3){
            log.error("User can't create more than 3 tunnels: userId={}", user.getId());
            throw new BaseException("You can't create more than 3 tunnels!");
        }

        Tunnel save = repo.save(Tunnel.builder()
                .sessionId(sessionId)
                .subdomain(getUniqueString())
                .user(user)
                .build());

        log.info("Tunnel created: subdomain={}", save.getSubdomain());

        return save.getSubdomain();
    }

    private static String getUniqueString() {
        byte[] randomBytes = new byte[8];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public void deactivate(Long tunnelId) {
        log.info("Deactivating tunnel: tunnelId={}", tunnelId);
        Optional<Tunnel> tunnel = repo.findById(tunnelId);

        if (tunnel.isEmpty()){
            log.error("Tunnel not found: tunnelId={}", tunnelId);
            throw new BaseException("Tunnel not found!");
        }

        log.info("Tunnel deactivated and deleting: tunnelId={}", tunnelId);

        repo.deleteById(tunnelId);
    }

    public Tunnel getTunnelBySubdomain(String subdomain) {
        log.info("Getting tunnel by subdomain: subdomain={}", subdomain);
        return repo.findBySubdomain(subdomain).orElseThrow(() -> new BaseException("Tunnel not found!"));
    }

    public Tunnel getTunnelBySessionId(String id) {
        log.info("Getting tunnel by sessionId: sessionId={}", id);
        return repo.findBySessionId(id).orElseThrow(() -> new BaseException("Tunnel not found!"));
    }
}
