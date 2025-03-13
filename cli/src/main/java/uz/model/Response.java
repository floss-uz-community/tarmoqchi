package uz.model;

public class Response {
    private String requestId;
    private int status;
    private String body;

    public Response(String requestId, int status, String body) {
        this.requestId = requestId;
        this.status = status;
        this.body = body;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
