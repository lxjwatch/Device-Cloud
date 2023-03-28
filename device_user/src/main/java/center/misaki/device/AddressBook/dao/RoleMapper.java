package center.misaki.device.AddressBook.dao;

import center.misaki.device.domain.Pojo.Role;
import center.misaki.device.domain.Pojo.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    //查询当前用户ID，所对应的角色和角色名集合
    @MapKey("roleId")
    List<Map<String,Object>> selectRoleIdsByUserId(int userId, int tenementId);
    
    //添加一个角色给用户
    int insertIntoUserRole(int userId, int roleId,int tenementId);
    
    //删除一个角色给用户
    int deleteOneUserRole(int userId,int roleId,int tenementId);
    
    int insertRoleGroup(int tenementId,String name);
    
    //查询拥有这个角色ID，的所有用户
    List<User> selectUserForRole(int roleId,int tenementId);
    String selectNameGroup(int groupId,int tenementId);
    
    //更改组名
    int changeRoleGroupName(int groupId,int tenementId,String name);
    
    List<Integer> selectAllGroupId(int tenementId);
    
    //查询这个角色名字
    @Select({"select role.name from role where role.id=#{arg0}"})
    String selectNameById(Integer roleId);
    
    //删除这个角色组
    int deleteRoleGroup(int groupId);
    
    //查询拥有角色ID，所有的用户ID
    List<Integer> selectUserIdsForRole(int roleId);
    
}
