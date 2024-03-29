package center.misaki.device.exception;

import center.misaki.device.base.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Misaki
 */
//全局异常捕捉处理
@Slf4j
@ControllerAdvice
public class CustomExceptionHandler {

    @ResponseBody
    @ExceptionHandler(value = ForbiddenException.class)
    public Result<?> forbiddenHandler(ForbiddenException ex){
        ex.printStackTrace();
        return Result.error(ex.getMessage());
    }

    
}
