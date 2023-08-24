package center.misaki.device.AddressBook.service.impl;

import center.misaki.device.AddressBook.AuthScope;
import center.misaki.device.AddressBook.dao.DepartmentMapper;
import center.misaki.device.AddressBook.dao.RoleMapper;
import center.misaki.device.AddressBook.dao.UserMapper;
import center.misaki.device.AddressBook.dto.RoleDto;
import center.misaki.device.AddressBook.service.RoleService;
import center.misaki.device.AddressBook.vo.RoleGroupVo;
import center.misaki.device.AddressBook.vo.UserVo;
import center.misaki.device.Auth.SecurityUtils;
import center.misaki.device.domain.Pojo.Role;
import center.misaki.device.domain.Pojo.User;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Misaki
 */
@Slf4j
@Service
public class RoleServiceImpl implements RoleService {
    
    private final RoleMapper roleMapper;
    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;

    public RoleServiceImpl(RoleMapper roleMapper, DepartmentMapper departmentMapper, UserMapper userMapper) {
        this.roleMapper = roleMapper;
        this.departmentMapper = departmentMapper;
        this.userMapper = userMapper;
    }


    //返回当前用户拥有的角色ID集合
    public List<Integer> getUserRoleIds(Integer userId){
        List<Map<String, Object>> roleIdNameMap = roleMapper.selectRoleIdsByUserId(userId, SecurityUtils.getCurrentUser().getTenementId());
        return roleIdNameMap.stream().map(m->((Long)m.get("roleId")).intValue()).collect(Collectors.toList());
    }
    //返回当前用户拥有的角色ID，和角色名Map集合
    public Map<Integer,String> getUserRoleMap(Integer userId){
        List<Map<String, Object>> roleIdNameMap = roleMapper.selectRoleIdsByUserId
                (userId, SecurityUtils.getCurrentUser().getTenementId());
        Map<Integer, String> ans = new HashMap<>();
        roleIdNameMap.forEach(r->{
            ans.put(((Long) r.get("roleId")).intValue(), (String) r.get("name"));
        });
        return ans;
    }
    
    //修改当前用户拥有的角色ID
    @Async
    @Transactional
    public void changeRoleIdForUser(List<Integer> changeRoleId,List<Integer> originRoleId,Integer userId){
        Set<Integer> originIds = new HashSet<>(originRoleId);
        changeRoleId.forEach(c->{
            if(originIds.contains(c)) originIds.remove(c);
            else{
                insertOneUserToRole(userId,c);
            }
        });
        originIds.forEach(o->{
            deleteOneUserFromRole(userId,o);
        });
    }
    
    //添加一个角色给用户
    @Async
    @Transactional
    public void insertOneUserToRole(Integer userId,Integer roleId){
        int i = roleMapper.insertIntoUserRole(userId, roleId, SecurityUtils.getCurrentUser().getTenementId());
        log.info("添加用户id为 {} 的用户至 角色id为 {} 的角色 ：",userId,roleId+i>0?"成功":"失败");
    }
    
    //删除一个角色给用户
    @Async
    @Transactional
    public void deleteOneUserFromRole(Integer userId,Integer roleId){
        int i = roleMapper.deleteOneUserRole(userId, roleId, SecurityUtils.getCurrentUser().getTenementId());
        log.info("删除用户id为 {} 的用户从 角色id为 {} 的角色 ：",userId,roleId+i>0?"成功":"失败");
    }
    
    

    @Override
    @Transactional
    public boolean addOneRoleGroup(String groupName) {
        int i = roleMapper.insertRoleGroup(SecurityUtils.getCurrentUser().getTenementId(), groupName);
        return i>0;
    }

    @Override
    @Transactional
    public boolean addOneRole(RoleDto roleDto) {
        Role role = new Role();
        role.setName(roleDto.getRoleName());
        role.setGroupId(roleDto.getGroupId());
        role.setTenementId(SecurityUtils.getCurrentUser().getTenementId());
        int i = roleMapper.insert(role);
        return i>0;
    }

    @Override
    @Transactional
    public boolean deleteOneRole(RoleDto roleDto) {
        //查询这个角色是否有用户拥有
        List<User> users = roleMapper.selectUserForRole(roleDto.getRoleId(), SecurityUtils.getCurrentUser().getTenementId());
        if(users==null||users.size()>0){
            return false;
        }
        int i = roleMapper.deleteById(roleDto.getRoleId());
        return i>0;
    }

    @Override
    public boolean changeRoleName(RoleDto roleDto) {
        int i = roleMapper.update(null, new UpdateWrapper<Role>().eq("id", roleDto.getRoleId()).set("name", roleDto.getRoleName()));
        return i>0;
    }

    @Override
    public boolean changeGroupName(RoleDto.GroupDto groupDto) {
        int i = roleMapper.changeRoleGroupName(groupDto.getGroupId(), SecurityUtils.getCurrentUser().getTenementId(), groupDto.getName());
        return i>0;
    }

    @Override
    @Transactional
    public List<UserVo.UserRoleVo> getAllUserForRole(Integer roleId) {
        List<User> users = roleMapper.selectUserForRole(roleId, SecurityUtils.getCurrentUser().getTenementId());
        List<UserVo.UserRoleVo> userRoleVos = new ArrayList<>();
        users.forEach(u->{
            UserVo.UserRoleVo userRoleVo = new UserVo.UserRoleVo();
            userRoleVo.setUserId(u.getId());
            userRoleVo.setNickName(u.getNickName());
            userRoleVo.setDepartments(getDepartMapForUser(u.getId()));
            userRoleVos.add(userRoleVo);
        });
        return userRoleVos;
    }
    // 辅助函数，获得某个用户所在部门 ID，Name 的 Map
    private Map<Integer,String> getDepartMapForUser(Integer userId){
        List<Map<String, Object>> departIdNames = departmentMapper.selectUserDepartIds(userId, SecurityUtils.getCurrentUser().getTenementId());
        Map<Integer, String> ans = new HashMap<>();
        departIdNames.forEach(d->{
            ans.put((((Long) d.get("departmentId")).intValue()),(String) d.get("name"));
        });
        return ans;
    }
    
    
    
    @Override
//    @AuthScope(role = true,useId = false)
    public List<RoleGroupVo> getAllGroupRole() {
        List<Role> roleList = roleMapper.selectList(new QueryWrapper<Role>().eq("tenement_id", SecurityUtils.getCurrentUser().getTenementId()));
        List<Integer> groupIds = roleMapper.selectAllGroupId(SecurityUtils.getCurrentUser().getTenementId());
        Map<Integer, List<Role>> groupUser = new HashMap<>();
        groupIds.forEach(d->{
            groupUser.put(d,new ArrayList<>());
        });
        roleList.forEach(r->{
            groupUser.get(r.getGroupId()).add(r);
        });
        List<RoleGroupVo> roleGroupVos = new ArrayList<>();
        groupUser.forEach((k,v)->{
            RoleGroupVo roleGroupVo = new RoleGroupVo();
            roleGroupVo.setName(roleMapper.selectNameGroup(k,SecurityUtils.getCurrentUser().getTenementId()));
            roleGroupVo.setGroupId(k);
            Map<Integer, String> map = new HashMap<>();
            v.forEach(v1->{map.put(v1.getId(),v1.getName());});
            roleGroupVo.setRoles(map);
            roleGroupVos.add(roleGroupVo);
        });
        return roleGroupVos;
    }

    @Override
    @Transactional
    public boolean changeRoleGroup(RoleDto roleDto) {
        int i = roleMapper.update(null, new UpdateWrapper<Role>().eq("id", roleDto.getRoleId()).set("group_id", roleDto.getGroupId()));
        return i>0;
    }

    @Override
    @Async
    @Transactional
    public void saveUserIdsForRole(List<Integer> userIds, Integer roleId) {
//        if (userIds==null||userIds.isEmpty()) return;
        Set<Integer> originUserIds = roleMapper.selectUserForRole(roleId, SecurityUtils.getCurrentUser().getTenementId()).stream().map(User::getId).collect(Collectors.toSet());
        userIds.forEach(u->{
            if(originUserIds.contains(u)) originUserIds.remove(u);
            else insertOneUserToRole(u,roleId);
        });
        originUserIds.forEach(o->{
            deleteOneUserFromRole(o,roleId);
        });
    }

    @Override
    @Transactional
    public List<UserVo.UserRoleVo> searchUser(String userInfo, Integer roleId) {
        List<User> allUsers = userMapper.selectList(new QueryWrapper<User>().like("username", userInfo).or().like("nick_name", userInfo).or().like("email", userInfo).or().like("phone", userInfo));
        Set<Integer> roleUserIds = roleMapper.selectUserForRole(roleId, SecurityUtils.getCurrentUser().getTenementId()).stream().map(User::getId).collect(Collectors.toSet());
        List<UserVo.UserRoleVo> ans = new ArrayList<>();
        allUsers.forEach(a->{
            if(roleUserIds.contains(a.getId())){
                UserVo.UserRoleVo userRoleVo = new UserVo.UserRoleVo();
                userRoleVo.setUserId(a.getId());
                userRoleVo.setNickName(a.getNickName());
                userRoleVo.setDepartments(getDepartMapForUser(a.getId()));
                ans.add(userRoleVo);
            }
        });
        return ans;
    }

    @Override
    @Transactional
    public boolean deleteRoleGroup(Integer groupId) {
        Long count = roleMapper.selectCount(new QueryWrapper<Role>().eq("group_id", groupId));
        if(count>0) return false;
        int i = roleMapper.deleteRoleGroup(groupId);
        return i>0;
    }
    
    //获取当前角色下所有的用户ID
    @Transactional
    public List<Integer> getUserIdsForRole(Integer roleId){
        return roleMapper.selectUserIdsForRole(roleId);
    }


}
