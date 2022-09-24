package center.misaki.device.AddressBook.dto;

import lombok.Data;

/**
 * @author Misaki
 */
@Data
public class RoleDto {
    
    private Integer groupId;
    private String roleName;
    private Integer roleId;
    
    @Data
    public static class GroupDto{
        private Integer groupId;
        private String name;
    }
    
}
