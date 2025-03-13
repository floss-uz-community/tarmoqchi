package uz.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.server.domain.entity.User;
import uz.server.domain.exception.BaseException;
import uz.server.repository.UserRepository;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository repo;

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
        log.info("Authorizing user with token: {}", token);

        User user = getByToken(token);

        if (Objects.isNull(user)){
            log.error("Token is incorrect, please check your token and try again!");
            throw new BaseException("Token is incorrect, please check your token and try again!");
        }

        if (user.getConnectedToCLI()) {
            log.error("User already connected to CLI!");
            throw new BaseException("User already connected to CLI!");
        }

        log.info("User authorized: userId={}", user.getId());
        user.setConnectedToCLI(true);
        repo.save(user);
    }

    public User getByToken(String token) {
        log.info("Getting user by token: token={}", token);
        return repo.findByToken(token);
    }

    public void update(User user) {
        log.info("Updating user: userId={}", user.getId());
        repo.save(user);
    }
}
