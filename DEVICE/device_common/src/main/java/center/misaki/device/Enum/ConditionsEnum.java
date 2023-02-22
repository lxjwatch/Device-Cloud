package center.misaki.device.Enum;

/**
 * @author Misaki
 * 运算条件枚举
 */
public enum ConditionsEnum {
    EQUALS("等于", '='),
    NO_EQUALS("不等于",'#'),
    EQUALS_ANYONE("等于任意一个",'$'),
    NO_EQUALS_ANYONE("不等于任意一个",'*'),
    CONTAINS("包含",'|'),
    NO_CONTAINS("不包含",'-'),
    NULL("为空",'%'),
    NO_NULL("不为空",'@'),
    GREATER("大于",'>'),
    LESS("小于",'<'),
    GREATER_AND_EQUALS("大于等于",']'),
    LESS_AND_EQUALS("小于等于",'['),
    SCOPE("选择范围",'~'),
    DYNAMIC("动态筛选",','),
    CONTAINS_ANYONE("包含任意一个",')'),
    CONTAINS_ALL("同时包含",'(');
    
    public final String name;
    public final Character operator;
    
    ConditionsEnum(String name, Character operator) {
        this.name = name;
        this.operator = operator;
    }
    
    public static ConditionsEnum valueOf(int i){
        return ConditionsEnum.values()[i];
    }
    
    public static ConditionsEnum operatorTo(Character ope){
        ConditionsEnum[] enums = ConditionsEnum.values();
        for (ConditionsEnum anEnum : enums) {
            if(anEnum.operator.equals(ope))return  anEnum;
        }
        return null;
    }
    
}
