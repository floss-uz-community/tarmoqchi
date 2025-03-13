package uz.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.server.domain.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByToken(String token);
}