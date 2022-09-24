package center.misaki.device.Auth.api;

import center.misaki.device.AddressBook.dto.Head;
import center.misaki.device.Annotation.AuthOnCondition;
import center.misaki.device.Auth.dto.FormSingleAuthDto;
import center.misaki.device.Auth.dto.GroupFormDto;
import center.misaki.device.Auth.service.FormCrudAuthService;
import center.misaki.device.base.Result;
import center.misaki.device.domain.Pojo.GroupAuthForm;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * @author Misaki
 * 表单权限接口
 */
@RestController
@RequestMapping("/authF")
public class FormAuthController {
    private final FormCrudAuthService formCrudAuthService;

    public FormAuthController(FormCrudAuthService formCrudAuthService) {
        this.formCrudAuthService = formCrudAuthService;
    }
    
    //获取当前用户对某张表的应用权限
    @GetMapping("/show")
    public Result<Set<Integer>> showAuthForm(Integer formId){
        Set<Integer> formAuth = formCrudAuthService.getFormAuth(formId);
        return Result.ok(formAuth,"获取成功");
    }
    
    //查看当前表所对应的权限组
    @GetMapping("/showGroup")
    @AuthOnCondition(NeedSysAdmin=false)
    public Result<List<GroupAuthForm>> showGroupAuthForm(Integer formId){
        List<GroupAuthForm> formAuthGroup = formCrudAuthService.getFormAuthGroup(formId);
        return Result.ok(formAuthGroup,"获取成功");
    }
    
    //创建与这张表相关的权限组
    @PostMapping("/createGroup")
    @AuthOnCondition(NeedSysAdmin = false)
    public Result<?> createGroup(@RequestBody @Valid GroupFormDto groupFormDto){
        if(formCrudAuthService.createAuthGroup(groupFormDto)){
            return Result.ok(null,"创建成功");
        }else return Result.error("未知错误");
    }
    
    //更新这个权限组对应的权限
    @PostMapping("/updateGroup")
    @AuthOnCondition(NeedSysAdmin = false)
    public Result<?> updateGroup(@RequestBody @Valid GroupFormDto groupFormDto){
        formCrudAuthService.updateGroupAuth(groupFormDto);
        return Result.ok(null,"已尝试更改");
    }
    
    //更新权限组的用户接口
    @PostMapping("/updateGroupUser")
    @AuthOnCondition(NeedSysAdmin = false)
    public Result<?> updateGroupUser(@RequestBody @Valid Head head,Integer groupId){
        formCrudAuthService.updateUserGroup(head,groupId);
        return Result.ok(null,"已尝试更改");
    }
    
    //查看一张表的固有权限信息
    @GetMapping("/showDetails")
    @AuthOnCondition(NeedSysAdmin = false)
    public Result<List<FormSingleAuthDto.FormSingleAuthVo>> showConfig(Integer formId){
        List<FormSingleAuthDto.FormSingleAuthVo> authForOneForm = formCrudAuthService.getAuthForOneForm(formId);
        return Result.ok(authForOneForm,"获取成功");
    }
    
    //更新某一张表的  直接提交数据 权限
    @PostMapping("/updateSubmit")
    @AuthOnCondition(NeedSysAdmin = false)
    public Result<?>  updateSubmit(@RequestBody @Valid FormSingleAuthDto formSingleAuthDto){
        formCrudAuthService.updateSubmitAuth(formSingleAuthDto);
        return Result.ok(null,"已尝试更改");
    }
    
    //更新某一张表的  提交并管理本人 数据权限
    @PostMapping("/updateSubmitSelf")
    @AuthOnCondition(NeedSysAdmin = false)
    public Result<?> updateSubmitSelf(@RequestBody @Valid FormSingleAuthDto formSingleAuthDto){
        formCrudAuthService.updateSubmitAndSelfAuth(formSingleAuthDto);
        return Result.ok(null,"已尝试更改");
    }
    
    //更新某一张表的  管理全部数据  权限
    @PostMapping("/updateManage")
    @AuthOnCondition(NeedSysAdmin = false)
    public Result<?> updateManage(@RequestBody @Valid FormSingleAuthDto formSingleAuthDto){
        formCrudAuthService.updateManageAuth(formSingleAuthDto);
        return Result.ok(null,"已尝试更改");
    }
    
    //更新某一张表的  查看全部数据 权限
    @PostMapping("/updateWatch")
    @AuthOnCondition(NeedSysAdmin = false)
    public Result<?> updateWatch(@RequestBody @Valid FormSingleAuthDto formSingleAuthDto){
        formCrudAuthService.updateWatchAuth(formSingleAuthDto);
        return Result.ok(null,"已尝试更改");
    }
    
    
    
    
}
