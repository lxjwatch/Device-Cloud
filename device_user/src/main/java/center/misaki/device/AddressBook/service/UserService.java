package center.misaki.device.AddressBook.service;

import center.misaki.device.AddressBook.dto.Head;
import center.misaki.device.AddressBook.dto.UserDto;
import center.misaki.device.AddressBook.vo.UserVo;

import java.util.List;
import java.util.Set;

/**
 * 用户操作接口
 */
public interface UserService {

    //注册用户
    UserVo.registerUserVo registerUser(UserDto.RegisterUserDto registerUserDto);

    //注册员工
    UserVo.registerEmployeeVo registerEmployee(UserDto.RegisterEmployeeDto registerEmployeeDto);
    
    //新增用户
    Integer addUser(UserDto userDto);
    
    //管理修改用户信息
    boolean changeUserInfo(UserDto.ChangeUserInfoDto changeUserInfoDto);
    
    //用户自己初始化用户信息
    boolean changeUserInfo(UserDto.InitialUserDto initialUserDto);
    
    //将用户转为离职
    boolean resignationUser(Integer userId);
    
    //删除离职用户
    boolean deleteUser(Integer userId);
    
    //获取当前企业全部成员
    List<UserVo> getAllUser();
    
    //搜索用户
    List<UserVo> searchUser(String userInfo);
    
    //在某一部门下搜索用户
    List<UserVo> searchUser(String userInfo,Integer departmentId);
    
    //设置该用户为部门主管
    void setUserForDepartOwn(Integer userId,Integer departmentId);
    
    //获取单人的详细信息
    UserVo.SingleUserVo getOneUserDetail(Integer userId);
    
    
    Set<Integer> getUserIdsFromHead(Head head);

    //校验数据
    boolean judgePassword(UserDto.UpdateUserDto updateUserDto);

    //修改用户信息
    boolean updateUser(UserDto.UpdateUserDto updateUserDto);


}
