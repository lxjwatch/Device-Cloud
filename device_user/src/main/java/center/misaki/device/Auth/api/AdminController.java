package center.misaki.device.Auth.api;

import center.misaki.device.Annotation.AuthOnCondition;
import center.misaki.device.Auth.NorAdminGVo;
import center.misaki.device.Auth.dto.AuthDto;
import center.misaki.device.Auth.dto.NormalAdminDto;
import center.misaki.device.Auth.service.UserAdminService;
import center.misaki.device.base.Result;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * @author Misaki
 * 管理员接口
 */
@RestController
@RequestMapping("/admin")
public class AdminController {
    
    private final UserAdminService userAdminService;

    public AdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }
    
    //获取当前企业所有的系统管理员
    @GetMapping("/showSysAdmins")
    public Result<Map<Integer,Object>> showSysAdmins(){
        Map<Integer, Object> allSysAdmin = userAdminService.getAllSysAdmin();
        return Result.ok(allSysAdmin,"获取成功");
    }
    
    //获取当前企业所有的普通管理员组和组中的管理员
    @GetMapping("/showNormalAdmins")
    public Result<List<NorAdminGVo>> showNormalAdmins(){
        List<NorAdminGVo> allNormalGroupAdmin = userAdminService.getAllNormalGroupAdmin();
        return Result.ok(allNormalGroupAdmin,"获取成功");
    }
    

    //创造系统管理员
    @AuthOnCondition(NeedCreater = true)
    @PostMapping("/createSys")
    public Result<?> createSysAdmin(@RequestBody List<Integer> userIds){
        if(userAdminService.addSysAdmins(userIds)){
            return Result.ok(null,"添加成功");
        }else return Result.error("添加失败");
    }
    
    //删除系统管理员
    @AuthOnCondition(NeedCreater = true)
    @PostMapping("/deleteSys")
    public Result<?> deleteSys(Integer userId){
        if(userAdminService.deleteSysAdmin(userId)){
            
            return Result.ok(null,"删除成功");
        }else return Result.error("此用户不是系统管理员，或者已经被删除！");
    }
    
    
    //创造普通管理员组
    @AuthOnCondition
    @PostMapping("/createNormalAdmin")
    public Result<?> createNormal(@RequestBody @Valid AuthDto authDto){
        if(userAdminService.addNorAdminGroup(authDto)){
            return Result.ok(null,"添加成功");
        }else return Result.error("添加失败");
    }
    
    
    //更新普通管理组配置信息
    @AuthOnCondition
    @PostMapping("/updateNormalAdmin")
    public Result<?> updateNormal(@RequestBody @Valid AuthDto authDto,Integer groupId){
        userAdminService.updateNormalAdminConfig(authDto,groupId);
        return Result.ok(null,"更新成功");
    }
    
    //更新普通管理组名字
    @AuthOnCondition
    @PostMapping("/updateNormalAdminName")
    public Result<?> updateNormalName(String name,Integer groupId){
        userAdminService.updateNormalAdminName(groupId,name);
        return Result.ok(null,"更新成功");
    }
    
    
    //设置普通管理员
    @AuthOnCondition
    @PostMapping("/setupNormal")
    public Result<?> setUpNorMal(@RequestBody NormalAdminDto normalAdminDto){
        if(userAdminService.setupAdmins(normalAdminDto.getUserIds(),normalAdminDto.getGroupId())){
            return Result.ok(null,"添加成功");
        }else return Result.error("无法添加这些用户至管理组。");
    }    
    
    //删除普通管理员
    @AuthOnCondition
    @PostMapping("/deleteNormal")
    public Result<?> deleteNormal(Integer userId){
        if(userAdminService.deleteNormalAdmin(userId)){
            
            return Result.ok(null,"移除用户成功");
        }else return Result.error("用户不在管理组中，或者已经被删除！");
    }
    
    
    //删除普通管理员组
    @AuthOnCondition
    @PostMapping("/deleteNormalGroup")
    public Result<?> deleteNormalGroup(Integer groupId){
        if(userAdminService.deleteNormalGroup(groupId)){
            return Result.ok(null,"删除管理员组成功");
        }else return Result.error("管理员组中还有成员，无法删除！");
    }
    
    
    
}
