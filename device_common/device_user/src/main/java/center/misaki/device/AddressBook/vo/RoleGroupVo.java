package center.misaki.device.AddressBook.vo;

import lombok.Data;

import java.util.Map;

/**
 * @author Misaki
 */
@Data
public class RoleGroupVo {
    
    private Integer groupId;
    
    private String name;
    
    private Map<Integer,String> roles;
    
}
