package center.misaki.device.Exception;

import center.misaki.device.base.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.provider.NoSuchClientException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class CustomException {
    @ExceptionHandler(NoSuchClientException.class)
    public Result<?> noClientException(NoSuchClientException e){
        log.error("No client with requested id: pcc");
        return Result.error(e.getMessage());
    }
}
