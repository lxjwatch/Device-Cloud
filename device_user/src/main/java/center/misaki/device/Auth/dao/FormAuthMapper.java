package center.misaki.device.Auth.dao;

import center.misaki.device.domain.Pojo.DepartmentAuthForm;
import center.misaki.device.domain.Pojo.GroupAuthForm;
import center.misaki.device.domain.Pojo.RoleAuthForm;
import center.misaki.device.domain.Pojo.UserAuthForm;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface FormAuthMapper {
    
    @Select({"select * from userAuthForm where tenement_id=#{arg0} and form_id=#{arg1}"})
    List<UserAuthForm> selectUserAuthForFormId(Integer tenementId,Integer formId);
    
    @Select({"select * from departmentAuthForm where tenement_id=#{arg0} and form_id=#{arg1}"})
    List<DepartmentAuthForm> selectDepartAuthForFormId(Integer tenementId,Integer formId);
    
    @Select({"select * from roleAuthForm where tenement_id=#{arg0} and form_id=#{arg1}"})
    List<RoleAuthForm> selectRoleAuthForFormId(Integer tenementId,Integer formId);
    
    //Object是UserAuthForm
    @MapKey("user_id")
    Map<Long,Object> selectUserAuthMap(Integer tenementId,Integer formId);
    
    //Object是 DepartmentAuthForm
    @MapKey("department_id")
    Map<Long,Object> selectDepartAuthMap(Integer tenementId,Integer formId);
    
    //Object 是 RoleAuthForm
    @MapKey("role_id")
    Map<Long,Object> selectRoleAuthMap(Integer tenementId,Integer formId);
    
    
    //插入 userFormGroup  一条数据
    int insertIntoUserGroup(Integer tenementId,Integer userId,Integer groupId);
    
    //删除 userFormGroup 一条数据
    int deleteOneUserGroup(Integer userId,Integer groupId);
    
    //查询 groupId 的相关数据 即 userIds
    List<Integer> selectUserIdsGroup(Integer groupId);
    
    List<GroupAuthForm> selectUserFormGroupId(Integer userId,Integer formId);
    
}
