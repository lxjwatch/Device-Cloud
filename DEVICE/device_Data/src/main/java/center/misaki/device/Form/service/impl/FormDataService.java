package center.misaki.device.Form.service.impl;

import center.misaki.device.Form.dto.*;
import center.misaki.device.Form.vo.OneDataVo;
import center.misaki.device.domain.Pojo.Form;
import center.misaki.device.Form.dao.FormDataMapper;
import center.misaki.device.Form.dao.FormMapper;
import center.misaki.device.Form.pojo.FormData;
import center.misaki.device.Form.pojo.FormModifyLog;
import center.misaki.device.utils.UserInfoUtil;
import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.alibaba.fastjson.JSON.parseObject;

/**
 * @author Misaki
 * 表单数据服务类
 */
@Service
@Slf4j
public class FormDataService {
    
    private final FormDataMapper formDataMapper;
    
    private final DataLogService dataLogService;
    
    private final FormMapper formMapper;

    
    

    public FormDataService(FormDataMapper formDataMapper, DataLogService dataLogService, FormMapper formMapper) {
        this.formDataMapper = formDataMapper;
        this.dataLogService = dataLogService;
        this.formMapper = formMapper;
    }

    /**
     * 拿到一个表单中的所有数据，不管他是否已经被删除了，还是在流程中转中
     * @return 数据集合
     */
    public List<FormData> getOneFormAllData(int formId,String userInfo){
        return formDataMapper.selectOneFormDataAll(formId, UserInfoUtil.getTenementId(userInfo));
    }

    /**
     * 拿到一个表单中的正常数据，需要他不能被删除，和已经不在流程中
     */
    public List<FormData> getOneFormData(int formId,String userInfo){
        return formDataMapper.selectOneFormData(formId,UserInfoUtil.getTenementId(userInfo));
    }

    /**
     * 拿到一个表单中仅仅只有自己提交的数据，需要不被删除，和不在流程中
     */
    public List<FormData> getOneUserFormData(int formId,String userInfo){
        return formDataMapper.selectUserFormData(formId,UserInfoUtil.getTenementId(userInfo),UserInfoUtil.getUserName(userInfo));
    }

    /**
     * 根据数据ID查询一条数据的信息
     */
    public FormData getOneData(int dataId){
        return formDataMapper.selectById(dataId);
    }
    



    /**
     * 添加一条数据
     */
    @Transactional
    public boolean addOneData(OneDataDto oneDataDto,String userInfo){
        FormData oneData = new FormData();
        oneData.setTenementId(UserInfoUtil.getTenementId(userInfo));
        oneData.setFormId(oneDataDto.getFormId());
        oneData.setFormData(JSON.toJSONString(oneDataDto.getData(), SerializerFeature.WriteNonStringKeyAsString));
        //补充数据（创建者、创建时间，更新者、更新时间）
        auditData(oneData,userInfo);
        return formDataMapper.insert(oneData)>0;
    }
    
    /**
     * 添加一条需要流程流转的数据
     */
    @Transactional
    public Integer addOneFlowData(OneDataDto oneDataDto,String userInfo){
        FormData oneData = new FormData();
        oneData.setTenementId(UserInfoUtil.getTenementId(userInfo));
        oneData.setFormId(oneDataDto.getFormId());
        oneData.setFormData(JSON.toJSONString(oneDataDto.getData(), SerializerFeature.WriteNonStringKeyAsString));
        oneData.setIsFlowData(true);//标记该条数据是需要流程流转的数据，默认为false
        auditData(oneData,userInfo);
        formDataMapper.insert(oneData);
        return oneData.getId();
    }
    /**
     * 修改一条数据使其变为正常数据
     */
    @Transactional
    @Async
    public void changeOneData(Integer dataId){
        formDataMapper.update(null,new UpdateWrapper<FormData>().eq("id",dataId).set("is_flow_data",false));
    }
    
    

    /**
     * 修改一条数据
     */
    @Transactional
    public boolean changeOneData(OneDataDto oneDataDto,String userInfo){
        //拿到需要修改的数据
        FormData oneData = formDataMapper.selectById(oneDataDto.getDataId());
        //保存修改日志
        dataLogService.saveSingleDataLog(oneDataDto,userInfo,BeanUtil.copyProperties(oneData,FormData.class));
        //更新数据
        oneData.setFormData(JSON.toJSONString(oneDataDto.getData(),SerializerFeature.WriteNonStringKeyAsString));
        auditData(oneData,userInfo);
        return formDataMapper.updateById(oneData)>0;
    }

    /**
     * 批量修改数据
     */
    @Transactional
    public void batchChangeData(BatchChangeDto batchChangeDto,String userInfo){
        List<Integer> dataId = batchChangeDto.getDataId();
        FormModifyLog formModifyLog = new FormModifyLog();
        //获取该表单的表单域
        Set<String> originFieldIds = JSONObject.parseArray(formMapper.selectOneFormFields(batchChangeDto.getFormId(), UserInfoUtil.getTenementId(userInfo)), Form.FormFieldsDto.class)
                .stream()                               //类型：Stream<Form.FormFieldsDto>
                //获取出form_fields中的fieldsId,是一个字符串数组
                .map(Form.FormFieldsDto::getFieldsId)   //类型：Stream<List<String>>
                //将字符串数组中的每个字符串分别变成一个Stream流
                .flatMap(Collection::stream)            //类型：Stream<String>
                //将这些字符串Stream流转换成一个HashSet集合
                .collect(Collectors.toSet());           //类型：HashSet
        int success=0;
        for(Integer d: dataId){
            /**
             * 下面两行代码似乎可以放到if里，if条件不满足用不到这两个数据，用不到不获取是不是可以节省时间
             */
            FormData oneData = formDataMapper.selectById(d);
            Map<String, String> originData = parseObject(oneData.getFormData(), new TypeReference<Map<String, String>>() {});
            //修改的数据属于该提交的表单时才允许修改数据
            if(originFieldIds.contains(batchChangeDto.getFieldId())){
                //修改旧数据为提交的新数据（未更新数据库）
                originData.put(batchChangeDto.getFieldId(),batchChangeDto.getNewValue());
                success++;
                oneData.setFormData(JSON.toJSONString(originData, SerializerFeature.WriteNonStringKeyAsString));
                auditData(oneData, userInfo);
                //修改旧数据为提交的新数据（更新数据库）
                formDataMapper.updateById(oneData);
            }
        }
        //记录成功修改数据的条数
        formModifyLog.setSuccessNum(success);
        //记录批量修改数据日志
        dataLogService.saveBatchDataLog(batchChangeDto,userInfo,formModifyLog);
    }

    /**
     * 删除一条数据,  删除要慎重 ，所以采用比较严格的判断
     */
    @Transactional
    @Async
    public void deleteOneData(Integer formId,Integer dataId,String userInfo) throws SQLException {
        int i = formDataMapper.deleteById(dataId);
        dataLogService.deleteDataLog(dataId);
        if(i!=1) throw new SQLException("删除错误，操作非法");
    }

    /**
     * 批量删除数据，删除要慎重，采用严格判断法
     */
    @Async
    @Transactional
    public void BatchDeleteData(BatchDeleteDto batchDeleteDto,String userInfo){
        if(batchDeleteDto.getDataIds()==null||batchDeleteDto.getDataIds().isEmpty()) return;
        List<Integer> dataIds = batchDeleteDto.getDataIds();
        int i = formDataMapper.deleteBatchIds(dataIds);
        log.info("将要删除 {} 条数据，实际删除 {} 条",batchDeleteDto.getDataIds().size(),i);
        dataIds.forEach(dataLogService::deleteDataLog);
    }
    
    
    
    //获取一张表单内所有的数据ID，和数据值的MAP集合
    public List<Map<String,String>> getOneFormAllDataMap(Integer formId,String userInfo){
       return getOneFormData(formId, userInfo)
               .stream()
               .map(FormData::getFormData)
               .map(m -> JSON.parseObject(m, new TypeReference<Map<String, String>>() {}))
               .collect(Collectors.toList());
    }
    //获取一张表单内所有的数据ID，和数据值的MAP集合，除去其中一条数据
    public List<Map<String,String>> getOneFormAllDataMapExOne(Integer formId,Integer dataId,String userInfo){
       return getOneFormData(formId, userInfo)
               .stream()
               .filter(a->!a.getId().equals(dataId))
               .map(FormData::getFormData)
               .map(m -> JSON.parseObject(m, new TypeReference<Map<String, String>>() {}))
               .collect(Collectors.toList());
    }
    
    //获取一张表单内所有的数据ID
    public List<Integer> getOneFormAllDataId(Integer formId){
       return formDataMapper.selectIds(formId);
    }
    
    
    //将原始数据转换为：数据ID，和数据值的MAP集合
    public List<Map<String,String>> converterDataMap(List<FormData> originData){
        return originData
                .stream()
                .map(FormData::getFormData)
                .map(m -> JSON.parseObject(m, new TypeReference<Map<String, String>>(){}))
                .collect(Collectors.toList());
    }
    
    //添加一条评论信息
    @Async
    @Transactional
    public void  addComment(DataCommentDto dataCommentDto,Integer dataId){
        String comment = formDataMapper.selectOneDataComment(dataId);
        if(comment==null||comment.equals("")){
            List<DataCommentDto> dataCommentDtos = new ArrayList<>();
            dataCommentDtos.add(dataCommentDto);
            int i = formDataMapper.updateOneDataComment(dataId, JSONObject.toJSONString(dataCommentDtos));
            log.info("更新数据评论 {} 条",i);
        }else{
            List<DataCommentDto> dataCommentDtos = JSONObject.parseArray(comment, DataCommentDto.class);
            dataCommentDtos.add(dataCommentDto);
            int i = formDataMapper.updateOneDataComment(dataId, JSONObject.toJSONString(dataCommentDtos));
            log.info("更新数据评论 {} 条",i);
        }
    }

    /**
     * 根据DataID，获取表单名字
     */
    public String getFormNameByDataId(Integer dataId){
        return formDataMapper.selectOneFormName(dataId);
    }

    /**
     * 根据DataID，获取表单Id
     */
    public Integer getFormIdByDataId(Integer dataId){
        return formDataMapper.selectFormId(dataId);
    }

    /**
     * 关联其他表单数据接口
     */
    public List<String> getOtherFormDataByField(DataLinkOtherDto dataLinkOtherDto,String userInfo){
        Integer formId = dataLinkOtherDto.getFormId();
        String fieldId = dataLinkOtherDto.getFieldId();
        List<FormData> oneFormData = getOneFormData(formId, userInfo);
        if(dataLinkOtherDto.getOrder().equals("<v")){
            return oneFormData.stream().map(o -> {
                String formData = o.getFormData();
                Map<String, String> formDataMap = parseObject(formData, new TypeReference<Map<String, String>>() {
                });
                return formDataMap.get(fieldId);
            }).filter(Objects::nonNull).sorted(String::compareTo).collect(Collectors.toList());
        }else if(dataLinkOtherDto.getOrder().equals(">v")){
            return oneFormData.stream().map(o -> {
                String formData = o.getFormData();
                Map<String, String> formDataMap = parseObject(formData, new TypeReference<Map<String, String>>() {
                });
                return formDataMap.get(fieldId);
            }).filter(Objects::nonNull).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        }else if(dataLinkOtherDto.getOrder().equals("<t")){
            return oneFormData.stream().sorted((v1,v2)->{
                return v1.getCreateTime().isAfter(v2.getCreateTime())||v1.getCreateTime().isEqual(v2.getCreateTime())?1:-1;
            }).map(o -> {
                String formData = o.getFormData();
                Map<String, String> formDataMap = parseObject(formData, new TypeReference<Map<String, String>>() {
                });
                return formDataMap.get(fieldId);
            }).filter(Objects::nonNull).collect(Collectors.toList());
        }else {
            return oneFormData.stream().sorted((v1, v2) -> {
                return v1.getCreateTime().isBefore(v2.getCreateTime())||v1.getCreateTime().isEqual(v2.getCreateTime()) ? 1 : -1;
            }).map(o -> {
                String formData = o.getFormData();
                Map<String, String> formDataMap = parseObject(formData, new TypeReference<Map<String, String>>() {
                });
                return formDataMap.get(fieldId);
            }).filter(Objects::nonNull).collect(Collectors.toList());
        }
    }
    

    
    //设置好数据审计
    private void auditData(FormData oneData,String userInfo){
        //如果一条FormData数据创建时没有设置创建者，则设置为当前用户
        if(oneData.getCreatePerson()==null||oneData.getCreatePerson().equals("")){
            oneData.setCreatePerson(UserInfoUtil.getUserName(userInfo));
        }
        //如果一条FormData数据创建时没有设置创建时间，则设置为当前时间
        if(oneData.getCreateTime()==null){
            oneData.setCreateTime(LocalDateTime.now());
        }
        //设置一条FormData数据的更新人为当前用户
        oneData.setUpdatePerson(UserInfoUtil.getUserName(userInfo));
        //设置一条FormData数据的更新时间为当前时间
        oneData.setUpdateTime(LocalDateTime.now());
    }
    

}

