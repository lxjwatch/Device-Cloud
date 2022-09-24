package center.misaki.device.Form.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * @author Misaki
 * 修改表单结构，字段配置提交的Dto
 */
@Data
public class FormStrucDto implements Serializable {
    
    @NotNull
    private Integer formId;
    
//    @NotNull
    private String formName;
    
    @NotNull
    private String properties;
    
    @NotNull
    private List<SubFormDto> subForms;
    
    @Data
    public static class SubFormDto implements Serializable{
        
        private String name;
        private List<FieldStrucDto> fields;
    }
    
    
    @Data
    public static class FieldStrucDto implements Serializable{
        
        private String fieldId;
        @NotNull
        private Integer typeId;
        @NotNull
        private String name;
        /**
         * 字段详细的属性文档，由    序列化而来
         */
        @NotNull
        private String  detailJson;
    }
    
}
