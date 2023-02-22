package center.misaki.device.Flow;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author Misaki
 */
@Data
public class Flow {

    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private Integer tenementId;
    
    private String viewData;
    
    private Integer formId;
    //流程流转节点 节后序列化 
    private String flowNodes;
    //节点更多属性，暂时不管
    private String nodeMoreProperty; 
    //流程属性
    private String flowProperty;
    
    private String createPerson;
    
    private String updatePerson;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    private Boolean enable;
    
    @Data
    public static class Node{
        
        // nodeId 为 -1时则为开始节点，为 -2 则为结束
        private Integer nodeId;
        
        // 1:流程节点  2:抄送节点   3:子流程
        private Integer typeId;
        
        private String nodeName;
        
        private Head head;
        
        private Integer[] downIds;
        
        private Integer[] upIds;
        
        private String fieldAuth;
        
    }
    
    @Data
    public static class Head implements Serializable {
        private static final long serialVersionUID = -2L;
        
        private Integer[] department;
        private Integer[] role;
        private Integer[] user;
    }
    
    @Data
    public static class FLowProperty{
        private Boolean wx;
        private Boolean mail;
        private Boolean withdraw;
        private Boolean cuiBan;
        private Boolean see;
        private Integer rule;
    }
    
    
}
