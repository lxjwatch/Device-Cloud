package center.misaki.device.Aspect;

import center.misaki.device.utils.StringZipUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @author Misaki
 */
@Aspect
@Component
@Order(2)
public class UserInfoAspect {
    @Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping)")
    public void Get(){}

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping)")
    public void Post(){}


    @Around("Get()||Post()")
    public Object controller(ProceedingJoinPoint joinPoint) throws Throwable {
        //获取方法参数名
        String[] parameterNames = ((CodeSignature) joinPoint.getSignature()).getParameterNames();
        int index=-1;
        for(int i=0;i<parameterNames.length;i++){
            if(parameterNames[i].equals("userInfo")){
                index=i;
                break;
            }
        }
        //返回被通知方法参数列表
        Object[] args = joinPoint.getArgs();
        if(index!=-1) {
            args[index]=StringZipUtil.decompressData((String) args[index]);
        }
        return joinPoint.proceed(args);
    }
    
}
