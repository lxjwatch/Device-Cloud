package center.misaki.device.Annotation;


import java.lang.annotation.*;

/**
 *  该注解可以限制某些条件下禁止访问api
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthOnCondition {
    
 
    boolean NeedCreater() default false;
    

    boolean NeedSysAdmin() default true;
    

    boolean NeedNorAdmin() default true;
    
    
}
