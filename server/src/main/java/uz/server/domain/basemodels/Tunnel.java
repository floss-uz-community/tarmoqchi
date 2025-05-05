package uz.server.domain.basemodels;

import jakarta.persistence.*;
import lombok.*;
import uz.server.domain.entity.User;

@Getter
@Setter
@MappedSuperclass
public abstract class Tunnel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id")
    private String sessionId;

    @ManyToOne
    private User user;
}
