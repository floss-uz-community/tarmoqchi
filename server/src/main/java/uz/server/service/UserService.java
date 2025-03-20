package uz.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uz.server.domain.entity.User;
import uz.server.domain.exception.BaseException;
import uz.server.repository.UserRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository repo;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${github.client-id}")
    private String githubClientId;

    @Value("${github.client-secret}")
    private String githubClientSecret;

    public User authorizeSessionWithToken(String token) {
        log.info("Authorizing user with token: {}", token);

        User user = getByToken(token);

        if (Objects.isNull(user)){
            log.error("Token is incorrect, please check your token and try again!");
            throw new BaseException("Token is incorrect, please check your token and try again!");
        }

        log.info("User authorized: userId={}", user.getId());
        return user;
    }

    public void authorize(String token) {
        User user = authorizeSessionWithToken(token);

        if (user.getConnectedToCLI()) {
            log.error("User's token already connected to CLI!");
            throw new BaseException("User already connected to CLI!");
        }

        user.setConnectedToCLI(true);
        repo.save(user);
    }

    public User getByToken(String token) {
        log.info("Getting user by token: token={}", token);
        return repo.findByToken(token);
    }

    public User findById(Long id) {
        log.info("Getting user by id: userId={}", id);
        return repo.findById(id).orElse(null);
    }

    public void update(User user) {
        log.info("Updating user: userId={}", user.getId());
        repo.save(user);
    }

    @Transactional
    public User authorizeWithGithub(String code) {
        log.info("Authorizing user with GitHub: code={}", code);
        return getGithubUserInfo(getAccessToken(code));
    }

    public User getGithubUserInfo(String accessToken) {
        log.info("Getting GitHub user info: accessToken={}", accessToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>("", headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "https://api.github.com/user",
                HttpMethod.GET,
                entity,
                String.class
        );

        try {
            log.info("response.getBody()={}", response.getBody());
            User user = objectMapper.readValue(response.getBody(), User.class);

            User userById = findById(user.getId());

            if (userById != null){
                log.info("User already exists: userId={}", user.getId());

                userById.setAvatarUrl(user.getAvatarUrl());
                userById.setGithubProfile(user.getGithubProfile());
                userById.setName(user.getName());
                return userById;
            }

            log.info("User not found, creating new user: userId={}", user.getId());
            user.setRemainingRequests(300);
            user.setConnectedToCLI(false);
            user.setToken(UUID.randomUUID().toString());

            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                log.info("User email not found, getting primary email from GitHub: userId={}", user.getId());
                user.setEmail(getPrimaryEmail(accessToken));
            }

            log.info(user.getAvatarUrl());

            repo.save(user);

            log.info("User created: userId={}", user.getId());
            return user;
        } catch (Exception e) {
            log.error("Failed to parse GitHub user info: accessToken={}", accessToken);
            log.error("Error: {}", e.getMessage());
            throw new RuntimeException("Failed to parse GitHub user info", e);
        }
    }

    private String getPrimaryEmail(String accessToken) {
        log.info("Getting primary email from GitHub: accessToken={}", accessToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>("", headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "https://api.github.com/user/emails",
                HttpMethod.GET,
                entity,
                String.class
        );

        try {
            log.info("GitHub user emails received: accessToken={}", accessToken);
            JsonNode root = objectMapper.readTree(response.getBody());
            for (JsonNode email : root) {
                if (email.path("primary").asBoolean()) {
                    log.info("Primary email found: email={}", email.path("email").asText());
                    return email.path("email").asText();
                }
            }

            log.error("Primary email not found: accessToken={}", accessToken);
            return null;
        } catch (Exception e) {
            log.error("Failed to get GitHub user email: accessToken={}", accessToken);
            throw new RuntimeException("Failed to get GitHub user email", e);
        }
    }

    private String getAccessToken(String code) {
        log.info("Getting GitHub access token: code={}", code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", "application/json");

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", githubClientId);
        map.add("client_secret", githubClientSecret);
        map.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "https://github.com/login/oauth/access_token",
                request,
                String.class
        );

        try {
            log.info("GitHub access token received: code={}", code);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("access_token").asText();
        } catch (Exception e) {
            log.error("Failed to parse GitHub access token: code={}", code);
            throw new RuntimeException("Failed to parse GitHub access token", e);
        }
    }

    public List<User> getUsers() {
        return repo.findAll();
    }
}
