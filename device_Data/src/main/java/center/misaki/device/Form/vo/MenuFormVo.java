package center.misaki.device.Form.vo;

import lombok.Data;

import java.util.List;

/**
 * @author Misaki
 */
@Data
public class MenuFormVo {
    
    private Integer menuId;
    
    private String menuName;
    
    private List<SimpleFormVo> simpleForms;
    
    @Data
    public static class SimpleFormVo{
        private Integer formId;
        private String formName;
        private Integer type;
    }

    
}
