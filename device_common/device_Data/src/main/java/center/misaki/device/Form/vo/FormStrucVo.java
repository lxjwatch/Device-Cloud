package center.misaki.device.Form.vo;

import center.misaki.device.domain.Pojo.Form;
import center.misaki.device.Form.pojo.Field;
import lombok.Data;

import java.util.List;

/**
 * @author Misaki
 * 表单结构视图
 */
@Data
public class FormStrucVo {
    private Form form;
    
    private List<Field> fields;
    
    private String fieldsAuth;
    
    
    @Data
    public static class FormSimpleVo{
        private Integer formId;
        private String formName;
        private List<FieldSimpleVo> fieldSimpleVos;
        
        @Data
        public static class FieldSimpleVo{
            private String fieldId;
            private Integer typeId;
            private String fieldName;
        }
    }
    
    
}
