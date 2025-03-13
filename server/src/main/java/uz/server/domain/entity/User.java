package uz.server.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

@Entity(name = "users")

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String password;
    private String token;

    @Column(name = "remaining_requests")
    private Integer remainingRequests;

    @Column(name = "connected_to_cli")
    private Boolean connectedToCLI;
}
