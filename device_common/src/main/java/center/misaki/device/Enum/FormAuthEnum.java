package center.misaki.device.Enum;

/**
 * 表单四种权限枚举
 */
public enum FormAuthEnum {
    
    SUBMIT("submit", 1),
    SUBMIT_SELF("submit_self", 2),
    MANAGE("manage", 3),
    WATCH("watch", 4);
    
    
    public final String name;
    public final Integer operation;

    FormAuthEnum(String name, Integer operation) {
        this.name = name;
        this.operation = operation;
    }
    
    //i是提前规定好的
    public static FieldTypeEnum valueOf(int i){
        return FieldTypeEnum.values()[i-1];
    }
}
