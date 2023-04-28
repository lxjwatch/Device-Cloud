package center.misaki.device.AddressBook;

import java.lang.annotation.*;

/**
 * 对于普通管理员做更为细致的权限划分
 * 用于方便控制能管辖和查询的范围
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthScope {
     
     boolean department() default false;
     //不使用ID时是 RoleGroupVo
     boolean role() default false;
     //是否使用方法参数的 相关ID进行控制？
     boolean useId() default true;
     //是否需要编辑
     boolean modify() default false;
     
}
