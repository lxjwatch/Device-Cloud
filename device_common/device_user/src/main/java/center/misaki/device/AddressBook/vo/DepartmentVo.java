package center.misaki.device.AddressBook.vo;

import center.misaki.device.AddressBook.pojo.Department;
import lombok.Data;

import java.util.List;

/**
 * @author Misaki
 * 部门树形VO
 */
@Data
public class DepartmentVo {
    
    private Integer id;
    private Integer preId;
    private String name;
    
    private List<DepartmentVo> nodes;

    public DepartmentVo(Department department) {
        this.id = department.getId();
        this.preId = department.getPreId();
        this.name =department.getName();
    }

    public DepartmentVo() {
    }
}
