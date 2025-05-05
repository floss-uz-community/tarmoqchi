package uz.server.domain.entity;

import lombok.*;
import uz.server.domain.basemodels.Tunnel;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HttpTunnel extends Tunnel {
    private String subdomain;

    public static class Builder {
        private User user;
        private String sessionId;
        private String subdomain;

        public Builder subdomain(String subdomain) {
            this.subdomain = subdomain;
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

        public HttpTunnel build() {
            HttpTunnel httpTunnel = new HttpTunnel();
            httpTunnel.setSubdomain(this.subdomain);
            httpTunnel.setSessionId(this.sessionId);
            httpTunnel.setUser(this.user);
            return httpTunnel;
        }
    }
}
