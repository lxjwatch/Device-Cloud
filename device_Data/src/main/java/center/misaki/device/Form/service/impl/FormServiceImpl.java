package center.misaki.device.Form.service.impl;

import center.misaki.device.Event.FlowStartEvent;
import center.misaki.device.Form.dao.FieldMapper;
import center.misaki.device.Form.dao.FormMapper;
import center.misaki.device.Form.dto.BatchChangeDto;
import center.misaki.device.Form.dto.BatchDeleteDto;
import center.misaki.device.Form.dto.DataScreenDto;
import center.misaki.device.Form.dto.OneDataDto;
import center.misaki.device.Form.pojo.DataModifyLog;
import center.misaki.device.Form.pojo.Field;
import center.misaki.device.Form.pojo.FormData;
import center.misaki.device.Form.service.DataScreenService;
import center.misaki.device.Form.service.FormService;
import center.misaki.device.Form.vo.BatchLogVo;
import center.misaki.device.Form.vo.FormDataVo;
import center.misaki.device.Form.vo.OneDataVo;
import center.misaki.device.domain.Pojo.Form;
import center.misaki.device.utils.UserInfoUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * @author Misaki
 */
@Service
public class FormServiceImpl implements FormService {
    
    private final FormMapper formMapper;
    private final FieldMapper fieldMapper;
    private final FormDataService formDataService;
    private final DataLogService dataLogService;
    private final DataScreenService dataScreenService;
    @Autowired
    private  Executor executor;
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    
    public FormServiceImpl(FormMapper formMapper, FieldMapper fieldMapper, FormDataService formDataService, DataLogService dataLogService, DataScreenService dataScreenService) {
        this.formMapper = formMapper;
        this.fieldMapper = fieldMapper;
        this.formDataService = formDataService;
        this.dataLogService = dataLogService;
        this.dataScreenService = dataScreenService;
    }


    @Override
    public FormDataVo getOneFormAllData(int formId,String userInfo) {
        //获取表单数据
        List<FormData> oneFormAllData = formDataService.getOneFormAllData(formId,userInfo);
        //获取表单域
        List<Field> oneFormFieldsMap = getOneFormFieldsMap(formId,userInfo);
        //创建表单视图对象
        FormDataVo formDataVo = new FormDataVo();
        formDataVo.setFieldsValue(oneFormAllData);
        formDataVo.setFields(oneFormFieldsMap);
        return formDataVo;
    }

    
    @Override
    public FormDataVo getOneFormData(int formId, String userInfo) {
        //获取表单数据
        List<FormData> oneFormData = formDataService.getOneFormData(formId,userInfo);
        //获取表单域
        List<Field> oneFormFieldsMap = getOneFormFieldsMap(formId, userInfo);
        //创建表单视图对象
        FormDataVo formDataVo = new FormDataVo();
        formDataVo.setFieldsValue(oneFormData);
        formDataVo.setFields(oneFormFieldsMap);
        return formDataVo;
    }

    @Override
    public FormDataVo getOneUserFormData(int formId, String userInfo) {
        //只获取当前用户创建的表单数据
        List<FormData> oneUserFormData = formDataService.getOneUserFormData(formId, userInfo);
        //获取表单域
        List<Field> oneFormFieldsMap = getOneFormFieldsMap(formId, userInfo);
        //创建表单视图对象
        FormDataVo formDataVo = new FormDataVo();
        formDataVo.setFieldsValue(oneUserFormData);
        formDataVo.setFields(oneFormFieldsMap);
        return formDataVo;
    }

    @Override
    public OneDataVo getOneData(int formId, int dataId,String userInfo) {
        //获取确切的一个表单的数据
        FormData oneData = formDataService.getOneData(dataId);
        //获取该表单的修改日志
        List<DataModifyLog> oneDataLog = dataLogService.getOneDataLog(dataId);
        //获取该表单的表单域
        List<Field> fields = getOneFormFieldsMap(formId, userInfo);
        //获取该表单内部的子表和字段结构
        String originFieldsData = formMapper.selectOneFormFields(formId, UserInfoUtil.getTenementId(userInfo));
        List<Form.FormFieldsDto> formFieldsDtos = JSONObject.parseArray(originFieldsData,Form.FormFieldsDto.class);
        //创建该表单的视图对象
        OneDataVo oneDataVo = new OneDataVo();
        oneDataVo.setData(oneData);
        oneDataVo.setFields(fields);
        oneDataVo.setForm(formFieldsDtos);
        oneDataVo.setLogs(oneDataLog);
        return oneDataVo;
    }

    public OneDataVo getOneData(int dataId,String userInfo) {
        //获取当前表单数据
        FormData oneData = formDataService.getOneData(dataId);
        //获取单条数据的更改日志
        List<DataModifyLog> oneDataLog = dataLogService.getOneDataLog(dataId);
        //拿到当前表中正在使用的字段，字段序号和字段的集合（即子表单的内容）
        Integer formId = oneData.getFormId();
        List<Field> fields = getOneFormFieldsMap(formId, userInfo);
        //获取子表单id和名字
        String originFieldsData = formMapper.selectOneFormFields(formId, UserInfoUtil.getTenementId(userInfo));
        List<Form.FormFieldsDto> formFieldsDtos = JSONObject.parseArray(originFieldsData,Form.FormFieldsDto.class);
        //创建视图对象
        OneDataVo oneDataVo = new OneDataVo();
        oneDataVo.setData(oneData);
        oneDataVo.setFields(fields);
        oneDataVo.setForm(formFieldsDtos);
        oneDataVo.setLogs(oneDataLog);
        return oneDataVo;
    }

    @Override
    public List<BatchLogVo> getBatchLogs(int formId, String userInfo) {
        return dataLogService.getBatchDataLog(formId,userInfo);
    }


    @Override
    public boolean addOneData(OneDataDto oneDataDto, String userInfo) {
        //判断该表单的类型，对表单做出相应的处理（0是普通表单，1是流程表单）
        if(formMapper.selectType(oneDataDto.getFormId())==0){
            //普通表单
            return formDataService.addOneData(oneDataDto,userInfo);
        }else{
            //流程表单
            Integer dataId = formDataService.addOneFlowData(oneDataDto, userInfo);
            applicationEventPublisher.publishEvent(new FlowStartEvent(this,oneDataDto.getFormId(),dataId, userInfo));
            return dataId!=null;
        }
    }

    @Override
    public List<String> addOneData(OneDataDto.OneDataDtoPlus oneDataDtoPlus, String userInfo) {
        //首先进行重复检查
        Map<String, String> data = oneDataDtoPlus.getData();
        //List<反序列化后的formData表中的from_data>
        List<Map<String, String>> originAllData = formDataService.getOneFormAllDataMap(oneDataDtoPlus.getFormId(),userInfo);
        List<String> checkFieldIds = oneDataDtoPlus.getCheckFieldIds();
        List<String> checkAns = new ArrayList<>();
        for(String checkId:checkFieldIds){
            String checkData=data.get(checkId);
            for(Map<String,String> o:originAllData){
                //不是要校验的数据则跳过
                if(!o.containsKey(checkId)) continue;
                String originData = o.get(checkId);
                //如果数据相等，记录重复数据的id
                if(originData.equals(checkData)){
                    checkAns.add(checkId);
                    break;
                }
            }
        }
        //检验出有重复数据直接返回
        if(!checkAns.isEmpty()) return checkAns;
        //检验数据正常，异步提交添加数据
        OneDataDto oneDataDto = new OneDataDto();
        oneDataDto.setData(data);
        oneDataDto.setFormId(oneDataDtoPlus.getFormId());
        executor.execute(()->{addOneData(oneDataDto,userInfo);});
        return checkAns;
    }

    @Override
    public boolean changeOneData(OneDataDto oneDataDto, String userInfo) {
        return formDataService.changeOneData(oneDataDto,userInfo);
    }

    @Override
    public List<String> changeOneData(OneDataDto.OneDataDtoPlus oneDataDtoPlus, String userInfo) {
        //首先进行重复检查
        Map<String, String> data = oneDataDtoPlus.getData();
        //List<反序列化后的formData表中的from_data>，除去要修改的那条数据
        List<Map<String, String>> originAllData = formDataService.getOneFormAllDataMapExOne(oneDataDtoPlus.getFormId(), oneDataDtoPlus.getDataId(), userInfo);
        List<String> checkFieldIds = oneDataDtoPlus.getCheckFieldIds();
        List<String> checkAns = new ArrayList<>();
        for(String checkId:checkFieldIds){
            String checkData=data.get(checkId);
            for(Map<String,String> o:originAllData){
                //不是要校验的数据则跳过
                if(!o.containsKey(checkId)) continue;
                String originData = o.get(checkId);
                //如果数据相等，记录重复数据的id
                if(originData.equals(checkData)){
                    checkAns.add(checkId);
                    break;
                }
            }
        }
        //检验出有重复数据直接返回
        if(!checkAns.isEmpty()) return checkAns;
        //检验数据正常，异步提交更改数据
        OneDataDto oneDataDto = new OneDataDto();
        oneDataDto.setDataId(oneDataDtoPlus.getDataId());
        oneDataDto.setData(data);//修改后的新数据（源数据已被剔除）
        oneDataDto.setFormId(oneDataDtoPlus.getFormId());
        executor.execute(()->formDataService.changeOneData(oneDataDto,userInfo));
        return checkAns;
    }

    @Override
    public void batchChangeData(BatchChangeDto batchChangeDto, String userInfo) {
        formDataService.batchChangeData(batchChangeDto,userInfo);
    }

    @Override
    public void deleteOneData(Integer dataId, Integer formId, String userInfo) throws SQLException {
        formDataService.deleteOneData(formId,dataId,userInfo);
        
    }

    @Override
    public void batchDelete(BatchDeleteDto batchDeleteDto, String userInfo) {
        formDataService.BatchDeleteData(batchDeleteDto,userInfo);
    }

    @Override
    public FormDataVo getAllDataAfterScreen(DataScreenDto dataScreenDto, String userInfo) throws ExecutionException, InterruptedException {
        //拿到一张表单的表单域数据
        List<Field> oneFormFieldsMap = getOneFormFieldsMap(dataScreenDto.getFormId(), userInfo);
        //拿到一张表单的数据
        List<FormData> originData = formDataService.getOneFormData(dataScreenDto.getFormId(), userInfo);
        //
        List<FormData> formData = dataScreenService.screen(dataScreenDto, originData);
        FormDataVo formDataVo = new FormDataVo();
        formDataVo.setFieldsValue(formData);
        formDataVo.setFields(oneFormFieldsMap);
        return formDataVo;
    }

    @Override
    public FormDataVo getUserDataAfterScreen(DataScreenDto dataScreenDto, String userInfo) throws ExecutionException, InterruptedException {
        List<FormData> originData = formDataService.getOneUserFormData(dataScreenDto.getFormId(), userInfo);
        List<Field> oneFormFieldsMap = getOneFormFieldsMap(dataScreenDto.getFormId(), userInfo);
        List<FormData> formData = dataScreenService.screen(dataScreenDto, originData);
        FormDataVo formDataVo = new FormDataVo();
        formDataVo.setFieldsValue(formData);
        formDataVo.setFields(oneFormFieldsMap);
        return formDataVo;
    }

    @Override
    public List<OneDataVo.OneFieldValue> dataLink(List<DataScreenDto> dataScreenDtos, String userInfo) throws ExecutionException, InterruptedException {
        List<OneDataVo.OneFieldValue> ans = new ArrayList<>();
        for(DataScreenDto dataScreenDto:dataScreenDtos){
            List<FormData> originData = formDataService.getOneUserFormData(dataScreenDto.getFormId(), userInfo);
            List<FormData> formData = dataScreenService.screen(dataScreenDto, originData);
            String linkFieldIds = dataScreenDto.getLinkFieldId();
            String originId = dataScreenDto.getOriginId();
            if(formData==null||formData.size()==0){
                OneDataVo.OneFieldValue oneFieldValue = new OneDataVo.OneFieldValue();
                oneFieldValue.setFieldId(originId);
                oneFieldValue.setFieldValue("");
                ans.add(oneFieldValue);
                continue;
            }
            FormData data = formData.get(0);
            Map<String, String> dataMap = JSON.parseObject(data.getFormData(), new TypeReference<Map<String, String>>() {});
            OneDataVo.OneFieldValue oneFieldValue = new OneDataVo.OneFieldValue();
            oneFieldValue.setFieldId(originId);
            oneFieldValue.setFieldValue(dataMap.get(linkFieldIds));
            ans.add(oneFieldValue);
        }
        return ans;
    }

    @Override
    public boolean changeFormTypeToFlow(int formId, String userInfo) {
        int i = formMapper.update(null, new UpdateWrapper<Form>().eq("id", formId).set("form_type", 1));
        return i>0;
    }

    @Override
    public boolean changeFormTypeToNormal(int formId, String userInfo) {
        int i = formMapper.update(null, new UpdateWrapper<Form>().eq("id", formId).set("form_type", 0));
        return i>0;
    }

    @Override
    public List<OneDataVo.OneFormLinkValue> getDataLinkFormSearch(List<DataScreenDto> dataScreenDtos, String userInfo) throws ExecutionException, InterruptedException {
        List<OneDataVo.OneFormLinkValue> ans = new ArrayList<>();
        for(DataScreenDto dataScreenDto:dataScreenDtos){
            List<FormData> originData = formDataService.getOneFormData(dataScreenDto.getFormId(), userInfo);
            List<FormData> screenData = dataScreenService.screen(dataScreenDto, originData);
            OneDataVo.OneFormLinkValue oneFormLinkValue = new OneDataVo.OneFormLinkValue();
            oneFormLinkValue.setValues(screenData);
            oneFormLinkValue.setWatchFieldIds(dataScreenDto.getFieldIds());
            oneFormLinkValue.setFieldId(dataScreenDto.getOriginId());
            ans.add(oneFormLinkValue);
        }
        return ans; 
    }

    @Override
    public List<FormData> fastQuery(List<Integer> dataIds) {
        List<FormData> ans = new ArrayList<>();
        dataIds.forEach(d->{ans.add(formDataService.getOneData(d));});
        return ans;
    }


    /**
     * 拿到一张表中正在使用的字段，字段序号和字段的集合
     * @return 字段集合
     */
    public List<Field> getOneFormFieldsMap(int formId,String userInfo){
        String originData = formMapper.selectOneFormFields(formId,UserInfoUtil.getTenementId(userInfo));
        List<Field> ans = new ArrayList<>();
        if(originData==null||originData.equals("")) return ans;
        //拿到一张表的子表单（即表单域fields）
        List<Form.FormFieldsDto> formFieldsDtos = JSONObject.parseArray(originData,Form.FormFieldsDto.class);
        formFieldsDtos.forEach(formFieldsDto->{
            //获取所有子表单的id
            List<String> fieldsId = formFieldsDto.getFieldsId();
            fieldsId.forEach(f->{//根据每个子表单的id获取一个子表单
                ans.add(fieldMapper.selectById(f));
            });
        });
        return ans;
    }
            
            
}
