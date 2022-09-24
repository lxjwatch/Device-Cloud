package center.misaki.device.Flow;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author Misaki
 */
@Data
public class FlowDto {
    
    private Integer formId;
    @NotNull(message = "流程名称不能为空")
    private Flow.Node[] nodes;
    @NotNull(message = "流程属性不能为空")
    private String flowProperty;
    @NotNull(message = "流程属性不能为空")
    private String origin;
    
    private Integer flowId;
    
}
