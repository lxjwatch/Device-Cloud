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
     * ���ã��ڵ��ӿ�֮ǰ������ѹ������û���Ϣ�������н�ѹ���ٴ���ӿ�
     * ��ѹ���userInfo:
     * {
     * "accountNonExpired":true,            //�˻��Ƿ񲻹���
     * "accountNonLocked":true,             //�˻��Ƿ񲻱���
     * "authorities":[],                    //Ȩ��
     * "creater":true,                      //�Ƿ�Ϊ������
     * "credentialsNonExpired":true,        //ƾ֤�Ƿ񲻹���
     * "enabled":true,                      //�Ƿ�����
     * "normalAdmin":false,                 //�Ƿ���ͨ����Ա
     * "sysAdmin":true,                     //�Ƿ�ϵͳ����Ա
     * "tenementId":1,                      //�⻧ID
     * "token":"eyJhbGciOiJIUzK0...",       //token����
     * "userId":1,"username":"Misaki"       //�û�ID���û���
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
            System.out.println("bugbugbug�㵽����ʲôbug��"+ args[i]);
        }

        return joinPoint.proceed(args);
    }
    
}
