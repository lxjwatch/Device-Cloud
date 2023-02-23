package center.misaki.device.Flow;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Misaki
 */
@Data
public class FlowLog {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    @JSONField(serialize = false)
    private Integer tenementId;
    @JSONField(serialize = false)
    private Integer flowId;
    
    private Integer dataId;
    
    private Boolean state;

    /**
     * @see Log 
     * 其实是 List<Log> 的 json 字符串
     */
    private String log;
    
    private String createPerson;
    
    private String updatePerson;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    @Data
    public static class Log{
        private String userName;
        private String res;
        private String message;
        private String attachment;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String nodeName;
    }
    
}
