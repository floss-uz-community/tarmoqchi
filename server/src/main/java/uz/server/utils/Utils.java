package uz.server.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import uz.server.domain.exception.BaseException;
import uz.server.domain.model.Request;
import uz.server.domain.model.Response;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class Utils {
    private final ObjectMapper objectMapper;

    public String parseToJson(Request payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BaseException("JSON serialization error");
        }
    }

    public Response parseAndValidateResponse(String sessionId, String payload) throws JsonProcessingException {
        Response response = objectMapper.readValue(payload, Response.class);

        if (response.getResponseType() == null || response.getRequestId() == null || response.getStatus() == null) {
            return null;
        }

        response.setBody(Objects.requireNonNullElse(response.getBody(), ""));

        return response;
    }
}
