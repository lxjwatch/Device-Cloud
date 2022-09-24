package center.misaki.device.Form.service.impl;

import center.misaki.device.Form.dao.DataLogMapper;
import center.misaki.device.Form.dao.FieldMapper;
import center.misaki.device.Form.dto.BatchChangeDto;
import center.misaki.device.Form.dto.OneDataDto;
import center.misaki.device.Form.pojo.DataModifyLog;
import center.misaki.device.Form.pojo.FormData;
import center.misaki.device.Form.pojo.FormModifyLog;
import center.misaki.device.Form.vo.BatchLogVo;
import center.misaki.device.utils.UserInfoUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * @author Misaki
 */
@Service
@Slf4j
public class DataLogService {
    private final DataLogMapper dataLogMapper;
    private final FieldMapper fieldMapper;

    public DataLogService(DataLogMapper dataLogMapper, FieldMapper fieldMapper) {
        this.dataLogMapper = dataLogMapper;
        this.fieldMapper = fieldMapper;
    }

    /**
     * 根据数据ID获取一条单条数据的日志
     */
    public List<DataModifyLog> getOneDataLog(int dataId){
        return dataLogMapper.selectByDataId(dataId);
    }

    /**
     * 根据 formId获取数据批量操作的日志
     */
    @Transactional
    public List<BatchLogVo> getBatchDataLog(int formId,String userInfo){
        List<FormModifyLog> modifyLogs = dataLogMapper.selectByFormId(formId, UserInfoUtil.getTenementId(userInfo));
        List<BatchLogVo> logVos = new ArrayList<>();
        modifyLogs.forEach(m->{
            Optional<String> name = fieldMapper.selectOneFieldName(m.getFieldId());
            if(name.isPresent()){
                BatchLogVo batchLogVo = new BatchLogVo();
                batchLogVo.setFormModifyLog(m);
                batchLogVo.setFieldName(name.get());
                logVos.add(batchLogVo);
            }
        });
        return logVos;
    }
    
    /**
     * 根据数据ID删除所有日志
     */
    @Transactional
    @Async
    public void deleteDataLog(int dataId){
        int i = dataLogMapper.delete(new QueryWrapper<DataModifyLog>().eq("data_id", dataId));
        log.info("删除了{}条日志",i);
    }
    

    /**
     * 添加一条单条数据更改日志
     */
    @Async
    @Transactional
    public void saveSingleDataLog(OneDataDto oneDataDto, String userInfo, FormData originFormData){
        Map<String, String> newData = oneDataDto.getData();
        Map<String, String> origin = JSON.parseObject(originFormData.getFormData(), new TypeReference<Map<String, String>>() {});
        
        Map<String, String[]> change = new HashMap<>();
        int changeNum=0;
        for (Map.Entry<String, String> entry : newData.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (origin.containsKey(k)) {
                String v1 = origin.get(k);
                if (!v.equals(v1)){
                    change.put(k, new String[]{v1, v});
                    changeNum++;
                }
            } else {
                change.put(k, new String[]{"",v});
                changeNum++;
            }
        }
        if(changeNum==0) return;
        DataModifyLog dataLog = new DataModifyLog();
        dataLog.setDataId(oneDataDto.getDataId());
        dataLog.setChangeNum(changeNum);
        dataLog.setTenementId(UserInfoUtil.getTenementId(userInfo));
        dataLog.setChangeContent(JSON.toJSONString(change, SerializerFeature.WriteNonStringKeyAsString,SerializerFeature.WriteNullStringAsEmpty));
        auditLog(dataLog,userInfo);

        int i = dataLogMapper.insert(dataLog);
        if(i<0) log.error("数据ID为 {} 日志存储失败，具体参数信息 oneDataDto :{},originFormData :{}",oneDataDto.getDataId(),oneDataDto,originFormData);
    }

    /**
     * 添加一条批量修改的日志
     */
    @Async
    @Transactional
    public void saveBatchDataLog(BatchChangeDto batchChangeDto,String userInfo,FormModifyLog formModifyLog){
        formModifyLog.setFormId(batchChangeDto.getFormId());
        formModifyLog.setModifyNum(batchChangeDto.getDataId().size());
        formModifyLog.setFieldId(batchChangeDto.getFieldId());
        formModifyLog.setNewValue(batchChangeDto.getNewValue());
        formModifyLog.setTenementId(UserInfoUtil.getTenementId(userInfo));
        formModifyLog.setFailNum(formModifyLog.getModifyNum()- formModifyLog.getSuccessNum());
        auditLog(formModifyLog,userInfo);
        int i = dataLogMapper.insertBatchModifyLog(formModifyLog);
        if(i<0) log.error("批量日志：{} 存储失败",formModifyLog);
    }
    
    
    
    
    
    //审计功能
    private void auditLog(DataModifyLog dataModifyLog,String userInfo){
        if(dataModifyLog.getCreatePerson()==null||dataModifyLog.getCreatePerson().equals("")){
            String userName = UserInfoUtil.getUserName(userInfo);
            dataModifyLog.setCreatePerson(userName);
        }
        
        if(dataModifyLog.getCreateTime()==null){
            dataModifyLog.setCreateTime(LocalDateTime.now());
        }
    }
    private void auditLog(FormModifyLog formModifyLog,String userInfo){
        if(formModifyLog.getCreatePerson()==null||formModifyLog.getCreatePerson().equals("")){
            String userName = UserInfoUtil.getUserName(userInfo);
            formModifyLog.setCreatePerson(userName);
        }

        if(formModifyLog.getCreateTime()==null){
            formModifyLog.setCreateTime(LocalDateTime.now());
        }
    }

}
