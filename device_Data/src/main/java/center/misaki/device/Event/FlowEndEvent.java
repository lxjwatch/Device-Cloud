package center.misaki.device.Event;

import org.springframework.context.ApplicationEvent;

/**
 * @author Misaki
 */
public class FlowEndEvent extends ApplicationEvent {
    
    private final Integer dataId;
    private final Integer flowLogId;
    private final String userInfo;
    
    //最后是否正常通过
    private final Boolean isAgree;
    
    public FlowEndEvent(Object source, Integer dataId, Integer flowLogId, String userInfo, Boolean isAgree) {
        super(source);
        this.dataId = dataId;
        this.flowLogId = flowLogId;
        this.userInfo = userInfo;
        this.isAgree = isAgree;
    }
    
    public Integer getDataId() {
        return dataId;
    }
    
    public Integer getFlowLogId() {
        return flowLogId;
    }
    
    public Boolean getIsAgree() {
        return isAgree;
    }
    
    public String getUserInfo() {
        return userInfo;
    }
}
