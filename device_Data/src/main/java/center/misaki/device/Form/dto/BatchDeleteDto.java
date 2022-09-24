package center.misaki.device.Form.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Misaki
 * 批量删除数据 DTO 对象
 */
@Data
public class BatchDeleteDto {
    @NotNull
    private Integer formId;
    @NotNull
    private List<Integer> dataIds;
}
