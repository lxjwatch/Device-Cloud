package center.misaki.device.AddressBook.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Misaki
 */
@Data
public class UserDto {
    
    @NotNull
    private String name;
    @NotNull
    private String userName;
    
    private String phone;
    
    private String email;
    
    private List<Integer> departmentIds;
    
    @Data
    public static class InitialUserDto{
        @NotNull
        private Integer userId;

        private String phone;

        private String email;
        private String gender;
        @NotNull
        private String pwd;
    }
    
    @Data
    public static class ChangeUserInfoDto{
        @NotNull
        private Integer userId;
        private String name;
        private List<Integer> departmentIds;
        private List<Integer> roleIds;
        private Integer state;
    }
    
}
