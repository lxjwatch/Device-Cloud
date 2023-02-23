package center.misaki.device.Form.service.impl;

import center.misaki.device.Flow.Flow;
import center.misaki.device.Flow.FlowLog;
import center.misaki.device.Flow.WorkLog;
import center.misaki.device.Flow.dao.FlowLogMapper;
import center.misaki.device.Flow.dao.WorkLogMapper;
import center.misaki.device.Flow.service.FlowService;
import center.misaki.device.Form.dao.MenuMapper;
import center.misaki.device.Form.dto.BatchDeleteDto;
import center.misaki.device.Form.vo.MenuFormVo;
import center.misaki.device.domain.Pojo.Form;
import center.misaki.device.Form.dao.FieldMapper;
import center.misaki.device.Form.dao.FormMapper;
import center.misaki.device.Form.dto.FormStrucDto;
import center.misaki.device.Form.pojo.Field;
import center.misaki.device.Form.service.StructureService;
import center.misaki.device.Form.vo.FormStrucVo;
import center.misaki.device.Form.vo.SimpleFieldVo;
import center.misaki.device.domain.Pojo.Menu;
import center.misaki.device.utils.UserInfoUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Misaki
 */
@Service
@Slf4j
public class StructureServiceImpl implements StructureService {
    private final  FormServiceImpl formService;
    private final FormMapper formMapper;
    private final FieldMapper fieldMapper;
    
    private final FormDataService formDataService;
    
    private final FieldService fieldService;
    
    private final MenuMapper menuMapper;
    
    @Autowired
    private FlowService flowService;
    
    private final WorkLogMapper workLogMapper;
    private final FlowLogMapper flowLogMapper;

    public StructureServiceImpl(FormServiceImpl formService, FormMapper formMapper, FieldMapper fieldMapper, FormDataService formDataService, FieldService fieldService, MenuMapper menuMapper, WorkLogMapper workLogMapper, FlowLogMapper flowLogMapper) {
        this.formService = formService;
        this.formMapper = formMapper;
        this.fieldMapper = fieldMapper;
        this.formDataService = formDataService;
        this.fieldService = fieldService;
        this.menuMapper = menuMapper;
        this.workLogMapper = workLogMapper;
        this.flowLogMapper = flowLogMapper;
    }

    @Override
    @Transactional
    public boolean createForm(Integer menuId, Integer formType, String name, int tenementId, String formFields){
        assert formType==0 || formType==1;
        Form form = new Form();
        form.setTenementId(tenementId);
        form.setFormName(name);
        form.setMenuId(menuId);
        form.setFormType(formType);

        form.setFormFields(formFields);
        form.setProperties("{\"displayType\":\"column\",\"labelWidth\":120,\"type\":\"object\"}");
        form.setCreateTime(LocalDateTime.now());
        form.setUpdateTime(LocalDateTime.now());

        int i = formMapper.insert(form);
        return i>0;
    }

    @Override
    public FormStrucVo getFormStruc(int formId, String userInfo) {
        Form form = formMapper.selectById(formId);
        List<Field> fields = formService.getOneFormFieldsMap(formId, userInfo);
        FormStrucVo formStrucVo = new FormStrucVo();
        formStrucVo.setForm(form);
        formStrucVo.setFields(fields);
        if(form.getFormType()==1){
            Flow.Node startNode = flowService.getStartNode(formId);
            if(startNode!=null) {
                formStrucVo.setFieldsAuth(startNode.getFieldAuth());
            }
        }
        return formStrucVo;
    }

    @Override
    @Transactional
    public boolean changeFormStruc(FormStrucDto formStrucDto, String userInfo) {
        Form form = formMapper.selectById(formStrucDto.getFormId());
        List<Form.FormFieldsDto> originFields = JSON.parseObject(form.getFormFields(), new TypeReference<List<Form.FormFieldsDto>>() {});
        fieldService.checkDeleteChange(originFields,new ArrayList<>(formStrucDto.getSubForms()));
        fieldService.checkModifyChange(originFields,new ArrayList<>(formStrucDto.getSubForms()));

        LocalDateTime updateTime = LocalDateTime.now();
        //分析表单自身属性是否变化
        if(!form.getProperties().equals(formStrucDto.getProperties())){
            form.setProperties(formStrucDto.getProperties());
            form.setUpdateTime(updateTime);
        }
//        if(!form.getFormName().equals(formStrucDto.getProperties())){
//            form.setFormName(formStrucDto.getFormName());
//            form.setUpdateTime(updateTime);
//        }
        //分析表单子表变化，以及子表新增字段变化
        List<FormStrucDto.SubFormDto> subForms = formStrucDto.getSubForms();
        List<FormStrucDto.FieldStrucDto> subFormField = subForms.stream().map(FormStrucDto.SubFormDto::getFields).flatMap(Collection::stream).collect(Collectors.toList());
        int oral=0;
        int success=0;
        Set<String> fields = originFields.stream().map(Form.FormFieldsDto::getFieldsId).flatMap(Collection::stream).collect(Collectors.toSet());
        for (FormStrucDto.FieldStrucDto fieldStrucDto : subFormField) {
            if (!fields.contains(fieldStrucDto.getFieldId())) {
                oral++;
                Field field = new Field();
                field.setId(fieldStrucDto.getFieldId());
                field.setTenementId(UserInfoUtil.getTenementId(userInfo));
                field.setFormId(formStrucDto.getFormId());
                field.setCreateTime(LocalDateTime.now());
                field.setUpdateTime(LocalDateTime.now());
                field.setName(fieldStrucDto.getName());
                field.setTypeId(fieldStrucDto.getTypeId());
                field.setDetailJson(fieldStrucDto.getDetailJson());
                int i = fieldMapper.insert(field);
                success+=i;
            }
        }
        log.info("新增字段：需要新增 {} 个，实际成功新增 {} 个",oral,success);
        
        List<Form.FormFieldsDto> newSubForm = new ArrayList<>();
        subForms.forEach(s->{
            Form.FormFieldsDto formFieldsDto = new Form.FormFieldsDto();
            formFieldsDto.setName(s.getName());
            if(s.getName().equals("root")&&s.getFields()==null) return;
            formFieldsDto.setFieldsId(s.getFields().stream().map(FormStrucDto.FieldStrucDto::getFieldId).collect(Collectors.toList()));
            newSubForm.add(formFieldsDto);
        });
        
        //更新表单
        form.setFormFields(JSON.toJSONString(newSubForm));
        int i = formMapper.updateById(form);
        
        log.info( i>0?"成功":"失败"+": 更新form {}",form);
        return i>0;
    }

    
    @Override
    public List<SimpleFieldVo> getFieldsInForm(int formId, String userInfo) {
        List<Form.FormFieldsDto> formFieldsDtos = JSON.parseObject(formMapper.selectOneFormFields(formId, UserInfoUtil.getTenementId(userInfo)), new TypeReference<List<Form.FormFieldsDto>>() {});
        List<SimpleFieldVo> simpleFieldVos = new ArrayList<>();
        formFieldsDtos.forEach(f->{
            f.getFieldsId().forEach(d->{
                Field field = fieldMapper.selectById(d);
                SimpleFieldVo simpleFieldVo = new SimpleFieldVo();
                simpleFieldVo.setFieldId(field.getId());
                simpleFieldVo.setTypeId(field.getTypeId());
                simpleFieldVo.setName(field.getName());
                simpleFieldVos.add(simpleFieldVo);
            });
        });
        return simpleFieldVos;
        
    }

    @Override
    @Transactional
    public List<MenuFormVo> getSimpleFormsInMenu(String userInfo) {
        List<MenuFormVo> ans = new ArrayList<>();
        List<Menu> menus = menuMapper.selectList(new QueryWrapper<Menu>().eq("tenement_id", UserInfoUtil.getTenementId(userInfo)));
        menus.forEach(m->{
            List<MenuFormVo.SimpleFormVo> simpleFormVos = formMapper.selectSimpleForms(m.getId());
            MenuFormVo menuFormVo = new MenuFormVo();
            menuFormVo.setMenuId(m.getId());
            menuFormVo.setMenuName(m.getName());
            menuFormVo.setSimpleForms(simpleFormVos);
            ans.add(menuFormVo);
        });
        return ans;
    }

    @Override
    @Transactional
    public boolean changeMenuName(int menuId, String menuName, String userInfo) {
        Menu menu = menuMapper.selectById(menuId);
        if(menu==null) return false;
        menu.setName(menuName);
        int i = menuMapper.updateById(menu);
        return i>0;
    }

    @Override
    @Transactional
    public boolean changeFormName(int formId, String formName, String userInfo) {
        int i = formMapper.update(null, new UpdateWrapper<Form>().eq("id", formId).set("form_name", formName).set("update_time", LocalDateTime.now()));
        return i>0;
    }

    @Override
    @Transactional
    public FormStrucDto createNormalForm(Integer menuId,Integer formType,String userInfo) {
        assert formType==0 || formType==1;
        Form form = new Form();
        form.setTenementId(UserInfoUtil.getTenementId(userInfo));
        form.setFormName("未命名表单");
        form.setMenuId(menuId);
        form.setTenementId(UserInfoUtil.getTenementId(userInfo));
        form.setFormType(formType);
        Form.FormFieldsDto formFieldsDto = new Form.FormFieldsDto();
        formFieldsDto.setName("root");
        formFieldsDto.setFieldsId(new ArrayList<>());
        form.setFormFields(JSON.toJSONString(Collections.singletonList(formFieldsDto)));
        form.setProperties("{\"displayType\":\"column\",\"labelWidth\":120,\"type\":\"object\"}");
        form.setCreateTime(LocalDateTime.now());
        form.setUpdateTime(LocalDateTime.now());
        int i = formMapper.insert(form);
        if(i<=0) return null;
        FormStrucDto formStrucDto = new FormStrucDto();
        formStrucDto.setFormId(form.getId());
        formStrucDto.setFormName(form.getFormName());
        formStrucDto.setProperties(form.getProperties());
        
        FormStrucDto.SubFormDto subFormDto = new FormStrucDto.SubFormDto();
        subFormDto.setName("root");
        subFormDto.setFields(new ArrayList<>());
        List<FormStrucDto.SubFormDto> subFormDtos = new ArrayList<>();
        subFormDtos.add(subFormDto);
        formStrucDto.setSubForms(subFormDtos);
        return formStrucDto;
    }

    @Override
    @Transactional
    public boolean changeFormType(int formId, int formType, String userInfo) {
        int i = formMapper.update(null, new UpdateWrapper<Form>().eq("id", formId).set("form_type", formType).set("update_time", LocalDateTime.now()));
        return i>0;
    }

    @Override
    @Transactional
    public boolean deleteForm(int formId, String userInfo) {
        int i = formMapper.deleteById(formId);
        List<Integer> dataIds = formDataService.getOneFormAllDataId(formId);
        BatchDeleteDto batchDeleteDto = new BatchDeleteDto();
        batchDeleteDto.setFormId(formId);
        batchDeleteDto.setDataIds(dataIds);
        formDataService.BatchDeleteData(batchDeleteDto,userInfo);
        fieldMapper.delete(new QueryWrapper<Field>().eq("form_id", formId));
        workLogMapper.delete(new QueryWrapper<WorkLog>().in("data_id", dataIds));
        flowLogMapper.delete(new QueryWrapper<FlowLog>().in("data_id", dataIds));
        return i>0;
    }

    @Override
    @Transactional
    public List<FormStrucVo.FormSimpleVo> getFormSimpleStruc(String userInfo) {
        List<Form> forms = formMapper.selectList(new QueryWrapper<Form>().eq("tenement_id", UserInfoUtil.getTenementId(userInfo)));
        List<FormStrucVo.FormSimpleVo> ans = new ArrayList<>();
        forms.forEach(f->{
            FormStrucVo.FormSimpleVo formSimpleVo = new FormStrucVo.FormSimpleVo();
            formSimpleVo.setFormId(f.getId());
            formSimpleVo.setFormName(f.getFormName());
            String formFields = f.getFormFields();
            if(formFields!=null&&!formFields.equals("")){
                List<FormStrucVo.FormSimpleVo.FieldSimpleVo> fieldSimpleVos = new ArrayList<>();
                List<Form.FormFieldsDto> formFieldsDtos = JSON.parseArray(formFields, Form.FormFieldsDto.class);
                formFieldsDtos.forEach(ff->{
                   ff.getFieldsId().forEach(fid->{
                       Field field = fieldMapper.selectById(fid);
                       if(field!=null){
                           FormStrucVo.FormSimpleVo.FieldSimpleVo fieldSimpleVo = new FormStrucVo.FormSimpleVo.FieldSimpleVo();
                           fieldSimpleVo.setFieldId(field.getId());
                           fieldSimpleVo.setFieldName(field.getName());
                           fieldSimpleVo.setTypeId(field.getTypeId());
                           fieldSimpleVos.add(fieldSimpleVo);
                       }
                   });
                });
                formSimpleVo.setFieldSimpleVos(fieldSimpleVos);
            }
            ans.add(formSimpleVo);
        });
        return ans;
    }


}
