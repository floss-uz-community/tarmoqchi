package uz.server.domain.model;

import lombok.*;
import uz.server.domain.enums.RequestType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Request {
    private String id;
    private RequestType type;
    private TunnelData tunnelData;
    private ForwardInfo forwardInfo;
    private String error;
    private boolean shutDown;
}
