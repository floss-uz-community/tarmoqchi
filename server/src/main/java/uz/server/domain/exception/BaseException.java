package uz.server.domain.exception;


public class BaseException extends RuntimeException {
    // this field is using for shutting down the CLI
    private final boolean shutDown;

    public BaseException(String message, boolean shutDown) {
        super(message);
        this.shutDown = shutDown;
    }

    public BaseException(String message) {
        this(message, false);
    }

    public boolean isShutDown() {
        return shutDown;
    }
}
