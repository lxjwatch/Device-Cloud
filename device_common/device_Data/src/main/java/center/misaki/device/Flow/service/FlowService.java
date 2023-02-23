package center.misaki.device.Flow.service;

import center.misaki.device.Flow.Flow;
import center.misaki.device.Flow.FlowDto;
import center.misaki.device.Flow.FlowVo;

import java.util.List;
import java.util.Set;

//流程服务接口
public interface FlowService {
    
    //创建流程
    boolean createFlow(FlowDto flowDto,String userInfo);
    
    //启用一个流程
    boolean enableFlow(Integer flowId,String userInfo);
    
    //更新一个流程
    boolean updateFlow(FlowDto flowDto,String userInfo);
    
    //删除一个流程
    boolean deleteFlow(Integer flowId,String userInfo);
    
    //显示一个流程
    String showFlow(Integer flowId);
    
    //显示正在使用的流程
    String showUsingFlow(Integer formId);
    
    //显示这张表单的所有流程
    List<FlowVo> showAllFlow(Integer formId);
    
    //获取流程属性
    Flow.FLowProperty getFlowProperty(Integer flowId);
    
    //获取这张表单中正在使用的流程的id
    Integer getFlowIdFromFormId(Integer formId);
    
    //开始一个流程，返回流程下一节点集合
    List<Flow.Node> startFlow(Integer formId);
    
    //从当前这一节点开始，返回流程上一节点集合
    List<Flow.Node> backFlow(Integer nodeId,Integer flowId);
    
    //从当前这一节点开始，返回流程下一节点集合
    List<Flow.Node> startFlowFromNode(Integer nodeId,Integer flowId);
    
    //返回当前这一节点的所有用户ID集合
    Set<Integer> getUserIds(Flow.Node node,String userInfo);
    
    //返回这一表单的开始节点
    Flow.Node getStartNode(Integer formId);
    
    //返回这一流程的某一ID的节点
    Flow.Node getNodeById(Integer nodeId,Integer flowId);
    
    
}
