package center.misaki.device.Form.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * @author Misaki
 * 一条数据，接收前端提交过来的数据
 */
@Data
public class OneDataDto {
    @NotNull
    private Integer formId;
    @NotNull
    private Map<String,String> data;
    
    private Integer dataId;
    
    @Data
    public static class OneDataDtoPlus{
        @NotNull
        private Integer formId;
        @NotNull
        private Map<String,String> data;

        private Integer dataId;
        
        private List<String> checkFieldIds;
        
    }
    
}
