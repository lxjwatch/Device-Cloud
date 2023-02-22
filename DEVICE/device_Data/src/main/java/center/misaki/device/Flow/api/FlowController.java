package center.misaki.device.Flow.api;

import center.misaki.device.Annotation.AuthOnCondition;
import center.misaki.device.Event.FlowNextEvent;
import center.misaki.device.Flow.FlowDto;
import center.misaki.device.Flow.FlowLog;
import center.misaki.device.Flow.FlowVo;
import center.misaki.device.Flow.service.FlowService;
import center.misaki.device.Flow.service.WorkLogService;
import center.misaki.device.base.Result;
import center.misaki.device.utils.UserInfoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Misaki
 * 流程接口
 */
@RestController
@RequestMapping("/flow")
public class FlowController {
    
    private final WorkLogService workLogService;
    private final FlowService flowService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public FlowController(WorkLogService workLogService, FlowService flowService) {
        this.workLogService = workLogService;
        this.flowService = flowService;
    }
    
    
    //用户创建流程
    @AuthOnCondition(NeedSysAdmin = false)
    @PostMapping("/create")
    public Result<?> createFlow(@RequestBody FlowDto flowDto,String userInfo){
        if(flowService.createFlow(flowDto,userInfo)){
            return Result.ok(null,"创建成功");
        }else return Result.error("创建失败");
    }
    
    //用户启用一个流程
    @AuthOnCondition(NeedSysAdmin = false)
    @PostMapping("/enable")
    public Result<?> enableFlow(Integer flowId,String userInfo){
        if(flowService.enableFlow(flowId,userInfo)){
            return Result.ok(null,"启用成功");
        }else return Result.error("启用失败");
    }
    
    //更新一个流程,不推荐使用
    @AuthOnCondition(NeedSysAdmin = false)
    @PostMapping("/update")
    public Result<?> updateFlow(@RequestBody FlowDto flowDto,String userInfo){
        if(flowService.updateFlow(flowDto,userInfo)){
            return Result.ok(null,"更新成功");
        }else return Result.error("更新失败");
    }
    
    //用户删除一个流程
    @AuthOnCondition
    @PostMapping("/delete")
    public Result<?> deleteFlow(Integer flowId,String userInfo){
        if(flowService.deleteFlow(flowId,userInfo)){
            return Result.ok(null,"删除成功");
        }else return Result.error("删除失败");
    }
    
    //展示这张表单的所有流程
    @GetMapping("/showFlow/form")
    public Result<List<FlowVo>> showFlowForm(Integer formId,String userInfo){
        List<FlowVo> flowVos = flowService.showAllFlow(formId);
        return Result.ok(flowVos,"获取成功");
    }
    
    //显示正在使用的流程仅视图
    @GetMapping("/showUsingFlow")
    public Result<String> showUsingFlow(Integer formId,String userInfo){
        String flow = flowService.showUsingFlow(formId);
        return Result.ok(flow,"获取成功");
    }
    
    //显示一个流程仅视图
    @GetMapping("/showFlow")
    public Result<String> showFlow(Integer flowId,String userInfo){
        String flow = flowService.showFlow(flowId);
        return Result.ok(flow,"获取成功");
    }

    //用户通过该流程节点
    @PostMapping("/agree")
    public Result<?> agreeFlowNext(Integer workLogId, String userInfo, @RequestBody FlowLog.Log flowLog) {
        workLogService.updateWorkLog(workLogId,true,userInfo);
        flowLog.setUserName(UserInfoUtil.getUserName(userInfo));
        flowLog.setRes("通过");
        flowLog.setStartTime(workLogService.getStartTime(workLogId));
        flowLog.setEndTime(LocalDateTime.now());
        applicationEventPublisher.publishEvent(new FlowNextEvent(this,workLogId,userInfo,flowLog, true));
        return Result.ok(null,"审批成功");
    }
    
    //用户驳回此节点
    @PostMapping("/reject")
    public Result<?> rejectFlowNext(Integer workLogId, String userInfo, @RequestBody FlowLog.Log flowLog) {
        workLogService.updateWorkLog(workLogId,false,userInfo);
        flowLog.setUserName(UserInfoUtil.getUserName(userInfo));
        flowLog.setRes("驳回");
        flowLog.setStartTime(workLogService.getStartTime(workLogId));
        flowLog.setEndTime(LocalDateTime.now());
        applicationEventPublisher.publishEvent(new FlowNextEvent(this,workLogId,userInfo,flowLog, false));
        return Result.ok(null,"审批成功");
    }
    
    //用户回退此节点
    @PostMapping("/back")
    public Result<?> backFlowNext(Integer workLogId, String userInfo, @RequestBody FlowLog.Log flowLog) {
        workLogService.updateWorkLog(workLogId,false,userInfo);
        flowLog.setUserName(UserInfoUtil.getUserName(userInfo));
        flowLog.setRes("回退");
        flowLog.setStartTime(workLogService.getStartTime(workLogId));
        flowLog.setEndTime(LocalDateTime.now());
        applicationEventPublisher.publishEvent(new FlowNextEvent(this,workLogId,userInfo,flowLog, false));
        return Result.ok(null,"审批成功");
    }
    
    
}
