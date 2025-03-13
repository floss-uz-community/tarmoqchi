package uz.server.domain.model;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ForwardInfo {
    private String method;
    private String path;
    private Map<String, String> headers;
    private String body;
}
