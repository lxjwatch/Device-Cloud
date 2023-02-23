package center.misaki.device.Aspect;

import center.misaki.device.Annotation.AuthOnCondition;
import center.misaki.device.Auth.SecurityUtils;
import center.misaki.device.Auth.dto.JwtUserDto;
import center.misaki.device.exception.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @author Misaki
 */
@Component
@Aspect
@Slf4j
@Order(2)
public class AuthPermissionAspect {

    @Pointcut("@annotation(center.misaki.device.Annotation.AuthOnCondition)")
    public void haveAnnoPer(){};
    
    
  
    @Around("haveAnnoPer()")
    public Object AroundMethodInvoke(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AuthOnCondition annotation = method.getAnnotation(AuthOnCondition.class);

        //获取访问被增强的方法需要哪些权限才能访问
        boolean needCreater = annotation.NeedCreater();
        boolean needNorAdmin = annotation.NeedNorAdmin();
        boolean needSysAdmin = annotation.NeedSysAdmin();

        //获取当前用户拥有那些权限
        JwtUserDto user = SecurityUtils.getCurrentUser();
        boolean isCreater= user.isCreater();
        boolean isSysAdmin= user.isSysAdmin();
        boolean isNorAdmin= user.isNormalAdmin();

        //日志记录用户的权限
        log.info("校验权限，该用户：{}，为 {}",user.getUsername(),isCreater?"系统创造者":isSysAdmin?"系统管理员":isNorAdmin?"普通管理员":"普通用户");

        //如果是创造者直接放行
        if(isCreater){
            return joinPoint.proceed();
        }
        if(needCreater){
            throw new  ForbiddenException("您不是系统创建者，无法使用此功能。");
        }

        if(isSysAdmin){
            return joinPoint.proceed();
        }
        if(needSysAdmin){
            throw new  ForbiddenException("您不是系统管理员，无法使用此功能。");
        }

        if(isNorAdmin){
            return joinPoint.proceed();
        }
        if(needNorAdmin){
            throw new  ForbiddenException("您不是普通管理员，无法使用此功能。");
        }
        return joinPoint.proceed();
    }
    
    
    
}
