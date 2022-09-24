package center.misaki.device.AddressBook.service;

import center.misaki.device.AddressBook.dto.RoleDto;
import center.misaki.device.AddressBook.vo.RoleGroupVo;
import center.misaki.device.AddressBook.vo.UserVo;

import java.util.List;

/**
 * 角色业务服务类
 */
public interface RoleService {
    
    //新增一个角色组
    boolean addOneRoleGroup(String groupName);
    
    //新增一个角色
    boolean addOneRole(RoleDto roleDto);
    
    //删除一个角色
    boolean deleteOneRole(RoleDto roleDto);
    
    boolean  changeRoleName(RoleDto roleDto);
    
    boolean changeGroupName(RoleDto.GroupDto groupDto);
    
    //查看这个角色下的所有人
    List<UserVo.UserRoleVo> getAllUserForRole(Integer roleId);
    
    //查询所有角色组和角色
    List<RoleGroupVo> getAllGroupRole();
    
    //调整一个角色的分组
    boolean changeRoleGroup(RoleDto roleDto);
    
    //批量给一个角色保存用户
    void saveUserIdsForRole(List<Integer> userIds,Integer roleId);
    
    //在角色用户内搜索用户
    List<UserVo.UserRoleVo> searchUser(String userInfo,Integer roleId);
    
    //删除角色组
    boolean deleteRoleGroup(Integer groupId);
    
}
