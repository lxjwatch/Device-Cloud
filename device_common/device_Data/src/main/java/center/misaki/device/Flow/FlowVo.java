package center.misaki.device.Flow;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Misaki
 */
@Data
public class FlowVo {
    
    private Integer id;
    
    private String origin;
    
    private Boolean enable;
    
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    private String createPerson;
    
    private String updatePerson;
    
}
