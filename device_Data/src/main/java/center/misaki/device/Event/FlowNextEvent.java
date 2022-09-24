package center.misaki.device.Event;

import center.misaki.device.Flow.FlowLog;
import org.springframework.context.ApplicationEvent;

/**
 * @author Misaki
 */
public class FlowNextEvent extends ApplicationEvent {
    
    private final Integer workLogId;
    private final String userInfo;
    private final FlowLog.Log flowLog;
    
    private final Boolean isAgree;
    
    public FlowNextEvent(Object source, Integer workLogId, String userInfo, FlowLog.Log flowLog, Boolean isAgree) {
        super(source);
        this.workLogId = workLogId;
        this.userInfo = userInfo;
        this.flowLog = flowLog;
        this.isAgree = isAgree;
    }
    
    public Integer getWorkLogId() {
        return workLogId;
    }
    
    public String getUserInfo() {
        return userInfo;
    }
    
    public FlowLog.Log getFlowLog() {
        return flowLog;
    }
    
    public Boolean getIsAgree() {
        return isAgree;
    }
    
}
