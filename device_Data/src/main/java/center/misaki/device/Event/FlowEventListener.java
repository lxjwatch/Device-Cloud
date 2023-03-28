package center.misaki.device.Event;

import center.misaki.device.Enum.WorkLogEnum;
import center.misaki.device.Flow.Flow;
import center.misaki.device.Flow.FlowLog;
import center.misaki.device.Flow.WorkLog;
import center.misaki.device.Flow.api.feign.FlowFeignController;
import center.misaki.device.Flow.api.feign.FlowMailDto;
import center.misaki.device.Flow.service.FlowLogService;
import center.misaki.device.Flow.service.FlowService;
import center.misaki.device.Flow.service.WorkLogService;
import center.misaki.device.Form.service.impl.FormDataService;
import center.misaki.device.utils.UserInfoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * @author Misaki
 */
@Slf4j
@Configuration
public class FlowEventListener {
    @Autowired
    private FlowService flowService;
    @Autowired
    private FlowLogService flowLogService;
    @Autowired
    private WorkLogService workLogService;
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    private Executor executor;
    @Autowired
    private FlowFeignController flowFeignController;
    
    @Async
    @EventListener(classes = {FlowStartEvent.class})
    @Transactional
    public void onFlowStartEvent(FlowStartEvent event) {
        log.info("---------------------流程发起时开始记录日志------------------");

        // 获取事件中的数据ID、表单ID和用户信息
        Integer dataId = event.getDataId();
        Integer formId = event.getFormId();
        String userInfo = event.getUserInfo();

        // 获取表单的起始节点、流程ID和流程属性
        Flow.Node startNode = flowService.getStartNode(formId);
        Integer flowId = flowService.getFlowIdFromFormId(formId);
        Flow.FLowProperty flowProperty = flowService.getFlowProperty(flowId);

        // 如果流程不存在，则将数据ID作为普通数据处理并返回
        if(flowProperty==null){
            log.error("流程不存在,将数据ID:{}作为普通数据处理",dataId);
            formDataService.changeOneData(dataId);
            return;
        }

        // 启动流程并记录日志
        List<Flow.Node> nextNodes = flowService.startFlow(formId);
        Integer flowLogId = flowLogService.startFlowLog(dataId, flowId,startNode, userInfo);

        // 记录工作日志
        workLogService.startFlowWorkLog(dataId,flowLogId,userInfo,startNode);

        // 遍历下一步节点，并根据节点类型进行相应的处理
        for (Flow.Node node : nextNodes) {
            Set<Integer> userIds = flowService.getUserIds(node, userInfo);
            if(node.getTypeId()==2){// 如果节点类型为抄送，则创建抄送日志并发送邮件（如果设置了邮件通知）
                workLogService.createCopyLog(userIds,dataId,flowLogId,userInfo,node);
                if(flowProperty.getMail()){
                    executor.execute(()->{userIds.forEach(u->{FlowCopySendEmail(dataId,u,flowFeignController,userInfo);});});
                }
            }else {
                workLogService.flowToUserWorkLog(userIds,dataId,flowLogId,userInfo,node);
                if(flowProperty.getMail()){
                    //发送邮件，通知负责人有待办消息
                    executor.execute(()->{userIds.forEach(u->{FlowSendEmail(dataId,u,flowFeignController,userInfo);});});
                }
                
            }
        }
    }
    
    @Async
    @EventListener(classes = {FlowNextEvent.class})
    @Transactional
    public void onFlowNextEvent(FlowNextEvent event){
        log.info("---------------------流程流转时开始记录日志------------------");
        Integer workLogId = event.getWorkLogId();
        WorkLog workLogById = workLogService.getWorkLogById(workLogId);

        //获取数据ID、节点ID、流程日志ID、流程ID
        Integer dataId=workLogById.getDataId();
        Integer nodeId=workLogById.getNodeId();
        Integer flowLogId = workLogById.getFlowLogId();
        Integer flowId = flowLogService.getFlowIdByFlowLogId(flowLogId);

        //获取流程属性
        Flow.FLowProperty flowProperty = flowService.getFlowProperty(flowId);
        if(flowProperty==null){
            log.error("流程不存在,将数据ID:{}作为普通数据处理",dataId);
            formDataService.changeOneData(dataId);
            return;
        }

        //获取流程日志、用户信息、是否同意
        FlowLog.Log flowLog = event.getFlowLog();
        String userInfo = event.getUserInfo();
        Boolean isAgree = event.getIsAgree();

        //记录流程日志
        flowLogService.logFlowLog(flowLogId,flowLog,flowService.getNodeById(nodeId,flowId),userInfo);

        //获取下一个节点
        List<Flow.Node> nextNodes = flowService.startFlowFromNode(nodeId, flowId);
        //如果节点通过就进行下一节点的执行
        if(isAgree){
            //如果该节点是结束节点
            if(nextNodes.stream().anyMatch(node->node.getNodeId()==-2)){
                //抄送节点
                nextNodes.stream().filter(node->node.getTypeId()==2).forEach(n->{
                    Set<Integer> userIds = flowService.getUserIds(n, userInfo);
                    if(flowProperty.getMail()){
                        //发送邮件，通知负责人有抄送消息
                        executor.execute(()->{userIds.forEach(u->{FlowCopySendEmail(dataId,u,flowFeignController,userInfo);});});
                    }
                    if(flowProperty.getWx()){
                        
                    }
                    workLogService.createCopyLog(userIds,dataId,flowLogId,userInfo,n);
                });
                //宣布流程结束
                if(flowProperty.getMail()){
                    //发送邮件，通知已经该数据已经结束审核
                    executor.execute(()->{FlowEndSendEmail(dataId,workLogService.getStartUserId(flowLogId),flowFeignController,userInfo,true);});
                }
                
                if(flowProperty.getWx()){
                    
                }
                //发布 流程结束 事件
                applicationEventPublisher.publishEvent(new FlowEndEvent(this,dataId,flowId,userInfo,true));
            }else{
                //该节点不是结束节点，流程未结束
                nextNodes.forEach(n->{
                    //获取用户ID
                    Set<Integer> userIds = flowService.getUserIds(n, userInfo);

                    //抄送节点
                    if(n.getTypeId()==2){
                        workLogService.createCopyLog(userIds,dataId,flowLogId,userInfo,n);
                        if(flowProperty.getMail()){
                            //发送邮件，通知负责人有抄送消息
                            executor.execute(()->{userIds.forEach(u->{FlowCopySendEmail(dataId,u,flowFeignController,userInfo);});});
                        }
                    }else {
                        //审核节点
                        workLogService.flowToUserWorkLog(userIds,dataId,flowLogId,userInfo,n);
                        if(flowProperty.getMail()){
                            //发送邮件，通知负责人有审核消息
                            executor.execute(()->{userIds.forEach(u->{FlowSendEmail(dataId,u,flowFeignController,userInfo);});});
                        }
                        if(flowProperty.getWx()){

                        }
                    }
                });
            }
        }else{
            //不同意流程
            if(flowLog.getRes().equals("回退")){
                List<Flow.Node> nodes = flowService.backFlow(nodeId, flowId);
                nodes.forEach(n->{
                    Set<Integer> userIds = flowService.getUserIds(n, userInfo);
                    if(flowProperty.getMail()){
                        //发送邮件，通知负责人有待办消息
                        executor.execute(()->{userIds.forEach(u->{FlowSendEmail(dataId,u,flowFeignController,userInfo);});});
                    }
                    if(flowProperty.getWx()){

                    }
                    workLogService.flowToUserWorkLog(userIds,dataId,flowLogId,userInfo,n);
                });
            }else{
                //宣布流程结束
                applicationEventPublisher.publishEvent(new FlowEndEvent(this,dataId,flowId,userInfo,false));
                if(flowProperty.getMail()){
                    //发送邮件，通知流程结束
                    executor.execute(()->{FlowEndSendEmail(dataId,workLogService.getStartUserId(flowLogId),flowFeignController,userInfo,false);});
                }
                if(flowProperty.getWx()){

                }
            }
        }

    }
    
    
    
    @Autowired
    private FormDataService formDataService;
    
    @Async
    @EventListener(classes = {FlowEndEvent.class})
    @Transactional
    public void onFlowEndEvent(FlowEndEvent event){
        log.info("---------------------流程结束时开始记录日志------------------");
        Integer flowLogId = event.getFlowLogId();
        Integer dataId= event.getDataId();
        Boolean isAgree = event.getIsAgree();
        String userInfo = event.getUserInfo();

        //如果同意结束，则将该流程数据变成普通数据
        if(isAgree) formDataService.changeOneData(dataId);
        flowLogService.finishFlowLog(flowLogId,userInfo);
    }
    
    
    
    //发送待办通知
    private void FlowSendEmail(Integer dataId,Integer userId,FlowFeignController f,String userInfo){
        FlowMailDto flowMailDto = new FlowMailDto();
        flowMailDto.setFormName(formDataService.getFormNameByDataId(dataId));
        flowMailDto.setUserId(userId);
        f.flowAdvice(flowMailDto,UserInfoUtil.getToken(userInfo));
    }
    //发送流程结束通知
    private void FlowEndSendEmail(Integer dataId,Integer userId,FlowFeignController f,String userInfo,Boolean isAgree){
        FlowMailDto flowMailDto = new FlowMailDto();
        flowMailDto.setFormName(formDataService.getFormNameByDataId(dataId));
        flowMailDto.setUserId(userId);
        flowMailDto.setIsAgree(isAgree);
        f.flowRejectAdvice(flowMailDto,UserInfoUtil.getToken(userInfo));
    }
    //发送抄送通知
    private void FlowCopySendEmail(Integer dataId,Integer userId,FlowFeignController f,String userInfo){
        FlowMailDto flowMailDto = new FlowMailDto();
        flowMailDto.setFormName(formDataService.getFormNameByDataId(dataId));
        flowMailDto.setUserId(userId);
        f.flowCopyAdvice(flowMailDto,UserInfoUtil.getToken(userInfo));
    }
    
}
