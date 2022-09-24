package center.misaki.device.domain.Dto;

import lombok.Data;

/**
 * @author Misaki
 * 传输过来的权限类
 */
@Data
public class AuthDto {
    
    private Boolean editForm;
    
    private Address addressBook;
    
    private Sco scope;

    private static final long serialVersionUID = -11;
    @Data
    public static class Address{
        private Boolean department;
        private Boolean[] role;
    }
    
    @Data
    public static class Sco{
        private Integer[] department;
        private Integer[] role;
    }
    
}
