package uz.server.domain.model;

import lombok.*;
import uz.server.domain.enums.RequestType;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Request {
    private String id;
    private RequestType type;
    private TunnelInfo tunnelInfo;
    private ForwardInfo forwardInfo;
    private String error;
}
