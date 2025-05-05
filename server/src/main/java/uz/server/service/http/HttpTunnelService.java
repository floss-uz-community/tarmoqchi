package uz.server.service.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.server.config.Settings;
import uz.server.domain.entity.HttpTunnel;
import uz.server.domain.entity.User;
import uz.server.domain.enums.TunnelType;
import uz.server.domain.exception.BaseException;
import uz.server.repository.HttpTunnelRepository;

import java.security.SecureRandom;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpTunnelService {
    private final HttpTunnelRepository repo;

    public HttpTunnel create(String sessionId, User user, String subdomain) {
        log.info("Creating HTTP tunnel: userId={}", user.getId());

        if (repo.countByUser(user) == 3) {
            log.error("HTTP tunnel limit exceeded for user: userId={}", user.getId());
            throw new BaseException(String.format("You cannot create more than %d %s tunnels.", 3, TunnelType.HTTP.name()), true);
        }

        if (subdomain != null && repo.existsBySubdomain(subdomain)) {
            log.error("Subdomain already exists: subdomain={}", subdomain);
            throw new BaseException("The subdomain '" + subdomain + "' already exists.");
        }

        HttpTunnel.Builder builder = new HttpTunnel.Builder().sessionId(sessionId).user(user);

        builder.subdomain(Objects.requireNonNullElseGet(subdomain, this::generateUniqueSubdomain));

        return repo.save(builder.build());
    }

    private String generateUniqueSubdomain() {
        long timestamp = System.currentTimeMillis() % 100000;
        StringBuilder sb = new StringBuilder();
        SecureRandom RANDOM = new SecureRandom();

        sb.append(timestamp);
        for (int i = 0; i < 8; i++) {
            String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    public HttpTunnel getBySubdomain(String subdomain) {
        log.info("Getting HTTP tunnel by subdomain: subdomain={}", subdomain);

        return repo.findBySubdomain(subdomain)
                .orElseThrow(() -> new BaseException(Settings.TUNNEL_NOT_FOUND_HTML));
    }

    public boolean existSessionId(String id) {
        return repo.existsBySessionId(id);
    }

    public void deactivateWithWSSessionId(String id) {
        repo.deleteHttpTunnelBySessionId(id);
    }
}
