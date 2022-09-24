package center.misaki.device.Enum;

/**
 * @author Misaki
 * 表单类型枚举类
 */
public enum FormTypeEnum {
    
    NORMAL("普通表单"),
    FLOW("流程表单");
    
    public final String name;
    FormTypeEnum(String name) {
        this.name = name;
    }
    
}
