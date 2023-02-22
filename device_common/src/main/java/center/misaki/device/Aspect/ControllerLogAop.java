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

/**
 * @author Misaki
 * Aop 日志管理
 *
 * @Order: 用来控制bean的执行顺序的而并非加载顺序。
 *         AOP 会用到 @Order，如果一个方法被多个 @Around 增强，
 *         可以使用 @Order 指定增强执行顺序
 * @Component: 标注一个类为Spring容器的Bean
 * @Aspect: 设置当前类为AOP切面类
 * @Slf4j: 开启log日志记录功能
 */
@Component
@Aspect
@Slf4j
@Order(1)
public class ControllerLogAop {

    /**
     * 切点：后端接口
     */
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void restController(){}

    /**
     * 切点：后端接口
     */
    @Pointcut("@within(org.springframework.stereotype.Controller)")
    public void controller(){}

    /**
     * 作用：对接口调用进行 日志记录时间 操作
     */
    @Around("controller()||restController()")
    public Object controller(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final Method method = signature.getMethod();
        if(method == null){
            return joinPoint.proceed();
        }
        //获取类名
        String className = joinPoint.getTarget().getClass().getSimpleName();
        //获取方法名
        String methodName = joinPoint.getSignature().getName();
        //获取方法参数
        Object[] args = joinPoint.getArgs();

        //获取请求信息
        ServletRequestAttributes requestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = Objects.requireNonNull(requestAttributes).getRequest();

        final StopWatch watch = new StopWatch(request.getRequestURI());

        //记录打印请求日志的时间
        watch.start("PrintRequest");
        printRequestLog(request, className, methodName, args);
        watch.stop();

        //记录方法执行直到方法返回结果的时间
        watch.start(className + "#" + methodName);
        final Object returnObj = joinPoint.proceed();
        watch.stop();

        //记录打印响应日志的时间
        watch.start("PrintResponse");
        printResponseLog(request, className, methodName, returnObj);
        watch.stop();

        //记录上面三个过程所花的时间
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
