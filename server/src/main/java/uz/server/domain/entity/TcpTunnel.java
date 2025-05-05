package uz.server.domain.entity;

import lombok.*;
import uz.server.domain.basemodels.Tunnel;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TcpTunnel extends Tunnel {
    private Integer port;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private User user;
        private String sessionId;
        private Integer port;

        public Builder port(Integer port) {
            this.port = port;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public TcpTunnel build() {
            TcpTunnel tcpTunnel = new TcpTunnel();
            tcpTunnel.setPort(this.port);
            tcpTunnel.setSessionId(this.sessionId);
            tcpTunnel.setUser(this.user);
            return tcpTunnel;
        }
    }
}
