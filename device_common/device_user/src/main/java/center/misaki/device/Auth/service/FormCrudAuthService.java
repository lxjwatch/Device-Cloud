package center.misaki.device.Auth.service;

import center.misaki.device.AddressBook.dto.Head;
import center.misaki.device.Auth.dto.FormSingleAuthDto;
import center.misaki.device.Auth.dto.GroupFormDto;
import center.misaki.device.domain.Pojo.GroupAuthForm;

import java.util.List;
import java.util.Set;

/**
 * 表单应用操作权限的设置
 */
public interface FormCrudAuthService {
    
    //获取当前用户对某张表的应用权限
    Set<Integer> getFormAuth(Integer formId);
    
    //更新某一张表的  直接提交数据 权限
    void updateSubmitAuth(FormSingleAuthDto formSingleAuthDto);
    
    //更新某一张表的  提交并管理本人 数据权限
    void updateSubmitAndSelfAuth(FormSingleAuthDto formSingleAuthDto);
    
    //更新某一张表的  管理全部数据  权限
    void updateManageAuth(FormSingleAuthDto formSingleAuthDto);
    
    //更新某一张表的  查看全部数据 权限
    void updateWatchAuth(FormSingleAuthDto formSingleAuthDto);
    
    //查看一张 表的权限配置
    List<FormSingleAuthDto.FormSingleAuthVo> getAuthForOneForm(Integer formId);
    
    //创建与这张表相关的权限组
    boolean createAuthGroup(GroupFormDto groupFormDto);

    //更新这个权限组对应的权限
    void updateGroupAuth(GroupFormDto groupFormDto);
    
    //将用户纳入与这张表相关的权限组
    void updateUserGroup(Head head,Integer groupId);
    
    //查看与这张表相关的权限组
    List<GroupAuthForm> getFormAuthGroup(Integer formId);
    
}
