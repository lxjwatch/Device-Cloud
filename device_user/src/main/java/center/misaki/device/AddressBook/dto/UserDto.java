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


    @Data
    public static class RegisterUserDto{
        @NotNull
        private String username;
        @NotNull
        private String password;
        @NotNull
        private String nickname;
        private String email;
        private String phone;
        @NotNull
        private String tenementName;
    }

    @Data
    public static class RegisterEmployeeDto{
        @NotNull
        private String username;
        @NotNull
        private String password;
        @NotNull
        private String nickname;
        private String email;
        private String phone;
        @NotNull
        private Integer tenementId;
    }

    @Data
    public static class UpdateUserDto{
        private String type;
        private String information;
        private String oldPassword;

    }
    
}
