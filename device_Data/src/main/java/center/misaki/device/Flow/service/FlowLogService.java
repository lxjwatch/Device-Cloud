package center.misaki.device.Flow.service;

import center.misaki.device.Flow.Flow;
import center.misaki.device.Flow.FlowLog;
import center.misaki.device.Flow.dao.FlowLogMapper;
import center.misaki.device.utils.UserInfoUtil;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Misaki
 * 流程日志服务接口
 */
@Service
public class FlowLogService {
    
    //日志LOG
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(FlowLogService.class);
    
    private final FlowLogMapper flowLogMapper;
    
    
    public FlowLogService(FlowLogMapper flowLogMapper) {
        this.flowLogMapper = flowLogMapper;
    }

    /**
     * 开始流程时创建日志
     */
    @Transactional
    public Integer startFlowLog(Integer dataId, Integer flowId, Flow.Node node, String userInfo){
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowId(flowId);
        flowLog.setTenementId(UserInfoUtil.getTenementId(userInfo));
        flowLog.setDataId(dataId);
        FlowLog.Log log = new FlowLog.Log();
        log.setUserName(UserInfoUtil.getUserName(userInfo));
        log.setRes("发起");
        log.setNodeName(node.getNodeName());
        log.setStartTime(LocalDateTime.now());
        log.setEndTime(LocalDateTime.now());
        List<FlowLog.Log> logs = new ArrayList<>();
        logs.add(log);
        flowLog.setLog(JSONObject.toJSONString(logs));
        auditData(flowLog,userInfo);
        flowLogMapper.insert(flowLog);
        return flowLog.getId();
    }

    /**
     * 记录流程日志
     */
    @Transactional
    @Async
    public void logFlowLog(Integer flowLogId,FlowLog.Log log,Flow.Node node,String userInfo){
        log.setUserName(UserInfoUtil.getUserName(userInfo));
        log.setNodeName(node.getNodeName());
        FlowLog flowLog = flowLogMapper.selectById(flowLogId);
        List<FlowLog.Log> logs = JSONObject.parseArray(flowLog.getLog(),FlowLog.Log.class);
        logs.add(log);
        flowLog.setLog(JSONObject.toJSONString(logs));
        auditData(flowLog,userInfo);
        int i = flowLogMapper.updateById(flowLog);
        LOG.info("成功记录流程日志，影响行数：{}",i);
    }

    /**
     * 
     */
    @Transactional
    public Integer getFlowIdByFlowLogId(Integer flowLogId){
       return flowLogMapper.getFlowId(flowLogId);
    }

    /**
     * 标记日志已经完成
     */
    @Transactional
    public void finishFlowLog(Integer flowLogId,String userInfo){
        FlowLog flowLog = flowLogMapper.selectById(flowLogId);
        flowLog.setState(true);//标记为结束
        auditData(flowLog,userInfo);
        int i = flowLogMapper.updateById(flowLog);
        LOG.info("成功标记日志已经完成，影响行数：{}",i);
    }

    /**
     * 删除日志 ,基本不会使用
     */
    @Transactional
    public void deleteFlowLog(Integer flowId,String userInfo){
        int i = flowLogMapper.deleteById(flowId);
        LOG.info("成功删除流程日志，影响行数：{}",i);
    }

    /**
     * 根据日志ID 获取日志
     */
    @Transactional
    public FlowLog getFlowLogById(Integer flowLogId){
        return flowLogMapper.selectById(flowLogId);
    }
    

    //设置好数据审计
    private void auditData(FlowLog oneData, String userInfo){
        if(oneData.getCreatePerson()==null||oneData.getCreatePerson().equals("")){
            oneData.setCreatePerson(UserInfoUtil.getUserName(userInfo));
        }
        if(oneData.getCreateTime()==null){
            oneData.setCreateTime(LocalDateTime.now());
        }
        oneData.setUpdatePerson(UserInfoUtil.getUserName(userInfo));
        oneData.setUpdateTime(LocalDateTime.now());
    }
    
}
