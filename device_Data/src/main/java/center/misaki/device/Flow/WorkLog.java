package center.misaki.device.Flow;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Misaki
 */
@Data
public class WorkLog {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private Integer tenementId;
    
    private Integer userId;
    
    private Integer state;
    
    private Integer nodeId;
    
    private Integer dataId;
    
    private Integer flowLogId;
    
    private Boolean allow;

    /**
     * @see NodeProperty
     */
    private String nodeProperty;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    @Data
    public static class NodeProperty{
        private String nodeName;
        private String fieldAuth;
        private String nodeMoreProperty;
    }
    
}
