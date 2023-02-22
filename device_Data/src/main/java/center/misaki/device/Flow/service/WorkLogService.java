package center.misaki.device.Flow.service;

import center.misaki.device.Enum.WorkLogEnum;
import center.misaki.device.Flow.Flow;
import center.misaki.device.Flow.WorkLog;
import center.misaki.device.Flow.WorkLogVo;
import center.misaki.device.Flow.dao.WorkLogMapper;
import center.misaki.device.Form.service.impl.FormDataService;
import center.misaki.device.Form.service.impl.FormServiceImpl;
import center.misaki.device.utils.UserInfoUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Misaki
 * 用户工作日志接口
 */
@Service
@Slf4j
public class WorkLogService {

    
    private final WorkLogMapper workLogMapper;
    
    public WorkLogService(WorkLogMapper workLogMapper) {
        this.workLogMapper = workLogMapper;
    }

    /**
     * 流程发起时创建用户工作日志
     */
    @Async
    @Transactional
    public void startFlowWorkLog(Integer dataId,Integer flowLogId,String userInfo,Flow.Node node){
        WorkLog workLog = new WorkLog();
        workLog.setFlowLogId(flowLogId);
        workLog.setAllow(true);
        workLog.setDataId(dataId);
        workLog.setUserId(UserInfoUtil.getUserId(userInfo));
        workLog.setTenementId(UserInfoUtil.getTenementId(userInfo));
        workLog.setNodeId(node.getNodeId());
        
        WorkLog.NodeProperty nodeProperty = new WorkLog.NodeProperty();
        //更多信息 之后有机会迭代再加
        nodeProperty.setNodeMoreProperty("");
        nodeProperty.setFieldAuth(node.getFieldAuth());
        nodeProperty.setNodeName(node.getNodeName());
        workLog.setNodeProperty(JSONObject.toJSONString(nodeProperty));
        
        workLog.setState(WorkLogEnum.ORIGIN.value);
        auditData(workLog);
        int i = workLogMapper.insert(workLog);
        log.info("成功创建 {} 条用户工作日志",i);
    }

    /**
     * 流程流转到某个用户还没处理时创建用户工作日志
     */
    @Transactional
    @Async
    public void flowToUserWorkLog(Set<Integer> userIds, Integer dataId, Integer flowLogId, String userInfo, Flow.Node node){
        int sum= userIds.size();
        int succ=0;
        for (Integer userId : userIds) {
        WorkLog workLog = new WorkLog();
        workLog.setFlowLogId(flowLogId);
        workLog.setAllow(false);
        workLog.setDataId(dataId);
        workLog.setNodeId(node.getNodeId());
        workLog.setTenementId(UserInfoUtil.getTenementId(userInfo));
        workLog.setState(WorkLogEnum.WAIT.value);
        auditData(workLog);
        
        WorkLog.NodeProperty nodeProperty = new WorkLog.NodeProperty();
        //更多信息 之后有机会迭代再加
        nodeProperty.setNodeMoreProperty("");
        nodeProperty.setFieldAuth(node.getFieldAuth());
        nodeProperty.setNodeName(node.getNodeName());
        workLog.setNodeProperty(JSONObject.toJSONString(nodeProperty));
        workLog.setUserId(userId);
        int i = workLogMapper.insert(workLog);
        succ+=i;
        }
        log.info("成功创建 {} 条用户工作日志,一共需要创建 {} 条日志",succ,sum);

    }


    /**
     * 流程流转到某个用户已处理时更新用户工作日志
     */
    @Async
    @Transactional
    public void updateWorkLog(Integer workLogId,boolean allow,String userInfo){
        WorkLog workLog = workLogMapper.selectById(workLogId);
        workLog.setAllow(allow);
        workLog.setState(WorkLogEnum.SOLVED.value);
        auditData(workLog);
        workLogMapper.updateById(workLog);
        
        List<WorkLog> workLogs = workLogMapper.selectList(new QueryWrapper<WorkLog>().eq("node_id", workLog.getNodeId()).eq("flow_log_id", workLog.getFlowLogId())
                .eq("tenement_id", UserInfoUtil.getTenementId(userInfo)).ne("user_id", workLog.getUserId()).eq("state", WorkLogEnum.WAIT.value));
        
        for (WorkLog workLog1 : workLogs) {
            workLog1.setAllow(allow);
            workLog1.setState(WorkLogEnum.SOLVED.value);
            auditData(workLog1);
            workLogMapper.updateById(workLog1);
        }
    }

    /**
     * 流程抄送给某些用户时更新用户工作日志
     */
    @Transactional
    @Async
    public void createCopyLog(Set<Integer> userIds,Integer dataId,Integer flowLogId,String userInfo,Flow.Node node){
        int sum= userIds.size();
        int succ=0;
        for (Integer userId : userIds) {
        WorkLog workLog = new WorkLog();
        workLog.setFlowLogId(flowLogId);
        workLog.setDataId(dataId);
        workLog.setNodeId(node.getNodeId());
        workLog.setTenementId(UserInfoUtil.getTenementId(userInfo));
        workLog.setState(WorkLogEnum.COPY.value);

            WorkLog.NodeProperty nodeProperty = new WorkLog.NodeProperty();
            //更多信息 之后有机会迭代再加
            nodeProperty.setNodeMoreProperty("");
            nodeProperty.setFieldAuth(node.getFieldAuth());
            nodeProperty.setNodeName(node.getNodeName());
            workLog.setNodeProperty(JSONObject.toJSONString(nodeProperty));
        
        
            auditData(workLog);
            workLog.setUserId(userId);
            int i = workLogMapper.insert(workLog);
            succ+=i;
        }
        log.info("成功创建 {} 条用户工作日志,一共需要创建 {} 条日志",succ,sum);
    }

    /**
     * 获取此工作日志的开始时间
     */
    public LocalDateTime getStartTime(Integer workLogId){
        return workLogMapper.getWorkStartTime(workLogId);
    }
    

    /**
     * 通过WorkLogId 获取WorkLog
     */
    @Transactional
    public WorkLog getWorkLogById(Integer workLogId){
        return workLogMapper.selectById(workLogId);
    }

    /**
     * 通过 flow_log_id，获取发起人的UserId;
     */
    @Transactional
    public Integer getStartUserId(Integer flowLogId){
        return workLogMapper.getStartUserId(flowLogId);
    }


    
    @Autowired
    FlowLogService flowLogService;
    @Autowired
    FormServiceImpl formService;
    @Autowired
    FormDataService formDataService;
    /**
     * 查找与我相关的工作日志
     */
    @Transactional
    public List<WorkLogVo> getWorkLogVos(WorkLogEnum workLogEnum,String userInfo){
        //根据user_id 和 state 查询用户所属的工作日志
        List<WorkLog> workLogs = workLogMapper.selectList(new QueryWrapper<WorkLog>().eq("user_id", UserInfoUtil.getUserId(userInfo))
                .eq("state", workLogEnum.value));
        List<WorkLogVo> workLogVos = new ArrayList<>();
        workLogs.forEach(workLog -> {
            //从数据库取出的WorkLog对象数据并不够完整，还需要添加一些属性（变成WorkLogVo）然后返回给前端
            WorkLogVo workLogVo = new WorkLogVo(workLog);
            //获取一张表单的数据
            workLogVo.setOneDataVo(formService.getOneData(workLog.getDataId(),userInfo));
            //获取流程日志(根据workLog表的flow_log_id去flowLog表获取整条流程日志)
            workLogVo.setFlowLog(flowLogService.getFlowLogById(workLog.getFlowLogId()));
            //获取表单名字(从workLog表获取data_id再去formData表获取form_id根据form_id在form表获取formName)
            workLogVo.setFormName(formDataService.getFormNameByDataId(workLog.getDataId()));
            //获取表单id（根据workLog表的data_id去formData表获取form_id）
            workLogVo.setFormId(formDataService.getFormIdByDataId(workLog.getDataId()));
            workLogVos.add(workLogVo);
        });
        return workLogVos;
    }



    //设置好数据审计
    private void auditData(WorkLog oneData){
        if(oneData.getCreateTime()==null){
            oneData.setCreateTime(LocalDateTime.now());
        }
        oneData.setUpdateTime(LocalDateTime.now());
    }
    

}
