package center.misaki.device.Auth.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Set;

/**
 * @author Misaki
 * 传输过来的权限类
 */
@Data
public class AuthDto {
    @NotNull
    private String name;
    @NotNull
    private Boolean editForm;
    @NotNull
    private Address addressBook;
    @NotNull
    private Sco scope;

    private static final long serialVersionUID = -11;
    @Data
    public static class Address{
        private Boolean department;
        private Boolean[] role;
    }
    
    @Data
    public static class Sco{
        private Set<Integer> department;
        private Set<Integer> role;
    }
    
    
    
}
