package uz.model;

import uz.model.enums.RequestType;

public class Request {
    private String id;
    private RequestType type;
    private TunnelInfo tunnelInfo;
    private ForwardInfo forwardInfo;
    private String error;

    public Request() {
    }

    public Request(String id, RequestType type, TunnelInfo tunnelInfo, ForwardInfo forwardInfo, String error) {
        this.id = id;
        this.type = type;
        this.tunnelInfo = tunnelInfo;
        this.forwardInfo = forwardInfo;
        this.error = error;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public TunnelInfo getTunnelInfo() {
        return tunnelInfo;
    }

    public void setTunnelInfo(TunnelInfo tunnelInfo) {
        this.tunnelInfo = tunnelInfo;
    }

    public ForwardInfo getForwardInfo() {
        return forwardInfo;
    }

    public void setForwardInfo(ForwardInfo forwardInfo) {
        this.forwardInfo = forwardInfo;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
