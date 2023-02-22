package center.misaki.device.Aspect;

import center.misaki.device.utils.StringZipUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

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

    /**
     * 作用：在调接口之前将网关压缩后的用户信息参数进行解压后再传入接口
     * 解压后的userInfo:
     * {
     * "accountNonExpired":true,            //账户是否不过期
     * "accountNonLocked":true,             //账户是否不被封
     * "authorities":[],                    //权限
     * "creater":true,                      //是否为创造者
     * "credentialsNonExpired":true,        //凭证是否不过期
     * "enabled":true,                      //是否启用
     * "normalAdmin":false,                 //是否普通管理员
     * "sysAdmin":true,                     //是否系统管理员
     * "tenementId":1,                      //租户ID
     * "token":"eyJhbGciOiJIUzK0...",       //token令牌
     * "userId":1,"username":"Misaki"       //用户ID和用户名
     * }
     */
    @Around("Get()||Post()")
    public Object controller(ProceedingJoinPoint joinPoint) throws Throwable {
        String[] parameterNames = ((CodeSignature) joinPoint.getSignature()).getParameterNames();
        int index=-1;
        for(int i=0;i<parameterNames.length;i++){
            if(parameterNames[i].equals("userInfo")){
                index=i;
                break;
            }
        }
        Object[] args = joinPoint.getArgs();
        if(index!=-1) {
            args[index]=StringZipUtil.decompressData((String) args[index]);
        }
        for (int i = 0;i<args.length;i++){
            System.out.println("bugbugbug你到底是什么bug："+ args[i]);
        }

        return joinPoint.proceed(args);
    }
    
}
