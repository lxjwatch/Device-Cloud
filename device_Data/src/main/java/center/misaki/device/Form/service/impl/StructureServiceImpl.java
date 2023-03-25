package center.misaki.device.Form.service.impl;

import center.misaki.device.Flow.Flow;
import center.misaki.device.Flow.FlowLog;
import center.misaki.device.Flow.WorkLog;
import center.misaki.device.Flow.dao.FlowLogMapper;
import center.misaki.device.Flow.dao.FlowMapper;
import center.misaki.device.Flow.dao.WorkLogMapper;
import center.misaki.device.Flow.service.FlowService;
import center.misaki.device.Form.dao.*;
import center.misaki.device.Form.dto.BatchDeleteDto;
import center.misaki.device.Form.vo.MenuFormVo;
import center.misaki.device.domain.Pojo.Form;
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

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    private final DepartmentMapper departmentMapper;
    
    @Autowired
    private FlowService flowService;

    @Resource
    private FlowMapper flowMapper;
    @Resource
    private UserMapper userMapper;
    
    private final WorkLogMapper workLogMapper;
    private final FlowLogMapper flowLogMapper;

    public StructureServiceImpl(FormServiceImpl formService, FormMapper formMapper, FieldMapper fieldMapper, FormDataService formDataService, FieldService fieldService, MenuMapper menuMapper, DepartmentMapper departmentMapper, WorkLogMapper workLogMapper, FlowLogMapper flowLogMapper) {
        this.formService = formService;
        this.formMapper = formMapper;
        this.fieldMapper = fieldMapper;
        this.formDataService = formDataService;
        this.fieldService = fieldService;
        this.menuMapper = menuMapper;
        this.departmentMapper = departmentMapper;
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

    @Override
    @Transactional
    public boolean createMenu(int tenementId) {
        //菜单初始化
        String[] menuNames = new String[]{"设备档案/履历","经营分析报表","设备点检巡检","设备维修保修","设备维护保养","备品备件管理","基础信息"};
        int[] menuIdMax = {menuMapper.selectIdMax()};
        Arrays.stream(menuNames)
                .map(menuName -> {
                    Menu menu = new Menu();
                    menu.setTenementId(tenementId);
                    menu.setId(++menuIdMax[0]);
                    menu.setName(menuName);
                    return menu;
                })
                .forEach(menuMapper::insert);

        //表单初始化
        Map<String, Integer> menuNameMap = Arrays.stream(menuNames)
                .collect(Collectors.toMap(Function.identity(),
                        menuName -> menuMapper.selectIdByTenementIdAndName(tenementId, menuName)));
//        Map<String, Integer> menuNameMap = new HashMap<>();
//        for (String menuName : menuNames){
//            menuNameMap.put(menuName, menuMapper.selectIdByTenementIdAndName(tenementId, menuName));
//        }
        createMenuForm(menuNameMap.get("设备档案/履历"),0,tenementId,"设备档案/履历表");
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

    //模板生成器
    @Override
    @Transactional
    public boolean createFormTemplate(int tenementId, int templateTenementId) {
        // 获取字段模板
        List<Field> fieldsTemplate = fieldMapper.selectList(new QueryWrapper<Field>().eq("tenement_id", templateTenementId));

        // 生成新字段id
        List<String> fieldIds = fieldsTemplate.stream()
                .map(Field::getTypeId)
                .map(this::getFieldsId)
                .collect(Collectors.toList());

        // 获取表单模板
        List<Form> formsTemplate = formMapper.selectList(new QueryWrapper<Form>().eq("tenement_id", templateTenementId));

        // 获取流程模板
        List<Flow> flowsTemplate = flowMapper.selectList(new QueryWrapper<Flow>().eq("tenement_id", templateTenementId));

        // 取出要进行替换操作的数据库字段
        List<String> formFieldsTemplate = formsTemplate.stream()
                .map(Form::getFormFields)
                .collect(Collectors.toList());
        List<String> propertiesTemplate = formsTemplate.stream()
                .map(Form::getProperties)
                .collect(Collectors.toList());
        List<String> detailJsonTemplate = fieldsTemplate.stream()
                .map(Field::getDetailJson)
                .collect(Collectors.toList());
        List<String> viewDataTemplate = flowsTemplate.stream()
                .map(Flow::getViewData)
                .collect(Collectors.toList());
        List<String> flowNodesTemplate = flowsTemplate.stream()
                .map(Flow::getFlowNodes)
                .collect(Collectors.toList());

        // 1.替换用户id
        String oldUserId = ":["+userMapper.selectIdByTenementId(templateTenementId)+"]";
        String newUserId = ":["+userMapper.selectIdByTenementId(tenementId)+"]";
        replaceString(flowNodesTemplate, oldUserId, newUserId);
        replaceString(viewDataTemplate, oldUserId, newUserId);

        // 替换字段id、表单id和部门id
        for (int i = 0; i < fieldsTemplate.size(); i++) {
            //2.替换字段id
            Field fieldTemplate = fieldsTemplate.get(i);
            String oldFieldId = fieldTemplate.getId();
            String newFieldId = fieldIds.get(i);
            replaceString(formFieldsTemplate, oldFieldId, newFieldId);
            replaceString(propertiesTemplate, oldFieldId, newFieldId);
            replaceString(detailJsonTemplate, oldFieldId, newFieldId);
            replaceString(viewDataTemplate, oldFieldId, newFieldId);
            replaceString(flowNodesTemplate, oldFieldId, newFieldId);

            //3.替换表单id
            String oldFormId = ":"+ fieldTemplate.getFormId() +",";
            String formName = formMapper.selectFormNameById(fieldTemplate.getFormId());
            String newFormId = ":"+ formMapper.selectIdByTenementIdAndFormName(tenementId, formName) +",";
            replaceString(detailJsonTemplate, oldFormId, newFormId);

            //4.替换表单id(带引号类型)
            String oldFormIdInArray = ":\""+ fieldTemplate.getFormId() +"\",";
            String newFormIdInArray = ":\""+ formMapper.selectIdByTenementIdAndFormName(tenementId, formName) +"\",";
            replaceString(detailJsonTemplate, oldFormIdInArray, newFormIdInArray);

            //5.替换部门id
            String newDepartmentId = ":["+departmentMapper.selectIdByTenementIdAndPreId(tenementId,-1)+"],";
            String oldDepartmentId = ":["+departmentMapper.selectIdByTenementIdAndPreId(templateTenementId,-1)+"],";
            replaceString(detailJsonTemplate, oldDepartmentId, newDepartmentId);
        }

        // 批量更新表单
        List<Form> forms = formMapper.selectList(new QueryWrapper<Form>().eq("tenement_id", tenementId));
        IntStream.range(0,forms.size()).forEach(i -> {
            Form form = forms.get(i);
            form.setFormFields(formFieldsTemplate.get(i));
            form.setProperties(propertiesTemplate.get(i));
        });
        boolean batchUpdateFormResult = formService.updateBatchById(forms);

        // 批量创建字段
        List<Field> fields = IntStream.range(0, fieldsTemplate.size())
                .mapToObj(i -> {
                    Field fieldTemplate = fieldsTemplate.get(i);
                    String fieldId = fieldIds.get(i);
                    String formName = formMapper.selectFormNameById(fieldTemplate.getFormId());
                    int formId = formMapper.selectIdByTenementIdAndFormName(tenementId, formName).intValue();
                    Field field = new Field();
                    field.setId(fieldId);
                    field.setTenementId(tenementId);
                    field.setFormId(formId);
                    field.setTypeId(fieldTemplate.getTypeId());
                    field.setName(fieldTemplate.getName());
                    field.setDetailJson(detailJsonTemplate.get(i));
                    field.setCreateTime(LocalDateTime.now());
                    field.setUpdateTime(LocalDateTime.now());
                    return field;
                })
                .collect(Collectors.toList());
        boolean batchCreateFieldResult = fieldService.saveBatch(fields);

        //初始化流程
        List<String> nodeIdsNotDistinctTemplate = new ArrayList<>();
        List<String> itemIdsNotDistinctTemplate = new ArrayList<>();

        flowsTemplate.forEach((flow)->{
            String viewData = flow.getViewData();
            String flowNodes = flow.getFlowNodes();

            //获取未去重模板节点id(或边id)
            String nodeIdRegex = ":\\d{7},";
            Pattern nodeIdPattern = Pattern.compile(nodeIdRegex);
            Matcher nodeIdViewDataMatcher = nodeIdPattern.matcher(viewData);
            Matcher nodeIdFlowNodesMatcher = nodeIdPattern.matcher(flowNodes);

            while (nodeIdViewDataMatcher.find()) {
                nodeIdsNotDistinctTemplate.add(nodeIdViewDataMatcher.group());
            }
            while (nodeIdFlowNodesMatcher.find()) {
                nodeIdsNotDistinctTemplate.add(nodeIdFlowNodesMatcher.group());
            }

            //获取未去重模板itemsId
            String itemIdRegex = ":\"[\\-\\w]{21}\"";
            Pattern itemIdPattern = Pattern.compile(itemIdRegex);
            Matcher itemIdViewDataMatcher = itemIdPattern.matcher(viewData);
            Matcher itemIdFlowNodesMatcher = itemIdPattern.matcher(flowNodes);

            while (itemIdViewDataMatcher.find()) {
                itemIdsNotDistinctTemplate.add(itemIdViewDataMatcher.group());
            }
            while (itemIdFlowNodesMatcher.find()) {
                itemIdsNotDistinctTemplate.add(itemIdFlowNodesMatcher.group());
            }
        });

        //获取模板节点id和边id
        List<String> nodeIdsTemplate = nodeIdsNotDistinctTemplate.stream().distinct().collect(Collectors.toList());

        //生成新节点id和边id
        List<Integer> nodeIds = IntStream.range(0, nodeIdsTemplate.size())
                .mapToObj(i -> getNodeId())
                .collect(Collectors.toList());

        //获取模板itemId
        List<String> itemIdsTemplate = itemIdsNotDistinctTemplate.stream().distinct().collect(Collectors.toList());

        //生成新itemId
        List<String> itemIds = IntStream.range(0, itemIdsTemplate.size())
                .mapToObj(i -> getItemId())
                .collect(Collectors.toList());

        //6.替换节点id
        for (int i=0;i<nodeIds.size();i++){
            String oldNodeId = nodeIdsTemplate.get(i);
            String newNodeId = ":"+nodeIds.get(i)+",";
            replaceString(viewDataTemplate,oldNodeId,newNodeId);
            replaceString(flowNodesTemplate,oldNodeId,newNodeId);

            //单独替换flowNodes中数组格式的节点id
            String oldNodeIdInFlowNodes = ":["+oldNodeId.substring(1,8)+"]";
            String newNodeIdInFlowNodes = ":["+nodeIds.get(i)+"]";
            replaceString(flowNodesTemplate,oldNodeIdInFlowNodes,newNodeIdInFlowNodes);
        }

        //7.替换itemId
        for (int i=0;i<itemIds.size();i++){
            String oldItemId = itemIdsTemplate.get(i);
            String newItemId = ":\""+itemIds.get(i)+"\"";
            replaceString(viewDataTemplate,oldItemId,newItemId);
            replaceString(flowNodesTemplate,oldItemId,newItemId);
        }

        //批量创建流程
        List<Flow> flows = IntStream.range(0, flowsTemplate.size())
                .mapToObj(i -> {
                    Flow flowTemplate = flowsTemplate.get(i);
                    Flow flow = new Flow();
                    flow.setTenementId(tenementId);
                    flow.setEnable(false);
                    String formName = formMapper.selectFormNameById(flowTemplate.getFormId());
                    flow.setFormId(formMapper.selectIdByTenementIdAndFormName(tenementId, formName).intValue());
                    flow.setFlowNodes(flowNodesTemplate.get(i));
                    flow.setViewData(viewDataTemplate.get(i));
                    flow.setFlowProperty(flowTemplate.getFlowProperty());
                    flow.setNodeMoreProperty(null);
                    String username = userMapper.selectUserNameByTenementId(tenementId);
                    flow.setCreatePerson(username);
                    flow.setUpdatePerson(username);
                    flow.setCreateTime(LocalDateTime.now());
                    flow.setUpdateTime(LocalDateTime.now());
                    return flow;
                })
                .collect(Collectors.toList());

        boolean batchCreateFlowResult = flowService.saveBatch(flows);

        return batchUpdateFormResult && batchCreateFieldResult && batchCreateFlowResult;
    }

    // 将字符串中旧值替换为新值
    private void replaceString(List<String> list, String oldValue, String newValue) {
        for (int i = 0; i < list.size(); i++) {
            String str = list.get(i);
            if (str.contains(oldValue)) {
                str = str.replace(oldValue, newValue);
                list.set(i, str);
            }
        }
    }

    // 生成随机7位数字的id
    private Integer getNodeId(){
        int idLength = 7; // ID长度为7
        StringBuilder sb = new StringBuilder(idLength);
        Random random = new Random();
        for (int i = 0; i < idLength; i++) {
            sb.append(random.nextInt(10)); // 生成0-9的随机整数
        }
        String randomIDStr = sb.toString();
        return Integer.parseInt(randomIDStr);
    }

    // 生成21位的随机id
    private String getItemId(){
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0,21);
    }

//    表单初始化（初代版本）
//    @Override
//    @Transactional
//    public boolean createFormTemplate(int tenementId) {
//
//        //生成表单id
//        String fieldsId01 = getFieldsId(0);
//        String fieldsId02 = getFieldsId(0);
//        String fieldsId03 = getFieldsId(8);
//        String fieldsId04 = getFieldsId(0);
//        String fieldsId05 = getFieldsId(0);
//        String fieldsId06 = getFieldsId(3);
//        String fieldsId07 = getFieldsId(3);
//        String fieldsId08 = getFieldsId(8);
//        String fieldsId09 = getFieldsId(1);
//        String fieldsId10 = getFieldsId(4);
//        String fieldsId11 = getFieldsId(6);
//        String fieldsId12 = getFieldsId(0);
//        String fieldsId13 = getFieldsId(0);
//        String fieldsId14 = getFieldsId(0);
//        String fieldsId15 = getFieldsId(0);
//        String fieldsId16 = getFieldsId(0);
//        String fieldsId17 = getFieldsId(0);
//        String fieldsId18 = getFieldsId(0);
//        String fieldsId19 = getFieldsId(0);
//        String fieldsId20 = getFieldsId(0);
//        String fieldsId21 = getFieldsId(0);
//        String fieldsId22 = getFieldsId(0);
//        String fieldsId23 = getFieldsId(0);
//        String fieldsId24 = getFieldsId(0);
//        String fieldsId25 = getFieldsId(0);
//        String fieldsId26 = getFieldsId(0);
//        String fieldsId27 = getFieldsId(0);
//        String fieldsId28 = getFieldsId(0);
//        String fieldsId29 = getFieldsId(0);
//        String fieldsId30 = getFieldsId(0);
//        String fieldsId31 = getFieldsId(0);
//        String fieldsId32 = getFieldsId(0);
//        String fieldsId33 = getFieldsId(0);
//        String fieldsId34 = getFieldsId(0);
//        String fieldsId35 = getFieldsId(0);
//        String fieldsId36 = getFieldsId(0);
//        String fieldsId37 = getFieldsId(0);
//        String fieldsId38 = getFieldsId(0);
//        String fieldsId39 = getFieldsId(0);
//        String fieldsId40 = getFieldsId(1);
//        String fieldsId41 = getFieldsId(1);
//        String fieldsId42 = getFieldsId(1);
//        String fieldsId43 = getFieldsId(1);
//        String fieldsId44 = getFieldsId(1);
//        String fieldsId45 = getFieldsId(1);
//        String fieldsId46 = getFieldsId(2);
//        String fieldsId47 = getFieldsId(2);
//        String fieldsId48 = getFieldsId(3);
//        String fieldsId49 = getFieldsId(3);
//        String fieldsId50 = getFieldsId(3);
//        String fieldsId51 = getFieldsId(3);
//        String fieldsId52 = getFieldsId(3);
//        String fieldsId53 = getFieldsId(3);
//        String fieldsId54 = getFieldsId(3);
//        String fieldsId55 = getFieldsId(3);
//        String fieldsId56 = getFieldsId(3);
//        String fieldsId57 = getFieldsId(3);
//        String fieldsId58 = getFieldsId(3);
//        String fieldsId59 = getFieldsId(3);
//        String fieldsId60 = getFieldsId(3);
//        String fieldsId61 = getFieldsId(4);
//        String fieldsId62 = getFieldsId(4);
//        String fieldsId63 = getFieldsId(4);
//        String fieldsId64 = getFieldsId(4);
//        String fieldsId65 = getFieldsId(6);
//        String fieldsId66 = getFieldsId(6);
//        String fieldsId67 = getFieldsId(6);
//        String fieldsId68 = getFieldsId(6);
//        String fieldsId69 = getFieldsId(6);
//        String fieldsId70 = getFieldsId(6);
//        String fieldsId71 = getFieldsId(6);
//        String fieldsId72 = getFieldsId(6);
//        String fieldsId73 = getFieldsId(6);
//        String fieldsId74 = getFieldsId(6);
//        String fieldsId75 = getFieldsId(6);
//        String fieldsId76 = getFieldsId(6);
//        String fieldsId77 = getFieldsId(6);
//        String fieldsId78 = getFieldsId(6);
//        String fieldsId79 = getFieldsId(6);
//        String fieldsId80 = getFieldsId(6);
//        String fieldsId81 = getFieldsId(6);
//        String fieldsId82 = getFieldsId(6);
//        String fieldsId83 = getFieldsId(8);
//        String fieldsId84 = getFieldsId(14);
//        String fieldsId85 = getFieldsId(14);
//        String fieldsId86 = getFieldsId(14);
//        String fieldsId87 = getFieldsId(14);
//        String fieldsId88 = getFieldsId(14);
//        String fieldsId89 = getFieldsId(15);
//        String fieldsId90 = getFieldsId(14);
//        String fieldsId91 = getFieldsId(14);
//        String fieldsId92 = getFieldsId(15);
//        String fieldsId93 = getFieldsId(15);
//        String fieldsId94 = getFieldsId(15);
//        String fieldsId95 = getFieldsId(15);
//        String fieldsId96 = getFieldsId(15);
//        String fieldsId97 = getFieldsId(15);
//        String fieldsId98 = getFieldsId(20);
//        String fieldsId99 = getFieldsId(20);
//        String fieldsId100 = getFieldsId(20);
//        String fieldsId101 = getFieldsId(20);
//        String fieldsId102 = getFieldsId(20);
//        String fieldsId103 = getFieldsId(20);
//
//        //暂存批量修改的form表单
//        List<Form> forms = new ArrayList<>();
//
//        //设备档案/履历表
//        String formFields1 = "[{\"fieldsId\":[],\"name\":\"root\"},{\"fieldsId\":[\""+fieldsId23+"\",\""+fieldsId15+"\",\""+fieldsId31+"\",\""+fieldsId36+"\",\""+fieldsId72+"\",\""+fieldsId77+"\",\""+fieldsId70+"\",\""+fieldsId79+"\",\""+fieldsId54+"\",\""+fieldsId56+"\"],\"name\":\"设备信息\"},{\"fieldsId\":[\""+fieldsId51+"\",\""+fieldsId84+"\",\""+fieldsId80+"\"],\"name\":\"点检巡检\"},{\"fieldsId\":[\""+fieldsId60+"\",\""+fieldsId87+"\"],\"name\":\"维修保修\"},{\"fieldsId\":[\""+fieldsId58+"\",\""+fieldsId86+"\"],\"name\":\"保养维护\"},{\"fieldsId\":[\""+fieldsId85+"\"],\"name\":\"备件更换\"}]";
//        int EquipmentFilesOrResumesFormId = (int)formMapper.selectIdByTenementIdAndFormName(tenementId,"设备档案/履历表").longValue();
//        Form form1 = formMapper.selectById(EquipmentFilesOrResumesFormId);
//        form1.setFormFields(formFields1);
//        forms.add(form1);
//
//        //设备巡检单
//        String formFields2 = "[{\"fieldsId\":[],\"name\":\"root\"},{\"fieldsId\":[\""+fieldsId48+"\",\""+fieldsId98+"\"],\"name\":\"巡检信息\"},{\"fieldsId\":[\""+fieldsId95+"\",\""+fieldsId62+"\",\""+fieldsId82+"\",\""+fieldsId45+"\"],\"name\":\"巡检记录\"},{\"fieldsId\":[\""+fieldsId88+"\"],\"name\":\"报修维修\"},{\"fieldsId\":[\""+fieldsId93+"\"],\"name\":\"设备信息\"}]";
//        int equipmentInspectionFormId = (int)formMapper.selectIdByTenementIdAndFormName(tenementId,"设备巡检单").longValue();
//        Form form2 = formMapper.selectById(equipmentInspectionFormId);
//        form2.setFormFields(formFields2);
//        forms.add(form2);
//
//        //巡检方案
//        String formFields3 = "[{\"fieldsId\":[\""+fieldsId17+"\",\""+fieldsId66+"\",\""+fieldsId44+"\"],\"name\":\"root\"}]";
//        int inspectionSchemeFormId = (int)formMapper.selectIdByTenementIdAndFormName(tenementId,"巡检方案").longValue();
//        Form form3 = formMapper.selectById(inspectionSchemeFormId);
//        form3.setFormFields(formFields3);
//        forms.add(form3);
//
//        //设备报修单
//        String formFields4 = "[{\"fieldsId\":[],\"name\":\"root\"},{\"fieldsId\":[\""+fieldsId37+"\",\""+fieldsId97+"\",\""+fieldsId52+"\",\""+fieldsId43+"\",\""+fieldsId99+"\"],\"name\":\"故障维修\"},{\"fieldsId\":[\""+fieldsId50+"\",\""+fieldsId61+"\",\""+fieldsId75+"\",\""+fieldsId67+"\",\""+fieldsId102+"\"],\"name\":\"维修派工\"},{\"fieldsId\":[\""+fieldsId55+"\",\""+fieldsId65+"\",\""+fieldsId63+"\",\""+fieldsId40+"\"],\"name\":\"维修结果\"},{\"fieldsId\":[\""+fieldsId96+"\",\""+fieldsId47+"\"],\"name\":\"备件更换\"}]";
//        int EquipmentRepairFormId = (int)formMapper.selectIdByTenementIdAndFormName(tenementId,"设备报修单").longValue();
//        Form form4 = formMapper.selectById(EquipmentRepairFormId);
//        form4.setFormFields(formFields4);
//        forms.add(form4);
//
//        //设备保养单
//        String formFields5 = "[{\"fieldsId\":[],\"name\":\"root\"},{\"fieldsId\":[\""+fieldsId01+"\",\""+fieldsId02+"\"],\"name\":\"设备信息\"},{\"fieldsId\":[\""+fieldsId03+"\",\""+fieldsId04+"\",\""+fieldsId11+"\",\""+fieldsId05+"\",\""+fieldsId06+"\",\""+fieldsId07+"\",\""+fieldsId08+"\",\""+fieldsId09+"\",\""+fieldsId10+"\"],\"name\":\"保养内容\"},{\"fieldsId\":[\""+fieldsId90+"\"],\"name\":\"报修维修\"},{\"fieldsId\":[\""+fieldsId83+"\",\""+fieldsId91+"\"],\"name\":\"备件更换\"}]";
//        int equipmentMaintenanceFormId = (int)formMapper.selectIdByTenementIdAndFormName(tenementId,"设备保养单").longValue();
//        Form form5 = formMapper.selectById(equipmentMaintenanceFormId);
//        form5.setFormFields(formFields5);
//        forms.add(form5);
//
//        //保养计划基础表
//        String formFields6 = "[{\"fieldsId\":[],\"name\":\"root\"},{\"fieldsId\":[\""+fieldsId94+"\"],\"name\":\"设备信息\"},{\"fieldsId\":[\""+fieldsId28+"\",\""+fieldsId101+"\",\""+fieldsId74+"\",\""+fieldsId73+"\",\""+fieldsId42+"\"],\"name\":\"保养信息\"}]";
//        int maintenancePlanBaseFormId = (int)formMapper.selectIdByTenementIdAndFormName(tenementId,"保养计划基础表").longValue();
//        Form form6 = formMapper.selectById(maintenancePlanBaseFormId);
//        form6.setFormFields(formFields6);
//        forms.add(form6);
//
//        //备件入库单
//        String formFields7 = "[{\"fieldsId\":[\""+fieldsId21+"\",\""+fieldsId49+"\",\""+fieldsId76+"\",\""+fieldsId103+"\",\""+fieldsId89+"\",\""+fieldsId12+"\"],\"name\":\"root\"}]";
//        int SparePartsWarehouseReceiptFormId = (int)formMapper.selectIdByTenementIdAndFormName(tenementId,"备件入库单").longValue();
//        Form form7 = formMapper.selectById(SparePartsWarehouseReceiptFormId);
//        form7.setFormFields(formFields7);
//        forms.add(form7);
//
//        //备件领用单
//        String formFields8 = "[{\"fieldsId\":[\""+fieldsId20+"\",\""+fieldsId59+"\",\""+fieldsId100+"\",\""+fieldsId64+"\",\""+fieldsId92+"\",\""+fieldsId46+"\"],\"name\":\"root\"}]";
//        int sparePartsRequisitionFormId = (int)formMapper.selectIdByTenementIdAndFormName(tenementId,"备件领用单").longValue();
//        Form form8 = formMapper.selectById(sparePartsRequisitionFormId);
//        form8.setFormFields(formFields8);
//        forms.add(form8);
//
//        //备件台账
//        String formFields9 = "[{\"fieldsId\":[\""+fieldsId25+"\",\""+fieldsId18+"\",\""+fieldsId27+"\",\""+fieldsId69+"\",\""+fieldsId32+"\",\""+fieldsId41+"\"],\"name\":\"root\"}]";
//        int sparePartsLedgerFormId = (int)formMapper.selectIdByTenementIdAndFormName(tenementId,"备件台账").longValue();
//        Form form9 = formMapper.selectById(sparePartsLedgerFormId);
//        form9.setFormFields(formFields9);
//        forms.add(form9);
//
//        //机房
//        String formFields10 = "[{\"fieldsId\":[\""+fieldsId39+"\"],\"name\":\"root\"}]";
//        int machineRoomFormId = (int)formMapper.selectIdByTenementIdAndFormName(tenementId,"机房").longValue();
//        Form form10 = formMapper.selectById(machineRoomFormId);
//        form10.setFormFields(formFields10);
//        forms.add(form10);
//
//        //设备类型
//        String formFields11 = "[{\"fieldsId\":[\""+fieldsId16+"\"],\"name\":\"root\"}]";
//        int deviceTypeFormId = (int)formMapper.selectIdByTenementIdAndFormName(tenementId,"设备类型").longValue();
//        Form form11 = formMapper.selectById(deviceTypeFormId);
//        form11.setFormFields(formFields11);
//        forms.add(form11);
//
//        //部门
//        String formFields12 = "[{\"fieldsId\":[\""+fieldsId29+"\"],\"name\":\"root\"}]";
//        int departmentFormId = (int)formMapper.selectIdByTenementIdAndFormName(tenementId,"部门").longValue();
//        Form form12 = formMapper.selectById(departmentFormId);
//        form12.setFormFields(formFields12);
//        forms.add(form12);
//
//        //设备状态
//        String formFields13 = "[{\"fieldsId\":[\""+fieldsId33+"\"],\"name\":\"root\"}]";
//        int equipmentStatusFormId = (int)formMapper.selectIdByTenementIdAndFormName(tenementId,"设备状态").longValue();
//        Form form13 = formMapper.selectById(equipmentStatusFormId);
//        form13.setFormFields(formFields13);
//        forms.add(form13);
//
//        //保养等级与频次
//        String formFields14 = "[{\"fieldsId\":[\""+fieldsId13+"\",\""+fieldsId19+"\",\""+fieldsId34+"\"],\"name\":\"root\"}]";
//        int maintenanceLevelAndFrequencyFormId = (int)formMapper.selectIdByTenementIdAndFormName(tenementId,"保养等级与频次").longValue();
//        Form form14 = formMapper.selectById(maintenanceLevelAndFrequencyFormId);
//        form14.setFormFields(formFields14);
//        forms.add(form14);
//
//        //仓库
//        String formFields15 = "[{\"fieldsId\":[\""+fieldsId35+"\"],\"name\":\"root\"}]";
//        int warehouseFormId = (int)formMapper.selectIdByTenementIdAndFormName(tenementId,"仓库").longValue();
//        Form form15 = formMapper.selectById(warehouseFormId);
//        form15.setFormFields(formFields15);
//        forms.add(form15);
//
//        //单位
//        String formFields16 = "[{\"fieldsId\":[\""+fieldsId22+"\"],\"name\":\"root\"}]";
//        int unitFormId = (int)formMapper.selectIdByTenementIdAndFormName(tenementId,"单位").longValue();
//        Form form16 = formMapper.selectById(unitFormId);
//        form16.setFormFields(formFields16);
//        forms.add(form16);
//
//        //安装地点
//        String formFields17 = "[{\"fieldsId\":[\""+fieldsId26+"\"],\"name\":\"root\"}]";
//        int installationLocationFormId = (int)formMapper.selectIdByTenementIdAndFormName(tenementId,"安装地点").longValue();
//        Form form17 = formMapper.selectById(installationLocationFormId);
//        form17.setFormFields(formFields17);
//        forms.add(form17);
//
//        //设备信息
//        String formFields18 = "[{\"fieldsId\":[\""+fieldsId24+"\",\""+fieldsId30+"\",\""+fieldsId38+"\",\""+fieldsId14+"\",\""+fieldsId78+"\",\""+fieldsId71+"\",\""+fieldsId81+"\",\""+fieldsId68+"\",\""+fieldsId57+"\",\""+fieldsId53+"\"],\"name\":\"root\"}]";
//        int deviceInformationFormId = (int)formMapper.selectIdByTenementIdAndFormName(tenementId,"设备信息").longValue();
//        Form form18 = formMapper.selectById(deviceInformationFormId);
//        form18.setFormFields(formFields18);
//        forms.add(form18);
//
//        //批量更新form表单
//        formService.updateBatchById(forms);
//
//        int departmentId = departmentMapper.selectIdByTenementIdAndPreId(tenementId,-1).intValue();
//
//        //生成字段数据
//        String detailJson01 = "{\"scan\":[\"input_scan\",\"change\"],\"typeId\":\"0\",\"check\":{},\"title\":\"设备编号\",\"type\":\"string\",\"required\":true,\"fieldId\":\""+fieldsId01+"\"}";
//        String detailJson02 = "{\"typeId\":\"0\",\"check\":{},\"title\":\"设备名称\",\"type\":\"string\",\"fieldId\":\""+fieldsId02+"\"}";
//        String detailJson03 = "{\"widget\":\"self_divider\",\"typeId\":\"8\",\"title\":\"保养计划\",\"type\":\"any\",\"fieldId\":\""+fieldsId03+"\"}";
//        String detailJson04 = "{\"typeId\":\"0\",\"check\":{},\"title\":\"保养负责人\",\"type\":\"string\",\"fieldId\":\""+fieldsId04+"\"}";
//        String detailJson05 = "{\"typeId\":\"0\",\"check\":{},\"title\":\"保养频次\",\"type\":\"string\",\"fieldId\":\""+fieldsId05+"\"}";
//        String detailJson06 = "{\"widget\":\"self_datapick\",\"typeId\":\"3\",\"title\":\"本次保养时间\",\"type\":\"string\",\"fieldId\":\""+fieldsId06+"\"}";
//        String detailJson07 = "{\"widget\":\"self_datapick\",\"typeId\":\"3\",\"title\":\"下次保养时间\",\"type\":\"string\",\"fieldId\":\""+fieldsId07+"\"}";
//        String detailJson08 = "{\"widget\":\"self_divider\",\"typeId\":\"8\",\"title\":\"保养内容\",\"type\":\"any\",\"fieldId\":\""+fieldsId08+"\"}";
//        String detailJson09 = "{\"format\":\"textarea\",\"typeId\":\"1\",\"check\":{},\"title\":\"保养内容以及要求\",\"type\":\"string\",\"fieldId\":\""+fieldsId09+"\"}";
//        String detailJson10 = "{\"widget\":\"self_radio\",\"enumNames\":[\"已完成\",\"未完成\"],\"typeId\":\"4\",\"className\":\"item_column\",\"title\":\"保养结果\",\"type\":\"string\",\"enum\":[\"已完成\",\"未完成\"],\"fieldId\":\""+fieldsId10+"\"}";
//        String detailJson11 = "{\"widget\":\"self_select\",\"option_list\":[\"日常维护\",\"一级保养\",\"二级保养\",\"三级保养\"],\"typeId\":\"6\",\"check\":{},\"title\":\"保养等级\",\"type\":\"any\",\"fieldId\":\""+fieldsId11+"\"}";
//        String detailJson12 = "{\"typeId\":\"0\",\"check\":{},\"title\":\"入库数量\",\"type\":\"string\",\"fieldId\":\""+fieldsId12+"\"}";
//        String detailJson13 = "{\"typeId\":\"0\",\"check\":{},\"title\":\"保养等级\",\"type\":\"string\"}";
//        String detailJson14 = "{\"typeId\":\"0\",\"check\":{},\"title\":\"规格型号\",\"type\":\"string\",\"fieldId\":\""+fieldsId14+"\"}";
//        String detailJson15 = "{\"width\":\"100%\",\"typeId\":\"0\",\"check\":{},\"title\":\"设备名称\",\"type\":\"string\",\"required\":false,\"fieldId\":\""+fieldsId15+"\"}";
//        String detailJson16 = "{\"typeId\":\"0\",\"check\":{},\"title\":\"设备类型\",\"type\":\"string\",\"fieldId\":\""+fieldsId16+"\"}";
//        String detailJson17 = "{\"option_list\":[\"选项一\",\"选项二\",\"选项三\"],\"typeId\":\"0\",\"check\":{},\"title\":\"巡检方案名称\",\"type\":\"string\",\"fieldId\":\""+fieldsId17+"\"}";
//        String detailJson18 = "{\"typeId\":\"0\",\"check\":{},\"title\":\"备件名称\",\"type\":\"string\"}";
//        String detailJson19 = "{\"typeId\":\"0\",\"check\":{},\"title\":\"保养频次\",\"type\":\"string\"}";
//        String detailJson20 = "{\"scan\":[\"input_scan\",\"change\"],\"typeId\":\"0\",\"check\":{},\"title\":\"备件领用单号\",\"type\":\"string\",\"required\":true,\"fieldId\":\""+fieldsId20+"\"}";
//        String detailJson21 = "{\"width\":\"100%\",\"scan\":[\"input_scan\",\"change\"],\"typeId\":\"0\",\"check\":{},\"title\":\"备件入库单号\",\"type\":\"string\",\"fieldId\":\""+fieldsId21+"\"}";
//        String detailJson22 = "{\"typeId\":\"0\",\"check\":{},\"title\":\"单位\",\"type\":\"string\"}";
//        String detailJson23 = "{\"width\":\"100%\",\"typeId\":\"0\",\"check\":{},\"title\":\"设备编号\",\"type\":\"string\",\"required\":false,\"fieldId\":\""+fieldsId23+"\"}";
//        String detailJson24 = "{\"option_list\":[\"选项一\",\"选项二\",\"选项三\"],\"scan\":[\"input_scan\",\"change\"],\"typeId\":\"0\",\"check\":{},\"title\":\"设备编号\",\"type\":\"string\",\"required\":true,\"fieldId\":\""+fieldsId24+"\"}";
//        String detailJson25 = "{\"scan\":[\"input_scan\",\"change\"],\"typeId\":\"0\",\"check\":{},\"title\":\"备件编号\",\"type\":\"string\"}";
//        String detailJson26 = "{\"typeId\":\"0\",\"check\":{},\"title\":\"安装地点\",\"type\":\"string\"}";
//        String detailJson27 = "{\"typeId\":\"0\",\"check\":{},\"title\":\"规格型号\",\"type\":\"string\"}";
//        String detailJson28 = "{\"typeId\":\"0\",\"check\":{},\"title\":\"保养计划名称\",\"type\":\"string\",\"required\":true}";
//        String detailJson29 = "{\"typeId\":\"0\",\"check\":{},\"title\":\"部门\",\"type\":\"string\"}";
//        String detailJson30 = "{\"scan\":[],\"typeId\":\"0\",\"check\":{},\"title\":\"设备名称\",\"type\":\"string\",\"required\":true,\"fieldId\":\""+fieldsId30+"\"}";
//        String detailJson31 = "{\"width\":\"100%\",\"typeId\":\"0\",\"check\":{},\"title\":\"设备厂商\",\"type\":\"string\",\"fieldId\":\""+fieldsId31+"\"}";
//        String detailJson32 = "{\"option_list\":[\"选项一\",\"选项二\",\"选项三\"],\"typeId\":\"0\",\"check\":{},\"title\":\"生产厂家\",\"type\":\"string\"}";
//        String detailJson33 = "{\"typeId\":\"0\",\"check\":{},\"title\":\"设备状态\",\"type\":\"string\"}";
//        String detailJson34 = "{\"typeId\":\"0\",\"check\":{},\"title\":\"间隔天数\",\"type\":\"string\"}";
//        String detailJson35 = "{\"typeId\":\"0\",\"check\":{},\"title\":\"仓库\",\"type\":\"string\"}";
//        String detailJson36 = "{\"width\":\"100%\",\"typeId\":\"0\",\"check\":{},\"title\":\"规格型号\",\"type\":\"string\",\"fieldId\":\""+fieldsId36+"\"}";
//        String detailJson37 = "{\"scan\":[\"input_scan\",\"change\"],\"typeId\":\"0\",\"check\":{},\"title\":\"维修工单\",\"type\":\"string\",\"required\":true,\"fieldId\":\""+fieldsId37+"\"}";
//        String detailJson38 = "{\"typeId\":\"0\",\"check\":{},\"title\":\"设备厂商\",\"type\":\"string\",\"fieldId\":\""+fieldsId38+"\"}";
//        String detailJson39 = "{\"typeId\":\"0\",\"check\":{},\"title\":\"机房\",\"type\":\"string\",\"fieldId\":\""+fieldsId39+"\"}";
//        String detailJson40 = "{\"widget\":\"self_textarea\",\"typeId\":\"1\",\"check\":{},\"title\":\"维修情况说明\",\"type\":\"string\",\"fieldId\":\""+fieldsId40+"\"}";
//        String detailJson41 = "{\"widget\":\"self_textarea\",\"typeId\":\"1\",\"check\":{},\"title\":\"备注\",\"type\":\"string\"}";
//        String detailJson42 = "{\"widget\":\"self_textarea\",\"option_list\":[\"选项一\",\"选项二\",\"选项三\"],\"typeId\":\"1\",\"describe\":\"<p></p>\",\"check\":{},\"title\":\"保养内容及要求\",\"type\":\"string\",\"required\":false,\"fieldId\":\""+fieldsId42+"\"}";
//        String detailJson43 = "{\"widget\":\"self_textarea\",\"typeId\":\"1\",\"check\":{},\"title\":\"故障简述\",\"type\":\"string\",\"fieldId\":\""+fieldsId43+"\"}";
//        String detailJson44 = "{\"widget\":\"self_textarea\",\"option_list\":[\"选项一\",\"选项二\",\"选项三\"],\"width\":\"100%\",\"typeId\":\"1\",\"check\":{},\"title\":\"巡检内容\",\"type\":\"string\"}";
//        String detailJson45 = "{\"widget\":\"self_textarea\",\"option_list\":[\"选项一\",\"选项二\",\"选项三\"],\"typeId\":\"1\",\"describe\":\"<p><span style=\\\"color:#d9534f\\\"><span style=\\\"font-size:16px\\\"><span style=\\\"background-color:#ffffff\\\"><strong>当巡检异常时，请发起维修工单</strong></span></span></span></p>\",\"check\":{},\"title\":\"本次巡检总结\",\"type\":\"string\",\"fieldId\":\""+fieldsId45+"\"}";
//        String detailJson46 = "{\"widget\":\"self_number\",\"typeId\":\"2\",\"title\":\"领用数量\",\"type\":\"number\",\"self_pattern\":{\"show\":[false,false],\"type\":1},\"fieldId\":\""+fieldsId46+"\"}";
//        String detailJson47 = "{\"widget\":\"self_number\",\"typeId\":\"2\",\"title\":\"更换使用数量\",\"type\":\"number\",\"self_pattern\":{\"show\":[false,false],\"type\":1},\"fieldId\":\""+fieldsId47+"\"}";
//        String detailJson48 = "{\"widget\":\"self_datapick\",\"typeId\":\"3\",\"title\":\"巡检时间\",\"type\":\"string\",\"fieldId\":\""+fieldsId48+"\"}";
//        String detailJson49 = "{\"widget\":\"self_datapick\",\"width\":\"100%\",\"format\":\"date\",\"typeId\":\"3\",\"title\":\"入库日期\",\"type\":\"string\",\"fieldId\":\""+fieldsId49+"\"}";
//        String detailJson50 = "{\"widget\":\"self_datapick\",\"typeId\":\"3\",\"title\":\"派工时间\",\"type\":\"string\",\"fieldId\":\""+fieldsId50+"\"}";
//        String detailJson51 = "{\"widget\":\"self_datapick\",\"width\":\"32%\",\"typeId\":\"3\",\"title\":\"最近巡检\",\"type\":\"string\",\"fieldId\":\""+fieldsId51+"\"}";
//        String detailJson52 = "{\"widget\":\"self_datapick\",\"format\":\"date\",\"typeId\":\"3\",\"title\":\"报修时间\",\"type\":\"string\",\"fieldId\":\""+fieldsId52+"\"}";
//        String detailJson53 = "{\"widget\":\"self_datapick\",\"format\":\"date\",\"typeId\":\"3\",\"title\":\"启用时间\",\"type\":\"string\",\"fieldId\":\""+fieldsId53+"\"}";
//        String detailJson54 = "{\"widget\":\"self_datapick\",\"option_list\":[\"选项一\",\"选项二\",\"选项三\"],\"width\":\"100%\",\"typeId\":\"3\",\"title\":\"购买时间\",\"type\":\"string\",\"fieldId\":\""+fieldsId54+"\"}";
//        String detailJson55 = "{\"widget\":\"self_datapick\",\"typeId\":\"3\",\"title\":\"维修完成时间\",\"type\":\"string\",\"fieldId\":\""+fieldsId55+"\"}";
//        String detailJson56 = "{\"widget\":\"self_datapick\",\"width\":\"100%\",\"typeId\":\"3\",\"title\":\"启用时间\",\"type\":\"string\",\"fieldId\":\""+fieldsId56+"\"}";
//        String detailJson57 = "{\"widget\":\"self_datapick\",\"option_list\":[\"选项一\",\"选项二\",\"选项三\"],\"format\":\"date\",\"typeId\":\"3\",\"title\":\"购买时间\",\"type\":\"string\",\"fieldId\":\""+fieldsId57+"\"}";
//        String detailJson58 = "{\"widget\":\"self_datapick\",\"typeId\":\"3\",\"title\":\"最近保养\",\"type\":\"string\",\"fieldId\":\""+fieldsId58+"\"}";
//        String detailJson59 = "{\"widget\":\"self_datapick\",\"format\":\"date\",\"typeId\":\"3\",\"title\":\"领用日期\",\"type\":\"string\",\"fieldId\":\""+fieldsId59+"\"}";
//        String detailJson60 = "{\"widget\":\"self_datapick\",\"typeId\":\"3\",\"title\":\"最近维修\",\"type\":\"string\",\"fieldId\":\""+fieldsId60+"\"}";
//        String detailJson61 = "{\"widget\":\"self_radio\",\"enumNames\":[\"特别紧急\",\"紧急\",\"一般\",\"不急\"],\"typeId\":\"4\",\"className\":\"item_column\",\"title\":\"故障等级\",\"type\":\"string\",\"enum\":[\"特别紧急\",\"紧急\",\"一般\",\"不急\"],\"fieldId\":\""+fieldsId61+"\"}";
//        String detailJson62 = "{\"widget\":\"self_radio\",\"enumNames\":[\"正常\",\"异常\"],\"typeId\":\"4\",\"className\":\"item_column\",\"title\":\"巡检结果\",\"type\":\"string\",\"enum\":[\"正常\",\"异常\"],\"fieldId\":\""+fieldsId62+"\"}";
//        String detailJson63 = "{\"widget\":\"self_radio\",\"enumNames\":[\"维修完成\",\"无法修复\"],\"option_list\":[\"选项一\",\"选项二\",\"选项三\"],\"typeId\":\"4\",\"className\":\"item_column\",\"title\":\"维修结果\",\"type\":\"string\",\"enum\":[\"维修完成\",\"无法修复\"],\"fieldId\":\""+fieldsId63+"\"}";
//        String detailJson64 = "{\"widget\":\"self_radio\",\"enumNames\":[\"设备维修\",\"设备保养\"],\"typeId\":\"4\",\"className\":\"item_column\",\"self_setting\":{\"mul\":false,\"scope\":{\"role\":[],\"department\":[],\"user\":[]},\"judge\":false,\"type\":\"1\"},\"title\":\"领用目的\",\"type\":\"string\",\"enum\":[\"设备维修\",\"设备保养\"],\"fieldId\":\""+fieldsId64+"\"}";
//        String detailJson65 = "{\"widget\":\"self_select\",\"option_list\":[\"自然磨损\",\"违章操作\",\"配件质量问题\",\"选项维护保养不到位\",\"其他\"],\"typeId\":\"6\",\"check\":{},\"title\":\"故障原因\",\"type\":\"any\",\"fieldId\":\""+fieldsId65+"\"}";
//        String detailJson66 = "{\"widget\":\"self_select\",\"option_list\":[\"一天一检\",\"一天二检\",\"一周一检\"],\"typeId\":\"6\",\"check\":{},\"title\":\"巡检时间频次\",\"type\":\"any\"}";
//        String detailJson67 = "{\"widget\":\"self_select\",\"option_list\":[\"电气维修组\",\"机械维修组\",\"不限\"],\"typeId\":\"6\",\"check\":{},\"title\":\"维修班组\",\"type\":\"any\",\"fieldId\":\""+fieldsId67+"\"}";
//        String detailJson68 = "{\"widget\":\"self_select\",\"option_type\":\"2\",\"option_list\":{\"formId\":\""+deviceTypeFormId+"\",\"fieldId\":\""+fieldsId16+"\"},\"typeId\":\"6\",\"check\":{},\"title\":\"设备类型\",\"type\":\"any\",\"fieldId\":\""+fieldsId68+"\"}";
//        String detailJson69 = "{\"widget\":\"self_select\",\"option_type\":\"2\",\"option_list\":{\"formId\":\""+unitFormId+"\",\"fieldId\":\""+fieldsId22+"\"},\"typeId\":\"6\",\"check\":{},\"title\":\"单位\",\"type\":\"any\"}";
//        String detailJson70 = "{\"widget\":\"self_select\",\"option_list\":{\"formId\":\""+installationLocationFormId+"\",\"fieldId\":\""+fieldsId26+"\"},\"option_type\":\"2\",\"width\":\"100%\",\"typeId\":\"6\",\"check\":{},\"title\":\"安装地点\",\"type\":\"any\",\"fieldId\":\""+fieldsId70+"\"}";
//        String detailJson71 = "{\"widget\":\"self_select\",\"option_type\":\"2\",\"option_list\":{\"formId\":\""+machineRoomFormId+"\",\"fieldId\":\""+fieldsId39+"\"},\"typeId\":\"6\",\"check\":{},\"title\":\"所属车间\",\"type\":\"any\",\"fieldId\":\""+fieldsId71+"\"}";
//        String detailJson72 = "{\"widget\":\"self_select\",\"option_list\":{\"formId\":\""+equipmentStatusFormId+"\",\"fieldId\":\""+fieldsId33+"\"},\"option_type\":\"2\",\"width\":\"100%\",\"typeId\":\"6\",\"check\":{},\"title\":\"设备状态\",\"type\":\"any\",\"fieldId\":\""+fieldsId72+"\"}";
//        String detailJson73 = "{\"widget\":\"self_select\",\"option_list\":[\"一天一次\",\"一周一次\",\"一周三次\",\"一月一次\"],\"typeId\":\"6\",\"check\":{},\"title\":\"保养频次\",\"type\":\"any\",\"fieldId\":\""+fieldsId73+"\"}";
//        String detailJson74 = "{\"widget\":\"self_select\",\"option_type\":\"2\",\"option_list\":{\"formId\":\""+maintenanceLevelAndFrequencyFormId+"\",\"fieldId\":\""+fieldsId13+"\"},\"typeId\":\"6\",\"check\":{},\"self_setting\":{\"mul\":false,\"scope\":{\"role\":[],\"department\":[],\"user\":[]},\"judge\":false,\"type\":\"1\"},\"title\":\"保养等级\",\"type\":\"any\",\"fieldId\":\""+fieldsId74+"\"}";
//        String detailJson75 = "{\"widget\":\"self_select\",\"option_list\":[\"电气故障\",\"机械故障\",\"物料原因故障\",\"能源供给故障\",\"其他故障\"],\"typeId\":\"6\",\"check\":{},\"title\":\"故障类别\",\"type\":\"any\",\"fieldId\":\""+fieldsId75+"\"}";
//        String detailJson76 = "{\"widget\":\"self_select\",\"option_type\":\"2\",\"option_list\":{\"formId\":\""+warehouseFormId+"\",\"fieldId\":\""+fieldsId35+"\"},\"typeId\":\"6\",\"check\":{},\"title\":\"入库仓库\",\"type\":\"any\",\"fieldId\":\""+fieldsId76+"\"}";
//        String detailJson77 = "{\"widget\":\"self_select\",\"option_list\":{\"formId\":\""+machineRoomFormId+"\",\"fieldId\":\""+fieldsId39+"\"},\"option_type\":\"2\",\"width\":\"100%\",\"typeId\":\"6\",\"check\":{},\"title\":\"所属车间\",\"type\":\"any\",\"fieldId\":\""+fieldsId77+"\"}";
//        String detailJson78 = "{\"widget\":\"self_select\",\"option_type\":\"2\",\"option_list\":{\"formId\":\""+equipmentStatusFormId+"\",\"fieldId\":\""+fieldsId33+"\"},\"typeId\":\"6\",\"check\":{},\"title\":\"设备状态\",\"type\":\"any\",\"fieldId\":\""+fieldsId78+"\"}";
//        String detailJson79 = "{\"widget\":\"self_select\",\"option_list\":{\"formId\":\""+deviceTypeFormId+"\",\"fieldId\":\""+fieldsId16+"\"},\"option_type\":\"2\",\"width\":\"100%\",\"typeId\":\"6\",\"check\":{},\"title\":\"设备类型\",\"type\":\"any\",\"fieldId\":\""+fieldsId79+"\"}";
//        String detailJson80 = "{\"widget\":\"self_select\",\"option_type\":\"2\",\"option_list\":{\"formId\":\""+inspectionSchemeFormId+"\",\"fieldId\":\""+fieldsId17+"\"},\"typeId\":\"6\",\"check\":{},\"title\":\"巡检方案\",\"type\":\"any\",\"fieldId\":\""+fieldsId80+"\"}";
//        String detailJson81 = "{\"widget\":\"self_select\",\"option_type\":\"2\",\"option_list\":{\"formId\":\""+installationLocationFormId+"\",\"fieldId\":\""+fieldsId26+"\"},\"typeId\":\"6\",\"check\":{},\"title\":\"安装地点\",\"type\":\"any\",\"fieldId\":\""+fieldsId81+"\"}";
//        String detailJson82 = "{\"widget\":\"self_select\",\"option_type\":\"2\",\"option_list\":{\"formId\":\""+equipmentStatusFormId+"\",\"fieldId\":\""+fieldsId33+"\"},\"typeId\":\"6\",\"check\":{},\"title\":\"设备状态\",\"type\":\"any\",\"fieldId\":\""+fieldsId82+"\"}";
//        String detailJson83 = "{\"widget\":\"self_divider\",\"typeId\":\"8\",\"describe\":\"<p><span style=\\\"background-color:#ffffff\\\"><span style=\\\"font-size:18px\\\"><span style=\\\"color:#f0ad4e\\\"><strong>若保养过程中需要更换备品备件，请在此记录</strong></span></span></span></p>\",\"title\":\"\",\"type\":\"any\",\"fieldId\":\""+fieldsId83+"\"}";
//        String detailJson84 = "{\"widget\":\"self_linkquery\",\"linkquery_condition\":{\"formId\":"+equipmentInspectionFormId+",\"originId\":\""+fieldsId84+"\",\"restrictType\":\"and\",\"mul\":\"mul\",\"conditions\":[],\"fieldIds\":[\""+fieldsId62+"\",\""+fieldsId98+"\",\""+fieldsId82+"\",\""+fieldsId48+"\"]},\"typeId\":\"14\",\"title\":\"巡检记录\",\"type\":\"any\",\"fieldId\":\""+fieldsId84+"\"}";
//        String detailJson85 = "{\"widget\":\"self_linkquery\",\"linkquery_condition\":{\"formId\":"+sparePartsRequisitionFormId+",\"originId\":\""+fieldsId85+"\",\"restrictType\":\"and\",\"mul\":\"mul\",\"conditions\":[],\"fieldIds\":[\""+fieldsId20+"\",\""+fieldsId100+"\",\""+fieldsId46+"\",\""+fieldsId64+"\"]},\"typeId\":\"14\",\"title\":\"备件更换记录\",\"type\":\"any\",\"fieldId\":\""+fieldsId85+"\"}";
//        String detailJson86 = "{\"widget\":\"self_linkquery\",\"hidden\":false,\"linkquery_condition\":{\"formId\":"+equipmentMaintenanceFormId+",\"mul\":\"mul\",\"conditions\":[],\"fieldIds\":[\""+fieldsId10+"\",\""+fieldsId11+"\",\""+fieldsId05+"\",\""+fieldsId06+"\"]},\"typeId\":\"14\",\"describe\":\"<p></p>\",\"title\":\"保养维护记录\",\"type\":\"any\",\"fieldId\":\""+fieldsId86+"\"}";
//        String detailJson87 = "{\"widget\":\"self_linkquery\",\"linkquery_condition\":{\"formId\":"+EquipmentRepairFormId+",\"originId\":\""+fieldsId87+"\",\"restrictType\":\"and\",\"mul\":\"mul\",\"conditions\":[],\"fieldIds\":[\""+fieldsId37+"\",\""+fieldsId102+"\",\""+fieldsId63+"\",\""+fieldsId55+"\"]},\"typeId\":\"14\",\"title\":\"维修保修记录\",\"type\":\"any\",\"fieldId\":\""+fieldsId87+"\"}";
//        String detailJson88 = "{\"widget\":\"self_linkquery\",\"linkquery_condition\":{\"formId\":"+EquipmentRepairFormId+",\"originId\":\""+fieldsId88+"\",\"restrictType\":\"and\",\"mul\":\"mul\",\"conditions\":[],\"fieldIds\":[\""+fieldsId37+"\",\""+fieldsId52+"\",\""+fieldsId43+"\",\""+fieldsId99+"\",\""+fieldsId75+"\"]},\"typeId\":\"14\",\"describe\":\"<p><span style=\\\"color:#d9534f\\\"><span style=\\\"font-size:16px\\\"><span style=\\\"background-color:#ffffff\\\"><strong>当巡检异常时，请发起维修工单</strong></span></span></span></p>\",\"title\":\"维修记录\",\"type\":\"any\",\"fieldId\":\""+fieldsId88+"\"}";
//        String detailJson89 = "{\"widget\":\"self_linkquery\",\"linkquery_condition\":{\"formId\":"+sparePartsLedgerFormId+",\"originId\":\""+fieldsId89+"\",\"restrictType\":\"and\",\"conditions\":[],\"fieldIds\":[\""+fieldsId25+"\",\""+fieldsId18+"\",\""+fieldsId27+"\",\""+fieldsId69+"\"],\"fieldShow\":[\""+fieldsId25+"\",\""+fieldsId18+"\",\""+fieldsId27+"\",\""+fieldsId69+"\"]},\"typeId\":\"15\",\"self_setting\":{\"mul\":false,\"scope\":{\"role\":[],\"department\":[],\"user\":[]},\"judge\":false,\"type\":\"1\"},\"title\":\"入库明细\",\"type\":\"any\",\"fieldId\":\""+fieldsId89+"\"}";
//        String detailJson90 = "{\"widget\":\"self_linkquery\",\"linkquery_condition\":{\"formId\":"+EquipmentRepairFormId+",\"originId\":\""+fieldsId90+"\",\"restrictType\":\"and\",\"mul\":\"mul\",\"conditions\":[],\"fieldIds\":[\""+fieldsId37+"\",\""+fieldsId52+"\",\""+fieldsId43+"\",\""+fieldsId99+"\",\""+fieldsId75+"\"]},\"typeId\":\"14\",\"describe\":\"<p><span style=\\\"color:#d9534f\\\"><span style=\\\"font-size:16px\\\"><span style=\\\"background-color:#ffffff\\\"><strong>当巡检异常时，请发起维修工单</strong></span></span></span></p>\",\"title\":\"维修记录\",\"type\":\"any\",\"fieldId\":\""+fieldsId90+"\"}";
//        String detailJson91 = "{\"widget\":\"self_linkquery\",\"linkquery_condition\":{\"formId\":"+sparePartsRequisitionFormId+",\"originId\":\""+fieldsId91+"\",\"restrictType\":\"and\",\"mul\":\"mul\",\"conditions\":[],\"fieldIds\":[\""+fieldsId20+"\",\""+fieldsId59+"\",\""+fieldsId100+"\",\""+fieldsId46+"\",\""+fieldsId64+"\"]},\"typeId\":\"14\",\"title\":\"备件更换\",\"type\":\"any\",\"fieldId\":\""+fieldsId91+"\"}";
//        String detailJson92 = "{\"widget\":\"self_linkquery\",\"linkquery_condition\":{\"formId\":"+sparePartsLedgerFormId+",\"originId\":\""+fieldsId92+"\",\"restrictType\":\"and\",\"conditions\":[],\"fieldIds\":[\""+fieldsId25+"\",\""+fieldsId18+"\",\""+fieldsId27+"\",\""+fieldsId69+"\"],\"fieldShow\":[\""+fieldsId25+"\",\""+fieldsId18+"\",\""+fieldsId27+"\",\""+fieldsId69+"\"]},\"typeId\":\"15\",\"title\":\"领用明细\",\"type\":\"any\",\"fieldId\":\""+fieldsId92+"\"}";
//        String detailJson93 = "{\"widget\":\"self_linkquery\",\"linkquery_condition\":{\"formId\":"+deviceInformationFormId+",\"originId\":\""+fieldsId93+"\",\"restrictType\":\"and\",\"conditions\":[],\"fieldIds\":[\""+fieldsId24+"\",\""+fieldsId30+"\",\""+fieldsId38+"\",\""+fieldsId14+"\",\""+fieldsId78+"\",\""+fieldsId71+"\",\""+fieldsId81+"\"],\"fieldShow\":[\""+fieldsId24+"\",\""+fieldsId30+"\",\""+fieldsId38+"\",\""+fieldsId14+"\",\""+fieldsId78+"\",\""+fieldsId71+"\",\""+fieldsId81+"\"]},\"typeId\":\"15\",\"title\":\"关联设备\",\"type\":\"any\",\"fieldId\":\""+fieldsId93+"\"}";
//        String detailJson94 = "{\"widget\":\"self_linkquery\",\"linkquery_condition\":{\"formId\":"+deviceInformationFormId+",\"originId\":\""+fieldsId94+"\",\"restrictType\":\"and\",\"conditions\":[],\"fieldIds\":[\""+fieldsId24+"\",\""+fieldsId30+"\",\""+fieldsId38+"\",\""+fieldsId14+"\",\""+fieldsId78+"\",\""+fieldsId71+"\",\""+fieldsId81+"\"],\"fieldShow\":[\""+fieldsId24+"\",\""+fieldsId30+"\",\""+fieldsId38+"\",\""+fieldsId14+"\",\""+fieldsId78+"\",\""+fieldsId71+"\"]},\"typeId\":\"15\",\"title\":\"设备基础信息\",\"type\":\"any\",\"fieldId\":\""+fieldsId94+"\"}";
//        String detailJson95 = "{\"widget\":\"self_linkquery\",\"linkquery_condition\":{\"formId\":"+inspectionSchemeFormId+",\"originId\":\""+fieldsId95+"\",\"restrictType\":\"and\",\"conditions\":[],\"fieldIds\":[\""+fieldsId17+"\",\""+fieldsId66+"\",\""+fieldsId44+"\"],\"fieldShow\":[\""+fieldsId17+"\",\""+fieldsId66+"\",\""+fieldsId44+"\"]},\"typeId\":\"15\",\"title\":\"关联巡检方案\",\"type\":\"any\",\"fieldId\":\""+fieldsId95+"\"}";
//        String detailJson96 = "{\"widget\":\"self_linkquery\",\"linkquery_condition\":{\"formId\":"+sparePartsLedgerFormId+",\"originId\":\""+fieldsId96+"\",\"restrictType\":\"and\",\"conditions\":[],\"fieldIds\":[\""+fieldsId25+"\",\""+fieldsId18+"\",\""+fieldsId27+"\",\""+fieldsId69+"\"],\"fieldShow\":[\""+fieldsId25+"\",\""+fieldsId18+"\",\""+fieldsId27+"\",\""+fieldsId69+"\"]},\"typeId\":\"15\",\"describe\":\"<p><span style=\\\"color:#fdda00\\\"><span style=\\\"font-size:18px\\\"><strong>若需要更换备件请在此记录</strong></span></span></p>\",\"title\":\"备件更换\",\"type\":\"any\",\"fieldId\":\""+fieldsId96+"\"}";
//        String detailJson97 = "{\"widget\":\"self_linkquery\",\"linkquery_condition\":{\"formId\":"+deviceInformationFormId+",\"originId\":\""+fieldsId97+"\",\"restrictType\":\"and\",\"conditions\":[],\"fieldIds\":[\""+fieldsId24+"\",\""+fieldsId30+"\",\""+fieldsId78+"\"],\"fieldShow\":[\""+fieldsId24+"\",\""+fieldsId30+"\",\""+fieldsId78+"\"]},\"typeId\":\"15\",\"title\":\"关联设备\",\"type\":\"any\",\"fieldId\":\""+fieldsId97+"\"}";
//        String detailJson98 = "{\"widget\":\"self_department_user\",\"typeId\":\"20\",\"self_setting\":{\"mul\":false,\"scope\":{\"role\":[],\"department\":["+departmentId+"],\"user\":[]},\"judge\":false,\"type\":\"1\"},\"check\":{},\"title\":\"巡检人员\",\"type\":\"any\",\"fieldId\":\""+fieldsId98+"\"}";
//        String detailJson99 = "{\"widget\":\"self_department_user\",\"typeId\":\"20\",\"self_setting\":{\"mul\":false,\"scope\":{\"role\":[],\"department\":["+departmentId+"],\"user\":[]},\"judge\":false,\"type\":\"1\"},\"check\":{},\"title\":\"报修人\",\"type\":\"any\",\"fieldId\":\""+fieldsId99+"\"}";
//        String detailJson100 = "{\"widget\":\"self_department_user\",\"typeId\":\"20\",\"self_setting\":{\"mul\":false,\"scope\":{\"role\":[],\"department\":["+departmentId+"],\"user\":[]},\"judge\":false,\"type\":\"1\"},\"check\":{},\"title\":\"领用人\",\"type\":\"any\",\"fieldId\":\""+fieldsId100+"\"}";
//        String detailJson101 = "{\"widget\":\"self_department_user\",\"typeId\":\"20\",\"self_setting\":{\"mul\":false,\"scope\":{\"role\":[],\"department\":["+departmentId+"],\"user\":[]},\"judge\":false,\"type\":\"1\"},\"check\":{},\"title\":\"保养人\",\"type\":\"any\",\"fieldId\":\""+fieldsId101+"\"}";
//        String detailJson102 = "{\"widget\":\"self_department_user\",\"option_list\":[\"选项一\",\"选项二\",\"选项三\"],\"typeId\":\"20\",\"self_setting\":{\"mul\":false,\"scope\":{\"role\":[],\"department\":["+departmentId+"],\"user\":[]},\"judge\":false,\"type\":\"1\"},\"check\":{},\"title\":\"维修责任人\",\"type\":\"any\",\"fieldId\":\""+fieldsId102+"\"}";
//        String detailJson103 = "{\"widget\":\"self_department_user\",\"typeId\":\"20\",\"self_setting\":{\"mul\":false,\"scope\":{\"role\":[],\"department\":["+departmentId+"],\"user\":[]},\"judge\":false,\"type\":\"1\"},\"check\":{},\"title\":\"入库人\",\"type\":\"any\",\"fieldId\":\""+fieldsId103+"\"}";
//
//
//        //字段初始化
//        createField(fieldsId01,tenementId,equipmentMaintenanceFormId,"设备编号",0,detailJson01);
//        createField(fieldsId02,tenementId,equipmentMaintenanceFormId,"设备名称",0,detailJson02);
//        createField(fieldsId03,tenementId,equipmentMaintenanceFormId,"保养计划",8,detailJson03);
//        createField(fieldsId04,tenementId,equipmentMaintenanceFormId,"保养负责人",0,detailJson04);
//        createField(fieldsId05,tenementId,equipmentMaintenanceFormId,"保养频次",0,detailJson05);
//        createField(fieldsId06,tenementId,equipmentMaintenanceFormId,"本次保养时间",3,detailJson06);
//        createField(fieldsId07,tenementId,equipmentMaintenanceFormId,"下次保养时间",3,detailJson07);
//        createField(fieldsId08,tenementId,equipmentMaintenanceFormId,"保养内容",8,detailJson08);
//        createField(fieldsId09,tenementId,equipmentMaintenanceFormId,"保养内容以及要求",1,detailJson09);
//        createField(fieldsId10,tenementId,equipmentMaintenanceFormId,"保养结果",4,detailJson10);
//        createField(fieldsId11,tenementId,equipmentMaintenanceFormId,"保养等级",6,detailJson11);
//        createField(fieldsId12,tenementId,SparePartsWarehouseReceiptFormId,"入库数量",0,detailJson12);
//        createField(fieldsId13,tenementId,maintenanceLevelAndFrequencyFormId,"保养等级",0,detailJson13);
//        createField(fieldsId14,tenementId,deviceInformationFormId,"规格型号",0,detailJson14);
//        createField(fieldsId15,tenementId,EquipmentFilesOrResumesFormId,"设备名称",0,detailJson15);
//        createField(fieldsId16,tenementId,deviceTypeFormId,"设备类型",0,detailJson16);
//        createField(fieldsId17,tenementId,inspectionSchemeFormId,"巡检方案名称",0,detailJson17);
//        createField(fieldsId18,tenementId,sparePartsLedgerFormId,"备件名称",0,detailJson18);
//        createField(fieldsId19,tenementId,maintenanceLevelAndFrequencyFormId,"保养频次",0,detailJson19);
//        createField(fieldsId20,tenementId,sparePartsRequisitionFormId,"备件领用单号",0,detailJson20);
//        createField(fieldsId21,tenementId,SparePartsWarehouseReceiptFormId,"备件入库单号",0,detailJson21);
//        createField(fieldsId22,tenementId,unitFormId,"单位",0,detailJson22);
//        createField(fieldsId23,tenementId,EquipmentFilesOrResumesFormId,"设备编号",0,detailJson23);
//        createField(fieldsId24,tenementId,deviceInformationFormId,"设备编号",0,detailJson24);
//        createField(fieldsId25,tenementId,sparePartsLedgerFormId,"备件编号",0,detailJson25);
//        createField(fieldsId26,tenementId,installationLocationFormId,"安装地点",0,detailJson26);
//        createField(fieldsId27,tenementId,sparePartsLedgerFormId,"规格型号",0,detailJson27);
//        createField(fieldsId28,tenementId,maintenancePlanBaseFormId,"保养计划名称",0,detailJson28);
//        createField(fieldsId29,tenementId,departmentFormId,"部门",0,detailJson29);
//        createField(fieldsId30,tenementId,deviceInformationFormId,"设备名称",0,detailJson30);
//        createField(fieldsId31,tenementId,EquipmentFilesOrResumesFormId,"设备厂商",0,detailJson31);
//        createField(fieldsId32,tenementId,sparePartsLedgerFormId,"生产厂家",0,detailJson32);
//        createField(fieldsId33,tenementId,equipmentStatusFormId,"设备状态",0,detailJson33);
//        createField(fieldsId34,tenementId,maintenanceLevelAndFrequencyFormId,"间隔天数",0,detailJson34);
//        createField(fieldsId35,tenementId,warehouseFormId,"仓库",0,detailJson35);
//        createField(fieldsId36,tenementId,EquipmentFilesOrResumesFormId,"规格型号",0,detailJson36);
//        createField(fieldsId37,tenementId,EquipmentRepairFormId,"维修工单",0,detailJson37);
//        createField(fieldsId38,tenementId,deviceInformationFormId,"设备厂商",0,detailJson38);
//        createField(fieldsId39,tenementId,machineRoomFormId,"机房",0,detailJson39);
//        createField(fieldsId40,tenementId,EquipmentRepairFormId,"维修情况说明",1,detailJson40);
//        createField(fieldsId41,tenementId,sparePartsLedgerFormId,"备注",1,detailJson41);
//        createField(fieldsId42,tenementId,maintenancePlanBaseFormId,"保养内容及要求",1,detailJson42);
//        createField(fieldsId43,tenementId,EquipmentRepairFormId,"故障简述",1,detailJson43);
//        createField(fieldsId44,tenementId,inspectionSchemeFormId,"巡检内容",1,detailJson44);
//        createField(fieldsId45,tenementId,equipmentInspectionFormId,"本次巡检总结",1,detailJson45);
//        createField(fieldsId46,tenementId,sparePartsRequisitionFormId,"领用数量",1,detailJson46);
//        createField(fieldsId47,tenementId,EquipmentRepairFormId,"更换使用数量",1,detailJson47);
//        createField(fieldsId48,tenementId,equipmentInspectionFormId,"巡检时间",3,detailJson48);
//        createField(fieldsId49,tenementId,SparePartsWarehouseReceiptFormId,"入库日期",3,detailJson49);
//        createField(fieldsId50,tenementId,EquipmentRepairFormId,"派工时间",3,detailJson50);
//        createField(fieldsId51,tenementId,EquipmentFilesOrResumesFormId,"最近巡检",3,detailJson51);
//        createField(fieldsId52,tenementId,EquipmentRepairFormId,"报修时间",3,detailJson52);
//        createField(fieldsId53,tenementId,deviceInformationFormId,"启用时间",3,detailJson53);
//        createField(fieldsId54,tenementId,EquipmentFilesOrResumesFormId,"购买时间",3,detailJson54);
//        createField(fieldsId55,tenementId,EquipmentRepairFormId,"维修完成时间",3,detailJson55);
//        createField(fieldsId56,tenementId,EquipmentFilesOrResumesFormId,"启用时间",3,detailJson56);
//        createField(fieldsId57,tenementId,deviceInformationFormId,"购买时间",3,detailJson57);
//        createField(fieldsId58,tenementId,EquipmentFilesOrResumesFormId,"最近保养",3,detailJson58);
//        createField(fieldsId59,tenementId,sparePartsRequisitionFormId,"领用日期",3,detailJson59);
//        createField(fieldsId60,tenementId,EquipmentFilesOrResumesFormId,"最近维修",3,detailJson60);
//        createField(fieldsId61,tenementId,EquipmentRepairFormId,"故障等级",4,detailJson61);
//        createField(fieldsId62,tenementId,equipmentInspectionFormId,"巡检结果",4,detailJson62);
//        createField(fieldsId63,tenementId,EquipmentRepairFormId,"维修结果",4,detailJson63);
//        createField(fieldsId64,tenementId,sparePartsRequisitionFormId,"领用目的",4,detailJson64);
//        createField(fieldsId65,tenementId,EquipmentRepairFormId,"故障原因",6,detailJson65);
//        createField(fieldsId66,tenementId,inspectionSchemeFormId,"巡检时间频次",6,detailJson66);
//        createField(fieldsId67,tenementId,EquipmentRepairFormId,"维修班组",6,detailJson67);
//        createField(fieldsId68,tenementId,deviceInformationFormId,"设备类型",6,detailJson68);
//        createField(fieldsId69,tenementId,sparePartsLedgerFormId,"单位",6,detailJson69);
//        createField(fieldsId70,tenementId,EquipmentFilesOrResumesFormId,"安装地点",6,detailJson70);
//        createField(fieldsId71,tenementId,deviceInformationFormId,"所属车间",6,detailJson71);
//        createField(fieldsId72,tenementId,EquipmentFilesOrResumesFormId,"设备状态",6,detailJson72);
//        createField(fieldsId73,tenementId,maintenancePlanBaseFormId,"保养频次",6,detailJson73);
//        createField(fieldsId74,tenementId,maintenancePlanBaseFormId,"保养等级",6,detailJson74);
//        createField(fieldsId75,tenementId,EquipmentRepairFormId,"故障类别",6,detailJson75);
//        createField(fieldsId76,tenementId,SparePartsWarehouseReceiptFormId,"入库仓库",6,detailJson76);
//        createField(fieldsId77,tenementId,EquipmentFilesOrResumesFormId,"所属车间",6,detailJson77);
//        createField(fieldsId78,tenementId,deviceInformationFormId,"设备状态",6,detailJson78);
//        createField(fieldsId79,tenementId,EquipmentFilesOrResumesFormId,"设备类型",6,detailJson79);
//        createField(fieldsId80,tenementId,EquipmentFilesOrResumesFormId,"巡检方案",6,detailJson80);
//        createField(fieldsId81,tenementId,deviceInformationFormId,"安装地点",6,detailJson81);
//        createField(fieldsId82,tenementId,equipmentInspectionFormId,"设备状态",6,detailJson82);
//        createField(fieldsId83,tenementId,equipmentMaintenanceFormId,"",8,detailJson83);
//        createField(fieldsId84,tenementId,EquipmentFilesOrResumesFormId,"巡检记录",14,detailJson84);
//        createField(fieldsId85,tenementId,EquipmentFilesOrResumesFormId,"备件更换记录",14,detailJson85);
//        createField(fieldsId86,tenementId,EquipmentFilesOrResumesFormId,"保养维护记录",14,detailJson86);
//        createField(fieldsId87,tenementId,EquipmentFilesOrResumesFormId,"维修保修记录",14,detailJson87);
//        createField(fieldsId88,tenementId,equipmentInspectionFormId,"维修记录",14,detailJson88);
//        createField(fieldsId89,tenementId,SparePartsWarehouseReceiptFormId,"入库明细",15,detailJson89);
//        createField(fieldsId90,tenementId,equipmentMaintenanceFormId,"维修记录",14,detailJson90);
//        createField(fieldsId91,tenementId,equipmentMaintenanceFormId,"备件更换",14,detailJson91);
//        createField(fieldsId92,tenementId,sparePartsRequisitionFormId,"领用明细",15,detailJson92);
//        createField(fieldsId93,tenementId,equipmentInspectionFormId,"关联设备",15,detailJson93);
//        createField(fieldsId94,tenementId,maintenancePlanBaseFormId,"设备基础信息",15,detailJson94);
//        createField(fieldsId95,tenementId,equipmentInspectionFormId,"关联巡检方案",15,detailJson95);
//        createField(fieldsId96,tenementId,EquipmentRepairFormId,"备件更换",15,detailJson96);
//        createField(fieldsId97,tenementId,EquipmentRepairFormId,"关联设备",15,detailJson97);
//        createField(fieldsId98,tenementId,equipmentInspectionFormId,"巡检人员",20,detailJson98);
//        createField(fieldsId99,tenementId,EquipmentRepairFormId,"报修人",20,detailJson99);
//        createField(fieldsId100,tenementId,sparePartsRequisitionFormId,"领用人",20,detailJson100);
//        createField(fieldsId101,tenementId,maintenancePlanBaseFormId,"保养人",20,detailJson101);
//        createField(fieldsId102,tenementId,EquipmentRepairFormId,"维修责任人",20,detailJson102);
//        createField(fieldsId103,tenementId,SparePartsWarehouseReceiptFormId,"入库人",20,detailJson103);
//
//        return true;
//    }



    //生成typeId_+19位的id
    @Override
    public String getFieldsId(Integer typeId){
        return typeId+"_"+UUID.randomUUID().toString().replaceAll("-", "").substring(0,19);
    }

    @Override
    @Transactional
    public boolean createField(String id,Integer tenementId,Integer formId,String name,Integer typeId,String detailJsom){
        Field field = new Field();
        field.setId(id);
        field.setTenementId(tenementId);
        field.setFormId(formId);
        field.setName(name);
        field.setTypeId(typeId);
        field.setDetailJson(detailJsom);
        field.setCreateTime(LocalDateTime.now());
        field.setUpdateTime(LocalDateTime.now());
        int i = fieldMapper.insert(field);
        return i>0;
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

//    @Override
//    @Transactional
//    public boolean createMenuForm(Integer menuId, Integer formType, Integer tenementId,String formName,String formFields,String properties) {
//        assert formType==0 || formType==1;
//        Form form = new Form();
//        form.setFormName(formName);
//        form.setMenuId(menuId);
//        form.setTenementId(tenementId);
//        form.setFormType(formType);
//        Form.FormFieldsDto formFieldsDto = new Form.FormFieldsDto();
//        formFieldsDto.setName("root");
//        formFieldsDto.setFieldsId(new ArrayList<>());
//        form.setFormFields(JSON.toJSONString(Collections.singletonList(formFieldsDto)));
//        form.setFormFields(formFields);
//        form.setProperties("{\"displayType\":\"column\",\"labelWidth\":120,\"type\":\"object\"}");
//        form.setProperties(properties);
//        form.setCreateTime(LocalDateTime.now());
//        form.setUpdateTime(LocalDateTime.now());
//        int i = formMapper.insert(form);
//        return i>0;
//    }
}
