package center.misaki.device.AddressBook.vo;

import lombok.Data;

import java.util.Map;

/**
 * @author Misaki
 */
@Data
public class UserVo {
    private Integer userId;
    
    private String name;
    
    private String phone;
    
    private String email;
    
    private Integer state;
    
    private Map<Integer,String> role;

    @Data
    public static class SingleUserVo{
        private String name;
        private String userName;
        private String phone;
        private String email;
        private String tenementName;
        private Integer tenementId;
        private Map<Integer,String> departments;
        private Map<Integer,String> roles;
    }
    
    @Data
    public static class UserRoleVo{
        private Integer userId;
        private String nickName;
        private Map<Integer,String> departments;
    }
    
    @Data
    public static class SimpleUserVo{
        private Integer userId;
        private String userName;
    }

    @Data
    public static class registerUserVo{
        private Integer tenementId;
    }

    @Data
    public static class registerEmployeeVo{
        private Integer tenementId;
        private String msg;
    }
}
