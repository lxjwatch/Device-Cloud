package center.misaki.device.Flow;

import center.misaki.device.Form.vo.OneDataVo;
import com.alibaba.fastjson.JSON;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Misaki
 */
@Data
public class WorkLogVo {
    private Integer id;
    
    private Integer userId;

    private Integer state;

    private Integer nodeId;
    
    private Integer dataId;

    private Integer flowLogId;
    
    private String formName;
    
    private Integer formId;

    private Boolean allow;
    
    private WorkLog.NodeProperty nodeProperty;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
    
    private OneDataVo oneDataVo;
    
    private FlowLog flowLog;
    
    public  WorkLogVo(WorkLog workLog){
        this.id = workLog.getId();
        this.userId = workLog.getUserId();
        this.state = workLog.getState();
        this.nodeId = workLog.getNodeId();
        this.dataId = workLog.getDataId();
        this.flowLogId = workLog.getFlowLogId();
        this.allow = workLog.getAllow();
        this.nodeProperty = JSON.parseObject(workLog.getNodeProperty(), WorkLog.NodeProperty.class);
        this.createTime = workLog.getCreateTime();
        this.updateTime = workLog.getUpdateTime();
    }
    
}
