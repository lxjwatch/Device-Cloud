package center.misaki.device.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception caused by accessing forbidden resources.
 * 访问禁止的资源导致异常
 */
public class ForbiddenException extends AbstractDeviceException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
