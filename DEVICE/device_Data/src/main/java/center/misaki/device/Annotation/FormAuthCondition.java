package center.misaki.device.Annotation;

import java.lang.annotation.*;

/**
 * 注解用于标记权限，控制其是否能够访问
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FormAuthCondition {
    
    boolean NeedSubmit() default false;
    
    boolean NeedSubmitSelf() default false;
    
    boolean NeedManage() default false;
    
    boolean NeedWatch()  default false;
    
}
