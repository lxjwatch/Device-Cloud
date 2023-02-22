package center.misaki.device.Form.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * @author Misaki
 * 数据筛选，筛选条件 DTO 对象
 */
@Data
public class DataScreenDto {

    private static final long serialVersionUID = -99L;
    
    @NotNull
    private Integer formId;
    
    private String restrictType;   //限制类型  or  或者 and

    private List<FieldRestrict> conditions;

    private FixRestrict createPerson;
    
    private FixRestrict createTime;
    
    private FixRestrict updateTime;
    
    
    private Map<String,String> nowValue;
    

    
    @Data
    public static class FieldRestrict{
        private String fieldId;
        private Integer fieldTypeId;
        private Character operator;
        private String operand;
        private Boolean custom;
    }
    
    @Data
    public static class FixRestrict{
        @NotNull
        private Character operator;
        
        private String operand;
    }

    private String linkFieldId;
    
    private String originId;
    
    
    private List<String> fieldIds;
}
