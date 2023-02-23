package center.misaki.device.Aspect;

import center.misaki.device.Annotation.AuthOnCondition;
import center.misaki.device.exception.ForbiddenException;
import center.misaki.device.utils.UserInfoUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Misaki
 */
@Component
@Aspect
@Slf4j
@Order(3)
public class AdminAspect {

    @Pointcut("@annotation(center.misaki.device.Annotation.AuthOnCondition)")
    public void haveAnnoPer(){};

    @Around("haveAnnoPer()")
    public Object AroundMethodInvoke(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AuthOnCondition annotation = method.getAnnotation(AuthOnCondition.class);

        boolean needCreater = annotation.NeedCreater();
        boolean needNorAdmin = annotation.NeedNorAdmin();
        boolean needSysAdmin = annotation.NeedSysAdmin();

        Map<String, Object> objectMap = getNameAndValue(joinPoint);
        String userInfo=(String)objectMap.get("userInfo");
        JSONObject jwtUser = UserInfoUtil.getObject(userInfo);
        boolean isCreater=jwtUser.getBoolean("creater");
        boolean isSysAdmin=jwtUser.getBoolean("sysAdmin");
        boolean isNorAdmin=jwtUser.getBoolean("normalAdmin");
        log.info("校验权限，该用户：{}，为 {}",jwtUser.getString("username"),isCreater?"系统创造者":isSysAdmin?"系统管理员":isNorAdmin?"普通管理员":"普通用户");
        if(isCreater){
            return joinPoint.proceed();
        }
        if(needCreater){
            throw new ForbiddenException("您不是系统创建者，无法使用此功能。");
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

    /**
     * 获取参数Map集合
     */
    Map<String, Object> getNameAndValue(ProceedingJoinPoint joinPoint) {
        Map<String, Object> param = new HashMap<>();
        Object[] paramValues = joinPoint.getArgs();
        String[] paramNames = ((CodeSignature)joinPoint.getSignature()).getParameterNames();
        for (int i = 0; i < paramNames.length; i++) {
            param.put(paramNames[i], paramValues[i]);
        }
        return param;
    }
    
}
