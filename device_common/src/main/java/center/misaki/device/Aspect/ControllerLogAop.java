package center.misaki.device.Aspect;

import center.misaki.device.utils.ServletUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Method;
import java.util.Objects;

@Component
@Aspect
@Slf4j
/**
 * @author Misaki
 * Aop 日志管理
 */
@Order(1)
public class ControllerLogAop {

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void restController(){}

    @Pointcut("@within(org.springframework.stereotype.Controller)")
    public void controller(){}
    
    @Around("controller()||restController()")
    public Object controller(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final Method method = signature.getMethod();
        if(method == null){
            return joinPoint.proceed();
        }
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        ServletRequestAttributes requestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = Objects.requireNonNull(requestAttributes).getRequest();
        final StopWatch watch = new StopWatch(request.getRequestURI());
        watch.start("PrintRequest");
        printRequestLog(request, className, methodName, args);
        watch.stop();
        watch.start(className + "#" + methodName);
        final Object returnObj = joinPoint.proceed();
        watch.stop();
        watch.start("PrintResponse");
        printResponseLog(request, className, methodName, returnObj);
        watch.stop();
       
            log.info("Usage:\n{}", watch.prettyPrint());
            
        return returnObj;
    }
    



    private void printRequestLog(HttpServletRequest request, String clazzName, String methodName,
                                 Object[] args) throws JsonProcessingException {
        log.info("Request URL: [{}], URI: [{}], Request Method: [{}], IP: [{}]",
                request.getRequestURL(),
                request.getRequestURI(),
                request.getMethod(),
                ServletUtils.getClientIP(request));

        if (args == null ) {
            return;
        }

        boolean shouldNotLog = false;
        for (Object arg : args) {
            if (arg == null
                    || arg instanceof HttpServletRequest
                    || arg instanceof HttpServletResponse
                    || arg instanceof MultipartFile
                    || arg.getClass().isAssignableFrom(MultipartFile[].class)) {
                shouldNotLog = true;
                break;
            }
        }

        if (!shouldNotLog) {
            String requestBody = JSON.toJSONString(args);
            log.info("{}.{} Parameters: [{}]", clazzName, methodName, requestBody);
        }
    }




    private void printResponseLog(HttpServletRequest request,String className,String methodName,Object returnObj){
        
            String returnData = "";
            if (returnObj != null) {
                if (returnObj instanceof ResponseEntity) {
                    ResponseEntity<?> responseEntity = (ResponseEntity<?>) returnObj;
                    if (responseEntity.getBody() instanceof Resource) {
                        returnData = "[ BINARY DATA ]";
                    } else if (responseEntity.getBody() != null) {
                        returnData = toString(responseEntity.getBody());
                    }
                } else {
                    returnData = toString(returnObj);
                }
            }
            log.info("{}.{} Response: [{}]", className, methodName, returnData);
        
    }


    @NonNull
    private String toString(@NotNull Object obj){
        Assert.notNull(obj, "Return object must not be null");
        String toString;
        if (obj.getClass().isAssignableFrom(byte[].class) && obj instanceof Resource) {
            toString = "[ BINARY DATA ]";
        } else {
            toString = JSONObject.toJSONString(obj);
        }
        return toString;
    }


}
