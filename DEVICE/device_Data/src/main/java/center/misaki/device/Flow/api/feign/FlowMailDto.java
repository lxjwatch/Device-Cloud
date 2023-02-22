package center.misaki.device.Flow.api.feign;

import lombok.Data;

/**
 * @author Misaki
 */
@Data
public class FlowMailDto {
    private static final long serialVersionUID = -1L;
    
    private Integer userId;
    
    private Boolean isAgree;
    
    private String formName;
    
    
}
