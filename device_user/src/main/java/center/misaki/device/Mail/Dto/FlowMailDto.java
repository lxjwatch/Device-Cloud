package center.misaki.device.Mail.Dto;

import lombok.Data;

/**
 * @author Misaki
 */
@Data
public class FlowMailDto {
    private static final long serialVersionUID = -1L;
    
    private Integer userId;
    
    
    private String formName;
    
    private Boolean isAgree;
    
    
}
