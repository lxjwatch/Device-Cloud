package center.misaki.device.Aspect;

import center.misaki.device.AddressBook.AuthScope;
import center.misaki.device.AddressBook.vo.RoleGroupVo;
import center.misaki.device.Auth.SecurityUtils;
import center.misaki.device.Auth.dto.AuthDto;
import center.misaki.device.Auth.dto.JwtUserDto;
import center.misaki.device.exception.ForbiddenException;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Misaki
 */
@Component
@Aspect
@Slf4j
@Order(3)
public class AuthScopeAspect {
    
    @Pointcut("@annotation(center.misaki.device.AddressBook.AuthScope)")
    public void authScope(){}
    
    
    

    @Around("authScope()")
    public Object AroundMethodInvoke(ProceedingJoinPoint joinPoint) throws Throwable{

        //获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //获取方法
        Method method = signature.getMethod();
        //获取注解
        AuthScope annotation = method.getAnnotation(AuthScope.class);

        boolean department = annotation.department();
        boolean role = annotation.role();
        boolean useId = annotation.useId();
        boolean modify = annotation.modify();

        JwtUserDto user = SecurityUtils.getCurrentUser();
        if(user.isCreater()) return joinPoint.proceed();
        if(user.isSysAdmin()) return joinPoint.proceed();
        
        
        AuthDto authDto = user.getAuthDto();
        AuthDto.Address addressBook = authDto.getAddressBook();
        AuthDto.Sco scope = authDto.getScope();
        Boolean isWatchDepartment = addressBook.getDepartment();
        Boolean[] isWatchManaRole = addressBook.getRole();

        //运行时异常，不允许访问
        if(department&&!isWatchDepartment) throw new ForbiddenException("不允许访问部门相关信息");
        if(role&&!isWatchManaRole[0]) throw new ForbiddenException("不允许访问角色相关信息");
        if(modify&&!isWatchManaRole[1]) throw new ForbiddenException("不允许修改角色相关信息");

        Map<String, Object> argMap = getNameAndValue(joinPoint);

        if(role && useId){
            Integer roleId = (Integer) argMap.get("roleId");
            if(scope.getRole().contains(-1)) return joinPoint.proceed();
            if(scope.getRole().contains(roleId)){
                return joinPoint.proceed();
            } else throw new ForbiddenException("操作非法，操作的角色不在您的管辖范围内！");
        }
        if(department && useId){
            Integer departmentId = (Integer) argMap.get("departmentId");
            if(scope.getDepartment().contains(-1)) return joinPoint.proceed();
            if(scope.getDepartment().contains(departmentId)) {
                return joinPoint.proceed();
            }else throw new ForbiddenException("操作非法，操作的部门不在您的管辖范围内");
        }
        Object proceed = joinPoint.proceed();
        if(role){
            List<RoleGroupVo> res= (List<RoleGroupVo>) proceed;
            Set<Integer> scopeRole = scope.getRole();
            if(scopeRole.contains(-1)) return proceed;
            return res.stream().filter(a -> {
                Map<Integer, String> roles = a.getRoles();
                Map<Integer, String> ans = roles.entrySet().stream().filter(r -> scopeRole.contains(r.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                if (ans.size() > 0) {
                    a.setRoles(ans);
                    return true;
                } else return false;
            }).collect(Collectors.toList());
        }
        
        //暂时没有涉及到部门的后置处理
        return proceed;
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
