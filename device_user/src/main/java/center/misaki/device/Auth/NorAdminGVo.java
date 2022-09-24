package center.misaki.device.Auth;

import center.misaki.device.AddressBook.vo.UserVo;
import center.misaki.device.Auth.dto.AuthDto;
import lombok.Data;

import java.util.List;

/**
 * @author Misaki
 * 普通管理组的视图对象
 */
@Data
public class NorAdminGVo {
    
    private Integer id;
    
    private String name;
    
    private List<UserVo.SimpleUserVo> admins;
    
    private AuthDto authDto;
}
