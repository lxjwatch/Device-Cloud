package center.misaki.device.exception;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Base exception of the project.
 * 项目的基本异常
 *
 * @author johnniang
 */
public abstract class AbstractDeviceException extends RuntimeException {

    /**
     * Error errorData.
     */
    private Object errorData;

    public AbstractDeviceException(String message) {
        super(message);
    }

    public AbstractDeviceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Http status code
     *
     * @return {@link HttpStatus}
     */
    @NonNull
    public abstract HttpStatus getStatus();

    @Nullable
    public Object getErrorData() {
        return errorData;
    }

    /**
     * Sets error errorData.
     *
     * @param errorData error data
     * @return current exception.
     */
    @NonNull
    public AbstractDeviceException setErrorData(@Nullable Object errorData) {
        this.errorData = errorData;
        return this;
    }
}
