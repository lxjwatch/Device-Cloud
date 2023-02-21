package center.misaki.device.AddressBook.service.impl;

import center.misaki.device.AddressBook.dao.UserMapper;
import center.misaki.device.AddressBook.dto.Head;
import center.misaki.device.AddressBook.dto.UserDto;
import center.misaki.device.AddressBook.service.UserService;
import center.misaki.device.AddressBook.vo.UserVo;
import center.misaki.device.Auth.SecurityUtils;
import center.misaki.device.Enum.UserStateEnum;
import center.misaki.device.domain.Pojo.User;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Misaki
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {
    
    private final UserMapper userMapper;
    private final DepartmentServiceImpl departmentServiceImpl;
    private final PasswordEncoder passwordEncoder;
    private final RoleServiceImpl roleServiceImpl;

    public UserServiceImpl(UserMapper userMapper, DepartmentServiceImpl departmentServiceImpl, PasswordEncoder passwordEncoder, RoleServiceImpl roleServiceImpl) {
        this.userMapper = userMapper;
        this.departmentServiceImpl = departmentServiceImpl;
        this.passwordEncoder = passwordEncoder;
        this.roleServiceImpl = roleServiceImpl;
    }


    @Override
    @Transactional
    public Integer addUser(UserDto userDto) {
        if(checkUserNameIsUsed(userDto.getUserName())) return -1;
        User user = new User();
        user.setEmail(userDto.getEmail());
        
        user.setNickName(userDto.getName());
        user.setPhone(userDto.getPhone());
        user.setUsername(userDto.getUserName());
        user.setTenementId(SecurityUtils.getCurrentUser().getTenementId());
        
        //插入到数据库中，默认状态是未到岗
        user.setState(UserStateEnum.NO_ARRIVE.ordinal());
        user.setIsForbidden(true);
        
        int i = userMapper.insert(user);
        if(userDto.getDepartmentIds()!=null){
            userDto.getDepartmentIds().forEach(d->{
                departmentServiceImpl.insertOneUserToDepart(user.getId(),d);
            });
        }
        return user.getId();
    }
    
    public boolean checkUserNameIsUsed(String userName){
        return userMapper.exists(new QueryWrapper<User>().eq("username",userName));
    }
    
    

    @Override
    public boolean changeUserInfo(UserDto.ChangeUserInfoDto changeUserInfoDto) {
        User user = userMapper.selectById(changeUserInfoDto.getUserId());
        List<Integer> originRoleIds = roleServiceImpl.getUserRoleIds(changeUserInfoDto.getUserId());
        if(!changeUserInfoDto.getRoleIds().equals(originRoleIds)){
            roleServiceImpl.changeRoleIdForUser(changeUserInfoDto.getRoleIds(),originRoleIds, changeUserInfoDto.getUserId());
        }
        if(!changeUserInfoDto.getState().equals(user.getState())){
            user.setState(changeUserInfoDto.getState());
        }
        if(!changeUserInfoDto.getName().equals(user.getNickName())){
            user.setNickName(changeUserInfoDto.getName());
        }
        List<Integer> originDepartIds = departmentServiceImpl.getDepartIdsForUser(changeUserInfoDto.getUserId());
        if(!changeUserInfoDto.getDepartmentIds().equals(originDepartIds)){
            departmentServiceImpl.changeDepartIdsForUser(changeUserInfoDto.getDepartmentIds(),originDepartIds, changeUserInfoDto.getUserId());
        }
        int i = userMapper.updateById(user);
        return i>0;
    }

    @Override
    public boolean changeUserInfo(UserDto.InitialUserDto initialUserDto) {
        User user = userMapper.selectById(initialUserDto.getUserId());
        user.setPhone(null);
        user.setEmail(null);
        if(initialUserDto.getEmail()!=null||!initialUserDto.getEmail().equals("")) user.setEmail(initialUserDto.getEmail());
        user.setState(1);
        if(initialUserDto.getPhone()!=null || !initialUserDto.getPhone().equals("")) user.setPhone(initialUserDto.getPhone());
        user.setIsForbidden(false);
        user.setGender(initialUserDto.getGender());
        user.setPwd(passwordEncoder.encode(initialUserDto.getPwd()));
        int i = userMapper.updateById(user);
        return i>0;
    }

    @Override
    public boolean resignationUser(Integer userId) {
        int i = userMapper.update(null, new UpdateWrapper<User>().eq("id", userId).eq("tenement_id", SecurityUtils.getCurrentUser().getTenementId())
                .set("state", 2));
        return i>0;
    }

    @Override
    public boolean deleteUser(Integer userId) {
        int i = userMapper.update(null, new UpdateWrapper<User>().eq("id", userId).eq("tenement_id", SecurityUtils.getCurrentUser().getTenementId())
                .set("is_delete", true));
        return i>0;
    }

    @Override
    public List<UserVo> getAllUser() {
        List<User> users = userMapper.selectList(new UpdateWrapper<User>().eq("tenement_id", SecurityUtils.getCurrentUser().getTenementId()).eq("is_delete", false));
        List<UserVo> userVos = new ArrayList<>(32);
        users.forEach(u->{
            UserVo userVo = new UserVo();
            userVo.setUserId(u.getId());
            userVo.setName(u.getNickName());
            userVo.setPhone(u.getPhone());
            userVo.setEmail(u.getEmail());
            userVo.setState(u.getState());
            userVo.setRole(roleServiceImpl.getUserRoleMap(u.getId()));
            userVos.add(userVo);
        });
        return userVos;
    }

    @Override
    public List<UserVo> searchUser(String userInfo) {
        List<User> users = userMapper.selectList(new QueryWrapper<User>().like("username", userInfo).or().like("nick_name", userInfo).or().like("email", userInfo).or().like("phone", userInfo));
        List<UserVo> userVos = new ArrayList<>(32);
        users.forEach(u->{
            UserVo userVo = new UserVo();
            userVo.setUserId(u.getId());
            userVo.setName(u.getNickName());
            userVo.setPhone(u.getPhone());
            userVo.setEmail(u.getEmail());
            userVo.setRole(roleServiceImpl.getUserRoleMap(u.getId()));
            userVo.setState(u.getState());
            userVos.add(userVo);
        });
        return userVos;
    }

    @Override
    public List<UserVo> searchUser(String userInfo, Integer departmentId) {
        List<UserVo> userVos = searchUser(userInfo);
        Set<Integer> userIds = new HashSet<>(departmentServiceImpl.getUserIdsForDepart(departmentId));
        return userVos.stream().filter(userVo -> userIds.contains(userVo.getUserId())).collect(Collectors.toList());
    }

    
    @Override
    public void setUserForDepartOwn(Integer userId, Integer departmentId) {
        departmentServiceImpl.setOwnForOneDepartment(userId,departmentId);
    }

    @Override
    public UserVo.SingleUserVo getOneUserDetail(Integer userId) {
        User user = userMapper.selectById(userId);
        UserVo.SingleUserVo userVo = new UserVo.SingleUserVo();
        userVo.setUserName(user.getUsername());
        userVo.setName(user.getNickName());
        userVo.setEmail(user.getEmail());
        userVo.setPhone(user.getPhone());
        userVo.setDepartments(departmentServiceImpl.getDepartMapForUser(userId));
        userVo.setRoles(roleServiceImpl.getUserRoleMap(userId));
        return userVo;
    }

    @Override
    public Set<Integer> getUserIdsFromHead(Head head) {
        Set<Integer> ans = new HashSet<>(Arrays.asList(head.getUser()));
        Integer[] department = head.getDepartment();
        for (Integer departmentId : department) {
            ans.addAll(departmentServiceImpl.getUserIdsForDepart(departmentId));
        }
        Integer[] role = head.getRole();
        for (Integer roleId : role) {
            ans.addAll(roleServiceImpl.getUserIdsForRole(roleId));
        }
        return ans;
    }
}
