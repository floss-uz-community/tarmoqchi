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
import uz.server.domain.dto.GithubUserDTO;
import uz.server.domain.entity.User;
import uz.server.domain.exception.BaseException;
import uz.server.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;
import java.util.List;

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

    public User authorizeWithToken(String token) {
        log.info("Authorizing user with token: {}", token);

        return getByToken(token)
                .orElseThrow(() -> {
                    log.error("Invalid token: {}", token);
                    return new BaseException("Token is incorrect, please check your token and try again!");
                });
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
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

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

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BaseException("GitHub access token request failed");
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            if (!root.has("access_token")) {
                throw new BaseException("Access token not found in GitHub response");
            }

            return root.path("access_token").asText();
        } catch (Exception e) {
            log.error("Failed to fetch access token", e);
            throw new BaseException("Failed to fetch access token from GitHub");
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

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BaseException("GitHub user info request failed");
            }

            GithubUserDTO dto = objectMapper.readValue(response.getBody(), GithubUserDTO.class);
            User user = User.from(dto);

            return findById(user.getId())
                    .map(existingUser -> updateExistingUser(existingUser, user))
                    .orElseGet(() -> createNewUser(user, accessToken));

        } catch (Exception e) {
            log.error("Failed to fetch GitHub user info", e);
            throw new BaseException("Failed to fetch GitHub user info");
        }
    }

    private User updateExistingUser(User existingUser, User githubUser) {
        log.info("Updating existing user: userId={}", existingUser.getId());

        if (githubUser.getName() != null) existingUser.setName(githubUser.getName());
        if (githubUser.getAvatarUrl() != null) existingUser.setAvatarUrl(githubUser.getAvatarUrl());

        return userRepository.save(existingUser);
    }

    private User createNewUser(User githubUser, String accessToken) {
        log.info("Creating new user: userId={}", githubUser.getId());

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

            return StreamSupport.stream(emails.spliterator(), false)
                    .filter(emailNode -> emailNode.path("primary").asBoolean(false))
                    .map(emailNode -> emailNode.path("email").asText())
                    .findFirst()
                    .orElseThrow(() -> new BaseException("Primary email not found"));

        } catch (Exception e) {
            log.error("Failed to fetch primary email", e);
            throw new BaseException("Failed to fetch primary email from GitHub");
        }
    }

    private HttpHeaders getAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }
}