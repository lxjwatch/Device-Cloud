package center.misaki.device.Enum;

/**
 * 表单字段类型枚举
 */
public enum FieldTypeEnum{
    
    //基础字段
    
    ONE_LINE_TEXT("单行文本"),
    MUL_LINE_TEXT("多行文本"),
    FIGURES("数字"),
    DATE_AND_TIME("日期和时间"),
    RADIO_BUTTON_GROUP("单选按钮组"),
    CHECK_BOX_GROUP("复选框组"),
    DROP_DOWN_BOX("下拉框"),
    DROP_DOWN_CHECK_BOX("下拉复选框"),
    DIVIDING_LINE("分割线"),
    
    //增强字段
    ADDRESS("地址"),
    LOCATION("定位"),
    PICTURE("图片"),
    ATTACHMENT("附件"),
    SUBFORM("子表单"),
    ASSOCIATION_QUERY("关联查询"),
    ASSOCIATED_DATA("关联数据"),
    SIGNATURE("手写签名"),
    NO("流水号"),
    PHONE("手机"),
    TEXT_RECOGNITION("文字识别"),
    
    //部门成员字段
    MEMBER_RADIO("成员单选"),
    MULTIPLE_MEMBERS("成员多选"),
    DEPARTMENTAL_RADIO("部门单选"),
    MORE_DEPARTMENTS("部门多选");
    
    
    private final String name;
    
     FieldTypeEnum (String name) {
        this.name = name;
     }

    public String getName() {
        return name;
    }
    
    public static FieldTypeEnum valueOf(int i){
         return FieldTypeEnum.values()[i];
    }
    
    
    
}
