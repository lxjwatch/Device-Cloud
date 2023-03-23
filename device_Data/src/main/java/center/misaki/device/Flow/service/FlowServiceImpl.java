package center.misaki.device.Flow.service;

import center.misaki.device.Flow.Flow;
import center.misaki.device.Flow.FlowDto;
import center.misaki.device.Flow.FlowVo;
import center.misaki.device.Flow.WorkLog;
import center.misaki.device.Flow.api.feign.FlowFeignController;
import center.misaki.device.Flow.dao.FlowLogMapper;
import center.misaki.device.Flow.dao.FlowMapper;
import center.misaki.device.Flow.dao.WorkLogMapper;
import center.misaki.device.Form.service.FormService;
import center.misaki.device.base.Result;
import center.misaki.device.utils.UserInfoUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * @author Misaki
 */
@Service
public class FlowServiceImpl implements FlowService {

    private final FlowMapper flowMapper;
    private final FormService formService;
    
    private final FlowLogMapper flowLogMapper;
    private final WorkLogMapper workLogMapper;
    @Autowired
    private FlowFeignController flowFeignController;

    public FlowServiceImpl(FlowMapper flowMapper, FormService formService, FlowLogMapper flowLogMapper, WorkLogMapper workLogMapper) {
        this.flowMapper = flowMapper;
        this.formService = formService;
        this.flowLogMapper = flowLogMapper;
        this.workLogMapper = workLogMapper;
    }

    @Override
    @Transactional
    public boolean createFlow(FlowDto flowDto , String userInfo) {
        Flow flow = new Flow();
        flow.setFormId(flowDto.getFormId());
        flow.setFlowNodes(JSONObject.toJSONString(flowDto.getNodes()));
        flow.setFlowProperty(flowDto.getFlowProperty());
        flow.setViewData(flowDto.getOrigin());
        flow.setTenementId(UserInfoUtil.getTenementId(userInfo));
        auditData(flow,userInfo);
        int i = flowMapper.insert(flow);
        return i > 0;
    }

    @Override
    @Transactional
    public boolean enableFlow(Integer flowId,String userInfo) {
        Flow flow = flowMapper.selectById(flowId);
        //根据该流程的表单ID从数据库中查询所有已启用的流程，除了当前流程。
        List<Flow> flows = flowMapper.selectList(new QueryWrapper<Flow>().eq("form_id", flow.getFormId()).eq("enable", true).ne("id", flowId));
        //将该表单所有已启用的流程设置为禁用（除了当前流程）
        flows.forEach(f -> {
            f.setEnable(false);
            flowMapper.updateById(f);
        });
        //启用当前流程
        flow.setEnable(true);
        int i = flowMapper.updateById(flow);
        //更改表单类型为流程表单
        boolean j = formService.changeFormTypeToFlow(flow.getFormId(), userInfo);
        return i > 0&&j;
    }

    @Override
    public boolean updateFlow(FlowDto flowDto, String userInfo) {
        Flow flow = flowMapper.selectById(flowDto.getFlowId());
        String nodes = JSONObject.toJSONString(flowDto.getNodes());
        if(flow.getFlowNodes().equals(nodes)){
            flow.setFlowNodes(null);
        }else {
            flow.setFlowNodes(nodes);
        }
        if(flow.getFlowProperty().equals(flowDto.getFlowProperty())){
            flow.setFlowProperty(null);
        }else {
            flow.setFlowProperty(flowDto.getFlowProperty());
        }
        if(flow.getViewData().equals(flowDto.getOrigin())){
            flow.setViewData(null);
        }else {
            flow.setViewData(flowDto.getOrigin());
        }
        flow.setEnable(null);
        auditData(flow,userInfo);
        int i = flowMapper.updateById(flow);
        return i > 0;
    }

    @Override
    @Transactional
    public boolean deleteFlow(Integer flowId, String userInfo) {
        int i = flowMapper.deleteById(flowId);
        List<Integer> flowLogsId = flowLogMapper.getIds(flowId);
        if(flowLogsId!=null&&flowLogsId.size()>0){
            flowLogMapper.deleteBatchIds(flowLogsId);
        }
        workLogMapper.delete(new QueryWrapper<WorkLog>().in("flow_log_id", flowLogsId));
        return i > 0;
    }

    @Override
    public String showFlow(Integer flowId) {
        return flowMapper.selectViewData(flowId);
    }

    @Override
    public String showUsingFlow(Integer formId) {
        Flow flow = flowMapper.selectOne(new QueryWrapper<Flow>().eq("form_id", formId).eq("enable", true));
        if(flow == null){
            return null;
        }
        return flow.getViewData();
    }

    @Override
    public List<FlowVo> showAllFlow(Integer formId) {
        List<Flow> flows = flowMapper.selectList(new QueryWrapper<Flow>().eq("form_id", formId));
        List<FlowVo> ans = new ArrayList<>();
        flows.forEach(f->{
            FlowVo flowVo = new FlowVo();
            flowVo.setOrigin(f.getViewData());
            flowVo.setEnable(f.getEnable());
            flowVo.setUpdateTime(f.getUpdateTime());
            flowVo.setCreateTime(f.getCreateTime());
            flowVo.setId(f.getId());
            flowVo.setCreatePerson(f.getCreatePerson());
            flowVo.setUpdatePerson(f.getUpdatePerson());
            ans.add(flowVo);
        });
        //按照创建时间从新到旧排序
        ans.sort((f1,f2)->{
            if(f1.getCreateTime().isAfter(f2.getCreateTime())){
                return -1;
            }else if(f1.getCreateTime().isBefore(f2.getCreateTime())){
                return 1;
            }else {
                return 0;
            }
        });
        return ans;
    }

    @Override
    public Flow.FLowProperty getFlowProperty(Integer flowId) {
        String flowProperty = flowMapper.selectFlowProperty(flowId);
        if(flowProperty == null){
            return null;
        }
        return JSON.parseObject(flowProperty,Flow.FLowProperty.class);
    }

    @Override
    public Integer getFlowIdFromFormId(Integer formId) {
        return flowMapper.selectFlowIdForFormId(formId);
    }

    @Override
    public List<Flow.Node> startFlow(Integer formId) {
        Flow flow = flowMapper.selectOne(new QueryWrapper<Flow>().eq("form_id", formId).eq("enable", true));
        // 将流程节点转换成Map，方便后面的查找（key=NodeId）
        Map<Integer, Flow.Node> nodeMap = convertToMap(JSON.parseArray(flow.getFlowNodes(), Flow.Node.class));
        // 获取起始节点
        Flow.Node node = nodeMap.get(-1);
        // 获取起始节点的下一步节点ID列表
        Integer[] downIds = node.getDownIds();
        // 遍历下一步节点ID列表，将节点添加到结果列表中
        List<Flow.Node> ans = new ArrayList<>();
        for (Integer downId : downIds) {
            ans.add(nodeMap.get(downId));
        }
        return ans;
    }

    @Override
    public List<Flow.Node> backFlow(Integer nodeId, Integer flowId) {
        Flow flow = flowMapper.selectById(flowId);
        Map<Integer, Flow.Node> nodeMap = convertToMap(JSON.parseArray(flow.getFlowNodes(), Flow.Node.class));
        Flow.Node node = nodeMap.get(nodeId);
        Integer[] upIds = node.getUpIds();
        List<Flow.Node> ans = new ArrayList<>();
        for (Integer upId : upIds) {
            ans.add(nodeMap.get(upId));
        }
        return ans;
    }

    @Override
    public List<Flow.Node> startFlowFromNode(Integer nodeId, Integer flowId) {
        Flow flow = flowMapper.selectById(flowId);
        Map<Integer, Flow.Node> nodeMap = convertToMap(JSON.parseArray(flow.getFlowNodes(), Flow.Node.class));
        Flow.Node node = nodeMap.get(nodeId);
        Integer[] downIds = node.getDownIds();
        List<Flow.Node> ans = new ArrayList<>();
        for (Integer downId : downIds) {
            ans.add(nodeMap.get(downId));
        }
        return ans;
    }

    @Override
    public Set<Integer> getUserIds(Flow.Node node,String userInfo) {
        Result<Set<Integer>> headUserIds = flowFeignController.getHeadUserIds(node.getHead(), UserInfoUtil.getToken(userInfo));
        return headUserIds.getData();
    }

    @Override
    public Flow.Node getStartNode(Integer formId) {
        Flow flow = flowMapper.selectOne(new QueryWrapper<Flow>().eq("form_id", formId).eq("enable", true));
        if(flow == null){
            return null;
        }
        Map<Integer, Flow.Node> nodeMap = convertToMap(JSON.parseArray(flow.getFlowNodes(), Flow.Node.class));
        return nodeMap.get(-1);
    }

    @Override
    public Flow.Node getNodeById(Integer nodeId, Integer flowId) {
        Flow flow = flowMapper.selectById(flowId);
        if(flow == null){
            return null;
        }
        Map<Integer, Flow.Node> nodeMap = convertToMap(JSON.parseArray(flow.getFlowNodes(), Flow.Node.class));
        return nodeMap.get(nodeId);
    }

    private Map<Integer,Flow.Node> convertToMap(List<Flow.Node> nodes){
        Map<Integer,Flow.Node> ans = new HashMap<>();
        nodes.forEach(n->{
            ans.put(n.getNodeId(),n);
        });
        return ans;
    }

    //设置好数据审计
    private void auditData(Flow oneData, String userInfo){
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
