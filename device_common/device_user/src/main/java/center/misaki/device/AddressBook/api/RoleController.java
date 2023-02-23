package center.misaki.device.AddressBook.api;

import center.misaki.device.AddressBook.AuthScope;
import center.misaki.device.AddressBook.dto.RoleDto;
import center.misaki.device.AddressBook.service.RoleService;
import center.misaki.device.AddressBook.vo.RoleGroupVo;
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
@RequestMapping("/role")
public class RoleController {
    
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }
    
    //获取所有的角色组和组里面的角色
    @GetMapping("/show")
//    @AuthOnCondition(NeedSysAdmin = false)
    public Result<List<RoleGroupVo>> showAllRole(){
        List<RoleGroupVo> allGroupRole = roleService.getAllGroupRole();
        return Result.ok(allGroupRole,"获取成功");
    }
    
    //创建角色组
    @PostMapping("/addGroup")
    @AuthOnCondition
    public Result<?> addGroup(@RequestBody RoleDto.GroupDto groupDto){
        if(roleService.addOneRoleGroup(groupDto.getName())){
            return Result.ok(null,"添加成功");
        }else return Result.error("添加失败");
    }
    
    //创建角色
    @PostMapping("/addRole")
    @AuthOnCondition
    public Result<?> addRole(@RequestBody RoleDto roleDto){
        if(roleService.addOneRole(roleDto)){
            return Result.ok(null,"添加成功");
        }else return Result.error("添加失败");
    }
    
    //修改角色名称
    @PostMapping("/changeRoleName")
    @AuthOnCondition
    public Result<?> changeRoleName(@RequestBody RoleDto roleDto){
        if(roleService.changeRoleName(roleDto)){
            return Result.ok(null,"修改成功");
        }else return Result.error("修改失败");
    }
    
    //删除角色
    @PostMapping("/deleteRole")
    @AuthOnCondition
    public Result<?> deleteGroup(@Valid RoleDto roleDto){
        if(roleService.deleteOneRole(roleDto)){
            return Result.ok(null,"删除成功");
        }else return Result.error("删除失败");
    }
    
    //删除角色组
    @PostMapping("/deleteGroup")
    @AuthOnCondition
    public Result<?> deleteGroup(@NotNull Integer groupId){
        if(roleService.deleteRoleGroup(groupId)){
            return Result.ok(null,"删除成功");
        }else return Result.error("删除失败");
    }
    
    
    //修改角色组名称
    @PostMapping("/changeGroupName")
    @AuthOnCondition
    public Result<?> changeGroupName(@RequestBody RoleDto.GroupDto groupDto){
        if(roleService.changeGroupName(groupDto)){
            return Result.ok(null,"修改成功");
        }else return Result.error("修改失败");
    }
    
    //获取一个角色下的所有成员
    @GetMapping("/showRoleUser")
//    @AuthOnCondition(NeedSysAdmin = false)
    @AuthScope(role = true)
    public Result<List<UserVo.UserRoleVo>> getRoleUser(@Valid @NotNull Integer roleId){
        List<UserVo.UserRoleVo> allUserForRole = roleService.getAllUserForRole(roleId);
        return Result.ok(allUserForRole,"获取成功");
    }
    
    //调整一个角色的分组
    @PostMapping("/changeRoleGroup")
    @AuthOnCondition
    public Result<?> changeRoleGroup(@RequestBody RoleDto roleDto){
        if(roleService.changeRoleGroup(roleDto)){
            return Result.ok(null,"修改成功");
        }else return Result.error("修改失败");
    }
    
    //批量给一个角色保存用户
    @PostMapping("/addUser")
    @AuthOnCondition(NeedSysAdmin = false)
    @AuthScope(role = true,modify = true)
    public Result<?> addUser(@RequestBody List<Integer> userIds,@Valid @NotNull Integer roleId){
        roleService.saveUserIdsForRole(userIds,roleId);
        return Result.ok(null,"保存成功");
    }
    
    //在角色中搜索接口
    @PostMapping("/search")
    @AuthOnCondition(NeedSysAdmin = false)
    @AuthScope(role = true)
    public Result<List<UserVo.UserRoleVo>> search(@Valid @NotNull String userInfo,@Valid @NotNull Integer roleId){
        List<UserVo.UserRoleVo> userRoleVos = roleService.searchUser(userInfo, roleId);
        return Result.ok(userRoleVos,"获取成功");
    }
    
    
}
