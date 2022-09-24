package center.misaki.device.AddressBook.service;


import center.misaki.device.AddressBook.dto.DepartmentDto;
import center.misaki.device.AddressBook.vo.DepartmentVo;
import center.misaki.device.AddressBook.vo.UserVo;

import java.util.List;

public interface DepartmentService {
    
    //添加新部门
    boolean addOneDepart(DepartmentDto departmentDto);
    
    //修改部门名称
    boolean changeDepartmentName(DepartmentDto departmentDto);
    
    //调整上级部门
    boolean changePreDepart(DepartmentDto departmentDto);
    
    //删除部门
    boolean deleteDepartment(Integer Id);
    
    //展示所有的部门树形结构
    DepartmentVo getAllDepartments();
    
    //查询一个部门下的所有成员Vo
    List<UserVo> getUserOnDepart(Integer departmentId);
    
}
