package uz.server.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.server.domain.enums.ResponseType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Response {
    private String requestId;
    private Integer status;
    private String body;
    private boolean last;
    private ResponseType responseType;
}
