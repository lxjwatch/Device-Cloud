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
        //将新表单的所有字段(多条field数据)与原表单的所有字段(多条field数据)做对比，判断需要删除什么旧字段
        fieldService.checkDeleteChange(originFields,new ArrayList<>(formStrucDto.getSubForms()));
        //将新表单的所有字段(多条field数据)与原表单的所有字段(多条field数据)做对比，判断需要修改什么旧字段
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
        //获取表单的子表单
        List<FormStrucDto.SubFormDto> subForms = formStrucDto.getSubForms();
        //将子表单转化为字段的集合
        List<FormStrucDto.FieldStrucDto> subFormField = subForms.stream().map(FormStrucDto.SubFormDto::getFields).flatMap(Collection::stream).collect(Collectors.toList());
        int oral=0;
        int success=0;
        //获取原始字段的集合
        Set<String> fields = originFields.stream().map(Form.FormFieldsDto::getFieldsId).flatMap(Collection::stream).collect(Collectors.toSet());
        //遍历子表单字段
        for (FormStrucDto.FieldStrucDto fieldStrucDto : subFormField) {
            //如果原始字段集合中不包含该子表单字段就说明该字段是新增的，下面将新增字段添加到数据库
            if (!fields.contains(fieldStrucDto.getFieldId())) {
                oral++;
                Field field = new Field();
                //这个新增字段的所有数据都是前端传来的，不是后端生成的，包括id，后端只负责存储
                field.setId(fieldStrucDto.getFieldId());
                field.setTenementId(UserInfoUtil.getTenementId(userInfo));
                field.setFormId(formStrucDto.getFormId());
                field.setCreateTime(LocalDateTime.now());
                field.setUpdateTime(LocalDateTime.now());
                field.setName(fieldStrucDto.getName());
                field.setTypeId(fieldStrucDto.getTypeId());
                field.setDetailJson(fieldStrucDto.getDetailJson());
                //将该新字段持久化到数据库
                int i = fieldMapper.insert(field);
                success+=i;
            }
        }
        log.info("新增字段：需要新增 {} 个，实际成功新增 {} 个",oral,success);

        //定义一个新的子表单集合
        List<Form.FormFieldsDto> newSubForm = new ArrayList<>();
        //遍历子表单
        subForms.forEach(s->{
            //定义一个新的FormFieldsDto对象
            Form.FormFieldsDto formFieldsDto = new Form.FormFieldsDto();
            formFieldsDto.setName(s.getName());
            //如果子表单的名称为root并且没有字段，返回
            if(s.getName().equals("root")&&s.getFields()==null) return;
            //否则设置FormFieldsDto对象的字段ID
            formFieldsDto.setFieldsId(s.getFields().stream().map(FormStrucDto.FieldStrucDto::getFieldId).collect(Collectors.toList()));
            //添加新的FormFieldsDto对象到集合
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
        // 根据用户所在的公司查询出所有表单
        List<Form> forms = formMapper.selectList(new QueryWrapper<Form>().eq("tenement_id", UserInfoUtil.getTenementId(userInfo)));
        // 初始化返回列表
        List<FormStrucVo.FormSimpleVo> ans = new ArrayList<>();
        // 遍历查询出的表单
        forms.forEach(f->{
            // 初始化表单结构
            FormStrucVo.FormSimpleVo formSimpleVo = new FormStrucVo.FormSimpleVo();
            formSimpleVo.setFormId(f.getId());
            formSimpleVo.setFormName(f.getFormName());
            // 获取表单字段
            String formFields = f.getFormFields();
            if(formFields!=null&&!formFields.equals("")){
                // 初始化字段列表
                List<FormStrucVo.FormSimpleVo.FieldSimpleVo> fieldSimpleVos = new ArrayList<>();
                // 解析表单字段
                List<Form.FormFieldsDto> formFieldsDtos = JSON.parseArray(formFields, Form.FormFieldsDto.class);
                // 遍历字段
                formFieldsDtos.forEach(ff->{
                   // 遍历字段id
                   ff.getFieldsId().forEach(fid->{
                       // 根据字段id查询字段
                       Field field = fieldMapper.selectById(fid);
                       if(field!=null){
                           // 初始化字段结构
                           FormStrucVo.FormSimpleVo.FieldSimpleVo fieldSimpleVo = new FormStrucVo.FormSimpleVo.FieldSimpleVo();
                           fieldSimpleVo.setFieldId(field.getId());
                           fieldSimpleVo.setFieldName(field.getName());
                           fieldSimpleVo.setTypeId(field.getTypeId());
                           // 添加到字段列表
                           fieldSimpleVos.add(fieldSimpleVo);
                       }
                   });
                });
                // 设置字段列表
                formSimpleVo.setFieldSimpleVos(fieldSimpleVos);
            }
            // 添加到返回列表
            ans.add(formSimpleVo);
        });
        // 返回结果
        return ans;
    }

//    @Override
//    @Transactional
//    public boolean createMenu(int tenementId) {
//        //创建菜单
//        Menu menu = new Menu();
//        menu.setTenementId(tenementId);
//        String[] menuNames = new String[]{"经营分析报表","设备点检巡检","设备维修保修","设备维护保养","备品备件管理","基础信息"};
//        int menuId = menuMapper.selectIdMax()+1;
//        for (int j = 0;j<menuNames.length;j++){
//            menu.setId(menuId++);
//            menu.setName(menuNames[j]);
//            menuMapper.insert(menu);
//        }
//        //菜单初始化
//        menuId = menuMapper.selectIdByTenementIdAndName(tenementId,"设备点检巡检");
//        createMenuForm(menuId,1,tenementId,"设备巡检单");
//        createMenuForm(menuId,0,tenementId,"巡检方案");
//
//        menuId = menuMapper.selectIdByTenementIdAndName(tenementId,"设备维修保修");
//        createMenuForm(menuId,1,tenementId,"设备报修单");
//
//        menuId = menuMapper.selectIdByTenementIdAndName(tenementId,"设备维护保养");
//        createMenuForm(menuId,0,tenementId,"设备保养单");
//        createMenuForm(menuId,0,tenementId,"保养计划基础表");
//
//        menuId = menuMapper.selectIdByTenementIdAndName(tenementId,"备品备件管理");
//        createMenuForm(menuId,0,tenementId,"备件入库单");
//        createMenuForm(menuId,0,tenementId,"备件领用单");
//        createMenuForm(menuId,0,tenementId,"备件台账");
//
//        menuId = menuMapper.selectIdByTenementIdAndName(tenementId,"基础信息");
//        createMenuForm(menuId,0,tenementId,"机房");
//        createMenuForm(menuId,0,tenementId,"设备类型");
//        createMenuForm(menuId,0,tenementId,"部门");
//        createMenuForm(menuId,0,tenementId,"设备状态");
//        createMenuForm(menuId,0,tenementId,"保养等级与频次");
//        createMenuForm(menuId,0,tenementId,"仓库");
//        createMenuForm(menuId,0,tenementId,"单位");
//        createMenuForm(menuId,0,tenementId,"安装地点");
//        createMenuForm(menuId,0,tenementId,"设备信息");
//
//        return true;
//    }

    //上面一个方法的优化版
    @Override
    @Transactional
    public boolean createMenu(int tenementId) {
        //菜单初始化
        Menu menu = new Menu();
        menu.setTenementId(tenementId);
        String[] menuNames = new String[]{"设备档案履历","经营分析报表","设备点检巡检","设备维修保修","设备维护保养","备品备件管理","基础信息"};
        int menuId = menuMapper.selectIdMax()+1;
        for (String menuName : menuNames){
            menu.setId(menuId++);
            menu.setName(menuName);
            menuMapper.insert(menu);
        }
        //表单初始化
        Map<String, Integer> menuNameMap = new HashMap<>();
        for (String menuName : menuNames){
            menuNameMap.put(menuName, menuMapper.selectIdByTenementIdAndName(tenementId, menuName));
        }
        createMenuForm(menuNameMap.get("设备档案履历"),0,tenementId,"设备档案履历表");
        createMenuForm(menuNameMap.get("设备点检巡检"),1,tenementId,"设备巡检单");
        createMenuForm(menuNameMap.get("设备点检巡检"),0,tenementId,"巡检方案");
        createMenuForm(menuNameMap.get("设备维修保修"),1,tenementId,"设备报修单");
        createMenuForm(menuNameMap.get("设备维护保养"),0,tenementId,"设备保养单");
        createMenuForm(menuNameMap.get("设备维护保养"),0,tenementId,"保养计划基础表");
        createMenuForm(menuNameMap.get("备品备件管理"),0,tenementId,"备件入库单");
        createMenuForm(menuNameMap.get("备品备件管理"),0,tenementId,"备件领用单");
        createMenuForm(menuNameMap.get("备品备件管理"),0,tenementId,"备件台账");
        createMenuForm(menuNameMap.get("基础信息"),0,tenementId,"机房");
        createMenuForm(menuNameMap.get("基础信息"),0,tenementId,"设备类型");
        createMenuForm(menuNameMap.get("基础信息"),0,tenementId,"部门");
        createMenuForm(menuNameMap.get("基础信息"),0,tenementId,"设备状态");
        createMenuForm(menuNameMap.get("基础信息"),0,tenementId,"保养等级与频次");
        createMenuForm(menuNameMap.get("基础信息"),0,tenementId,"仓库");
        createMenuForm(menuNameMap.get("基础信息"),0,tenementId,"单位");
        createMenuForm(menuNameMap.get("基础信息"),0,tenementId,"安装地点");
        createMenuForm(menuNameMap.get("基础信息"),0,tenementId,"设备信息");
        return true;
    }

    @Override
    @Transactional
    public boolean createMenuForm(Integer menuId, Integer formType, Integer tenementId,String formName) {
        assert formType==0 || formType==1;
        Form form = new Form();
        form.setFormName(formName);
        form.setMenuId(menuId);
        form.setTenementId(tenementId);
        form.setFormType(formType);
        Form.FormFieldsDto formFieldsDto = new Form.FormFieldsDto();
        formFieldsDto.setName("root");
        formFieldsDto.setFieldsId(new ArrayList<>());
        form.setFormFields(JSON.toJSONString(Collections.singletonList(formFieldsDto)));
        form.setProperties("{\"displayType\":\"column\",\"labelWidth\":120,\"type\":\"object\"}");
        form.setCreateTime(LocalDateTime.now());
        form.setUpdateTime(LocalDateTime.now());
        int i = formMapper.insert(form);
        return i>0;
    }

}
