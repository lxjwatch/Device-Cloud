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
        List<FormData> oneFormAllData = formDataService.getOneFormAllData(formId,userInfo);
        List<Field> oneFormFieldsMap = getOneFormFieldsMap(formId,userInfo);
        FormDataVo formDataVo = new FormDataVo();
        formDataVo.setFieldsValue(oneFormAllData);
        formDataVo.setFields(oneFormFieldsMap);
        return formDataVo;
    }

    
    @Override
    public FormDataVo getOneFormData(int formId, String userInfo) {
        List<FormData> oneFormData = formDataService.getOneFormData(formId,userInfo);
        List<Field> oneFormFieldsMap = getOneFormFieldsMap(formId, userInfo);
        FormDataVo formDataVo = new FormDataVo();
        formDataVo.setFieldsValue(oneFormData);
        formDataVo.setFields(oneFormFieldsMap);
        return formDataVo;
    }

    @Override
    public FormDataVo getOneUserFormData(int formId, String userInfo) {
        List<FormData> oneUserFormData = formDataService.getOneUserFormData(formId, userInfo);
        List<Field> oneFormFieldsMap = getOneFormFieldsMap(formId, userInfo);
        FormDataVo formDataVo = new FormDataVo();
        formDataVo.setFieldsValue(oneUserFormData);
        formDataVo.setFields(oneFormFieldsMap);
        return formDataVo;
    }

    @Override
    public OneDataVo getOneData(int formId, int dataId,String userInfo) {
        FormData oneData = formDataService.getOneData(dataId);
        List<DataModifyLog> oneDataLog = dataLogService.getOneDataLog(dataId);
        List<Field> fields = getOneFormFieldsMap(formId, userInfo);
        String originFieldsData = formMapper.selectOneFormFields(formId, UserInfoUtil.getTenementId(userInfo));
        List<Form.FormFieldsDto> formFieldsDtos = JSONObject.parseArray(originFieldsData,Form.FormFieldsDto.class);
        OneDataVo oneDataVo = new OneDataVo();
        oneDataVo.setData(oneData);
        oneDataVo.setFields(fields);
        oneDataVo.setForm(formFieldsDtos);
        oneDataVo.setLogs(oneDataLog);
        return oneDataVo;
    }

    public OneDataVo getOneData(int dataId,String userInfo) {
        FormData oneData = formDataService.getOneData(dataId);
        List<DataModifyLog> oneDataLog = dataLogService.getOneDataLog(dataId);
        Integer formId = oneData.getFormId();
        List<Field> fields = getOneFormFieldsMap(formId, userInfo);
        String originFieldsData = formMapper.selectOneFormFields(formId, UserInfoUtil.getTenementId(userInfo));
        List<Form.FormFieldsDto> formFieldsDtos = JSONObject.parseArray(originFieldsData,Form.FormFieldsDto.class);
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
        if(formMapper.selectType(oneDataDto.getFormId())==0){
            return formDataService.addOneData(oneDataDto,userInfo);
        }else{
            Integer dataId = formDataService.addOneFlowData(oneDataDto, userInfo);
            applicationEventPublisher.publishEvent(new FlowStartEvent(this,oneDataDto.getFormId(),dataId, userInfo));
            return dataId!=null;
        }
    }

    @Override
    public List<String> addOneData(OneDataDto.OneDataDtoPlus oneDataDtoPlus, String userInfo) {
        //首先进行重复检查
        Map<String, String> data = oneDataDtoPlus.getData();
        List<Map<String, String>> originAllData = formDataService.getOneFormAllDataMap(oneDataDtoPlus.getFormId(),userInfo);
        List<String> checkFieldIds = oneDataDtoPlus.getCheckFieldIds();
        List<String> checkAns = new ArrayList<>();
        for(String checkId:checkFieldIds){
            String checkData=data.get(checkId);
            for(Map<String,String> o:originAllData){
                if(!o.containsKey(checkId)) continue;
                String originData = o.get(checkId);
                if(originData.equals(checkData)){
                    checkAns.add(checkId);
                    break;
                }
            }
        }
        if(!checkAns.isEmpty()) return checkAns;
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
        List<Map<String, String>> originAllData = formDataService.getOneFormAllDataMapExOne(oneDataDtoPlus.getFormId(), oneDataDtoPlus.getDataId(), userInfo);
        List<String> checkFieldIds = oneDataDtoPlus.getCheckFieldIds();
        List<String> checkAns = new ArrayList<>();
        for(String checkId:checkFieldIds){
            String checkData=data.get(checkId);
            for(Map<String,String> o:originAllData){
                if(!o.containsKey(checkId)) continue;
                String originData = o.get(checkId);
                if(originData.equals(checkData)){
                    checkAns.add(checkId);
                    break;
                }
            }
        }
        if(!checkAns.isEmpty()) return checkAns;
        OneDataDto oneDataDto = new OneDataDto();
        oneDataDto.setDataId(oneDataDtoPlus.getDataId());
        oneDataDto.setData(data);
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
        List<Field> oneFormFieldsMap = getOneFormFieldsMap(dataScreenDto.getFormId(), userInfo);
        List<FormData> originData = formDataService.getOneFormData(dataScreenDto.getFormId(), userInfo);
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
        List<Form.FormFieldsDto> formFieldsDtos = JSONObject.parseArray(originData,Form.FormFieldsDto.class);
        formFieldsDtos.forEach(formFieldsDto->{
            List<String> fieldsId = formFieldsDto.getFieldsId();
            fieldsId.forEach(f->{
                ans.add(fieldMapper.selectById(f));
            });
        });
        return ans;
    }
            
            
}
