package center.misaki.device.Aspect;

import center.misaki.device.Annotation.FormAuthCondition;
import center.misaki.device.Enum.FormAuthEnum;
import center.misaki.device.Form.api.feign.FormAuthController;
import center.misaki.device.base.Result;
import center.misaki.device.exception.ForbiddenException;
import center.misaki.device.utils.JSONUtils;
import center.misaki.device.utils.UserInfoUtil;
import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Misaki
 */
@Component
@Aspect
@Slf4j
@Order(4)
public class FormAuthAspect {
    @Autowired
    private  FormAuthController formAuthController;
    
    @Pointcut("@annotation(center.misaki.device.Annotation.FormAuthCondition)")
    public void formAsp(){};
    

    @Around("formAsp()")
    public Object AroundMethodInvoke(ProceedingJoinPoint joinPoint) throws Throwable{
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        FormAuthCondition annotation = method.getAnnotation(FormAuthCondition.class);
        
        boolean needSubmit = annotation.NeedSubmit();
        boolean needSubmitSelf = annotation.NeedSubmitSelf();
        boolean needManage = annotation.NeedManage();
        boolean needWatch = annotation.NeedWatch();

        Map<String, Object> objectMap = getNameAndValue(joinPoint);
        Integer formId = (Integer) objectMap.get("formId");
        if(formId==null){
            formId= (Integer) BeanUtil.beanToMap(objectMap.get("dataDto")).get("formId");
        }

        String userInfo=(String)objectMap.get("userInfo");
        JSONObject jwtUser = UserInfoUtil.getObject(userInfo);
        if(jwtUser.getBoolean("creater")||jwtUser.getBoolean("sysAdmin")){
            return joinPoint.proceed();
        }


        Result<Set<Integer>> result = formAuthController.showAuthForm(formId, UserInfoUtil.getToken(userInfo));
        Set<Integer> operations = result.getData();

        if(
                needSubmit && operations.contains(FormAuthEnum.SUBMIT.operation)  || 
                needSubmitSelf && operations.contains(FormAuthEnum.SUBMIT_SELF.operation)    || 
                needManage  && operations.contains(FormAuthEnum.MANAGE.operation)    || 
                needWatch  && operations.contains(FormAuthEnum.WATCH.operation)
         ){
            return joinPoint.proceed();
        }else throw new ForbiddenException("您的没有进行此项操作的权限！");
        
        
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
