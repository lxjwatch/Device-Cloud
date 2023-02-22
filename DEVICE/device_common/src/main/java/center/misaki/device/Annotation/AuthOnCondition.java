package center.misaki.device.Annotation;


import java.lang.annotation.*;

/**
 *  该注解可以限制某些条件下禁止访问api
 *
 *  @Target： 用于描述注解的使用范围（即：被描述的注解可以用在什么地方）
 *  @Retention： 表示需要在什么级别保存该注释信息，用于描述注解的生命周期（即：被描述的注解在什么范围内有效）
 */
@Target(ElementType.METHOD)//设置该注解只能用于方法注解
@Retention(RetentionPolicy.RUNTIME)//设置该注解运行时保留
@Documented
public @interface AuthOnCondition {
    
 
    boolean NeedCreater() default false;
    

    boolean NeedSysAdmin() default true;
    

    boolean NeedNorAdmin() default true;
    
    
}
