package center.misaki.device.AddressBook.dao;

import center.misaki.device.AddressBook.pojo.Department;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
    
    Boolean existUserInDepart(Integer departmentId,Integer userId);
    
    //查询这个部门ID中UserId集合
    List<Integer> selectDepartUserIds(Integer departmentId,Integer tenementId);
    
    //查询这个User所在的部门ID,和部门名称集合
    @MapKey("departmentId")
    List<Map<String,Object>> selectUserDepartIds(Integer userId, Integer tenementId);
    
    //添加一个用户到某个部门中
    int insertIntoUserDepartment(Integer departmentId,Integer userId,Integer tenementId);
    
    //删除某一个用户从部门中
    int deleteOneUserDepartment(Integer userId,Integer departmentId,Integer tenementId);
    
    //查询以这个部门为上级部门的ID,name集合,即查询这个部门下级部门ID，name
    @MapKey("departmentId")
    List<Map<String,Object>> selectSubDepartIds(Integer departmentId,Integer tenementId);
    
    //查询这个部门名字
    @Select({"select name from department where id=#{arg0}"})
    String selectNameById(Integer departmentId);
    
    
}
