package center.misaki.device.Auth.dto;

import lombok.Data;

import java.util.List;

/**
 * @author Misaki
 */
@Data
public class NormalAdminDto {
    
    private Integer groupId;
    
    private List<Integer> userIds;
}
