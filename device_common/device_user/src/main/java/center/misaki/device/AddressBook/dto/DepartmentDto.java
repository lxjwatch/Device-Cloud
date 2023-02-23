package center.misaki.device.AddressBook.dto;

import lombok.Data;

/**
 * @author Misaki
 */
@Data
public class DepartmentDto {
    private Integer preId;//上一级部门Id
    
    private Integer departmentId;
    
    private String name;
}
