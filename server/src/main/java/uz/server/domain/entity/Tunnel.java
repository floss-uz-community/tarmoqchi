package uz.server.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity(name = "tunnels")

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Tunnel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionId;

    private String subdomain;

    private boolean active;

    @ManyToOne
    private User user;
}
