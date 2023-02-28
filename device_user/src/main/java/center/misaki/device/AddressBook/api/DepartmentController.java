package center.misaki.device.AddressBook.api;

import center.misaki.device.AddressBook.AuthScope;
import center.misaki.device.AddressBook.dto.DepartmentDto;
import center.misaki.device.AddressBook.service.DepartmentService;
import center.misaki.device.AddressBook.vo.DepartmentVo;
import center.misaki.device.AddressBook.vo.UserVo;
import center.misaki.device.Annotation.AuthOnCondition;
import center.misaki.device.base.Result;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Misaki
 */
@RestController
@RequestMapping("/department")
public class DepartmentController {
    
    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    //新增部门
    @PostMapping("/add")
    @AuthOnCondition
    public Result<?> addDepart(@RequestBody DepartmentDto departmentDto){
        if(departmentService.addOneDepart(departmentDto)){
            return Result.ok(null,"添加成功");
        }else return Result.error("添加错误");
    }
    
    //删除部门
    @PostMapping("/delete")
    @AuthOnCondition
    public Result<?> deleteDepart(@Valid @NotNull  Integer departmentId){
        if(departmentService.deleteDepartment(departmentId)){
            return Result.ok(null,"删除成功");
        }else return Result.error("删除失败");
    }
    
    
    //获取所有的部门
    @GetMapping("/show")
//    @AuthOnCondition(NeedSysAdmin = false)
    public Result<DepartmentVo> showAllDepartment(){
        DepartmentVo allDepartments = departmentService.getAllDepartments();
        return Result.ok(allDepartments,"获取成功");
    }
    
    //获取一个部门下所有的成员
    @GetMapping("/getOneDeUser")
//    @AuthOnCondition(NeedSysAdmin = false)
    @AuthScope(department = true)
    public Result<List<UserVo>> getOneDeUser(@Valid @NotNull  Integer departmentId){
        List<UserVo> userOnDepart = departmentService.getUserOnDepart(departmentId);
        return Result.ok(userOnDepart,"获取成功");
    }
    
    //调整上级部门接口
    @PostMapping("/changePre")
    @AuthOnCondition
    public Result<?> changePreDe(@RequestBody DepartmentDto departmentDto){
        if(departmentService.changePreDepart(departmentDto)){
            return Result.ok(null,"修改成功");
        }else return Result.error("修改失败");
    }
    
    //修改部门名称接口
    @PostMapping("/changeName")
    @AuthOnCondition
    public Result<?> changeName(@RequestBody DepartmentDto departmentDto){
        if(departmentService.changeDepartmentName(departmentDto)){
            return Result.ok(null,"修改成功");
        }else return Result.error("修改失败");
    }
    
}
