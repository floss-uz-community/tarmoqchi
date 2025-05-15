package uz.server.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import uz.server.domain.dto.GithubUserDTO;

import java.time.LocalDateTime;

@Entity(name = "users")

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    @Id
    private Long id;

    @Column(name = "login")
    private String name;

    private String email;

    @JsonProperty("avatar_url")
    @Column(name = "avatar_url")
    private String avatarUrl;

    private String token;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    public static User from(GithubUserDTO dto) {
        return User.builder()
                .name(dto.name())
                .email(dto.email())
                .avatarUrl(dto.avatar_url())
                .build();
    }
}
