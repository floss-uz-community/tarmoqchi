package uz.server.service.user;

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

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${github.client-id}")
    private String githubClientId;

    @Value("${github.client-secret}")
    private String githubClientSecret;

    public User authorizeSessionWithToken(String token) {
        log.info("Authorizing user with token: {}", token);

        return getByToken(token)
                .orElseThrow(() -> {
                    log.error("Invalid token: {}", token);
                    return new BaseException("Token is incorrect, please check your token and try again!");
                });
    }

    public void authorize(String token) {
        User user = authorizeSessionWithToken(token);
        userRepository.save(user);
        log.info("User connected to CLI successfully: userId={}", user.getId());
    }

    public Optional<User> getByToken(String token) {
        log.debug("Fetching user by token");
        return Optional.ofNullable(userRepository.findByToken(token));
    }

    public Optional<User> findById(Long id) {
        log.debug("Fetching user by ID: {}", id);
        return userRepository.findById(id);
    }

    @Transactional
    public User authorizeWithGithub(String code) {
        String accessToken = fetchAccessTokenFromGithub(code);
        return fetchGithubUserInfo(accessToken);
    }

    private String fetchAccessTokenFromGithub(String code) {
        log.info("Fetching GitHub access token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", githubClientId);
        body.add("client_secret", githubClientSecret);
        body.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://github.com/login/oauth/access_token",
                    request,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("access_token").asText();
        } catch (Exception e) {
            log.error("Failed to fetch access token", e);
            throw new RuntimeException("Failed to fetch access token from GitHub", e);
        }
    }

    private User fetchGithubUserInfo(String accessToken) {
        log.info("Fetching GitHub user info");

        HttpEntity<String> request = new HttpEntity<>(getAuthHeaders(accessToken));

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.github.com/user",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            User githubUser = objectMapper.readValue(response.getBody(), User.class);

            return findById(githubUser.getId())
                    .map(existingUser -> updateExistingUser(existingUser, githubUser))
                    .orElseGet(() -> createNewUser(githubUser, accessToken));

        } catch (Exception e) {
            log.error("Failed to fetch GitHub user info", e);
            throw new RuntimeException("Failed to fetch GitHub user info", e);
        }
    }

    private User updateExistingUser(User existingUser, User githubUser) {
        log.info("Updating existing user: userId={}", existingUser.getId());

        existingUser.setName(githubUser.getName());
        existingUser.setAvatarUrl(githubUser.getAvatarUrl());
        existingUser.setGithubProfile(githubUser.getGithubProfile());

        return userRepository.save(existingUser);
    }

    private User createNewUser(User githubUser, String accessToken) {
        log.info("Creating new user: userId={}", githubUser.getId());

        githubUser.setRemainingRequests(300);
        githubUser.setConnectedToCLI(false);
        githubUser.setToken(UUID.randomUUID().toString());

        if (githubUser.getEmail() == null || githubUser.getEmail().isEmpty()) {
            githubUser.setEmail(fetchPrimaryEmailFromGithub(accessToken));
        }

        return userRepository.save(githubUser);
    }

    private String fetchPrimaryEmailFromGithub(String accessToken) {
        log.info("Fetching primary email from GitHub");

        HttpEntity<String> request = new HttpEntity<>(getAuthHeaders(accessToken));

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            JsonNode emails = objectMapper.readTree(response.getBody());
            for (JsonNode emailNode : emails) {
                if (emailNode.path("primary").asBoolean(false)) {
                    return emailNode.path("email").asText();
                }
            }
            throw new RuntimeException("Primary email not found");
        } catch (Exception e) {
            log.error("Failed to fetch primary email", e);
            throw new RuntimeException("Failed to fetch primary email", e);
        }
    }

    private HttpHeaders getAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }
}