package center.misaki.device.Form.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Misaki
 * 批量修改几个数据的某一字段入惨 DTO
 */
@Data
public class BatchChangeDto {
    
    @NotNull
    private Integer formId;
    
    @NotNull
    private List<Integer> dataId;
    
    @NotNull
    private String fieldId;
    
    @NotNull
    private String newValue;
    
}
