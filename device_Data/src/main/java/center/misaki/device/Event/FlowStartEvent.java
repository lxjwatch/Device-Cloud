package center.misaki.device.Event;

import org.springframework.context.ApplicationEvent;

/**
 * @author Misaki
 */
public class FlowStartEvent extends ApplicationEvent {
    private static final long serialVersionUID = 1L;
    
    private final Integer formId;
    private final Integer dataId;
    private final String userInfo;
    
    public FlowStartEvent(Object source, Integer formId, Integer dataId, String userInfo) {
        super(source);
        this.formId = formId;
        this.dataId = dataId;
        this.userInfo = userInfo;
    }
    
    public Integer getFormId() {
        return formId;
    }
    
    public Integer getDataId() {
        return dataId;
    }
    
    public String getUserInfo() {
        return userInfo;
    }
}
