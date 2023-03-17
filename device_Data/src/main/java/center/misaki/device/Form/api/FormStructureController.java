package center.misaki.device.Form.api;

import center.misaki.device.Annotation.AuthOnCondition;
import center.misaki.device.Form.vo.MenuFormVo;
import center.misaki.device.base.Result;
import center.misaki.device.Form.dto.FormStrucDto;
import center.misaki.device.Form.service.StructureService;
import center.misaki.device.Form.vo.FormStrucVo;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Misaki
 * 表单结构接口
 */
@RestController
@RequestMapping("/form")
public class FormStructureController {
    
    private final StructureService structureService;

    public FormStructureController(StructureService structureService) {
        this.structureService = structureService;
    }

    //获取一张表单的结构以及字段配置文档
    @GetMapping("/get")
    public Result<FormStrucVo> get(@Valid @NotNull Integer formId,String userInfo){
        FormStrucVo strucVo = structureService.getFormStruc(formId, userInfo);
        return Result.ok(strucVo,"获取成功");
    }
    
    //改变表单结构,以及字段配置文档,以及表单属性
    @AuthOnCondition(NeedSysAdmin = false)
    @PostMapping("/change")
    public Result<?> change(@Valid @RequestBody FormStrucDto formStrucDto, String userInfo){
        if(structureService.changeFormStruc(formStrucDto,userInfo)){
            return Result.ok(null,"成功更新表单");
        }else return Result.error("修改表单失败");
    }
    
    //获取所有的表单的简单结构
    @GetMapping("/getFormSimpleStruc")
    public Result<List<FormStrucVo.FormSimpleVo>> getFormSimpleStruc(String userInfo){
        List<FormStrucVo.FormSimpleVo> formSimpleVos = structureService.getFormSimpleStruc(userInfo);
        return Result.ok(formSimpleVos,"获取成功");
    }    
    
    //获取所有菜单下的表单结构信息
    @GetMapping("/menu")
    public Result<List<MenuFormVo>> getMenuForms(String userInfo){
        List<MenuFormVo> menuForms = structureService.getSimpleFormsInMenu(userInfo);
        return Result.ok(menuForms,"获取成功");
    }
    
    //创建普通表单
    @AuthOnCondition(NeedSysAdmin = false)
    @PostMapping("/create/normalForm")
    public Result<FormStrucDto> createNormalForm(Integer menuId,String userInfo){
        FormStrucDto normalForm = structureService.createNormalForm(menuId, 0, userInfo);
        return Result.ok(normalForm,"成功创建表单");
    }
    
    //创建流程表单
    @AuthOnCondition(NeedSysAdmin = false)
    @PostMapping("/create/flowForm")
    public Result<FormStrucDto> createFlowForm(Integer menuId,String userInfo){
        FormStrucDto flowForm = structureService.createNormalForm(menuId, 1, userInfo);
        return Result.ok(flowForm,"成功创建表单");
    }
    
    //修改菜单名字
    @AuthOnCondition(NeedSysAdmin = false)
    @PostMapping("/change/menuName")
    public Result<?> changeMenuName(Integer menuId,String menuName,String userInfo){
        if(structureService.changeMenuName(menuId,menuName,userInfo)){
            return Result.ok(null,"成功修改菜单名字");
        }else return Result.error("修改菜单名字失败");
    }
    
    //修改表单名字
    @AuthOnCondition(NeedSysAdmin = false)
    @PostMapping("/change/formName")
    public Result<?> changeFormName(Integer formId,String formName,String userInfo){
        if(structureService.changeFormName(formId,formName,userInfo)){
            return Result.ok(null,"成功修改表单名字");
        }else return Result.error("修改表单名字失败");
    }
    
    
    
    //切换一个表单的类型
    @AuthOnCondition(NeedSysAdmin = false)
    @PostMapping("/change/formType")
    public Result<?> changeFormType(Integer formId,Integer formType,String userInfo){
        if((formType==0||formType==1)&&structureService.changeFormType(formId,formType,userInfo)){
            return Result.ok(null,"成功切换表单类型");
        }else return Result.error("表单类型不符合要求");
    }
    
    
    //删除一个表单
    @AuthOnCondition
    @PostMapping("/delete")
    public Result<?> delete(Integer formId,String userInfo){
        if(structureService.deleteForm(formId,userInfo)){
            return Result.ok(null,"成功删除表单");
        }else return Result.error("删除表单失败");
    }

    //创建公司菜单和空表单模板
    @PostMapping("/initMenu")
    public Result<?> initMenu(Integer tenementId){
        if (structureService.createMenu(tenementId)){
           return Result.ok(null,"成功初始化菜单");
        }else return Result.error("初始化菜单失败");
    }

    //公司表单初始化字段模板
    @PostMapping("/initTemplate")
    public Result<?> initTemplate(Integer tenementId){
        if (structureService.createFormTemplate(tenementId)){
            return Result.ok(null,"表单模板创建成功");
        }else return Result.error("表单模板创建失败");
    }
}
