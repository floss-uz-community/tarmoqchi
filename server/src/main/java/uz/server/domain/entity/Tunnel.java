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

    @Column(name = "session_id")
    private String sessionId;

    private String subdomain;

    @ManyToOne
    private User user;
}
