package center.misaki.device.Auth.dto;

import lombok.Data;

import java.util.Map;
import java.util.Set;

/**
 * @author Misaki
 * 表格单条栏目权限对象
 */
@Data
public class FormSingleAuthDto {

    private Integer  operation;
    
    private Integer formId;
    
    private Set<Integer> department;
    
    private Set<Integer> role;
    
    private Set<Integer> user;
    
    
    @Data
    public static class FormSingleAuthVo{

        private Integer operation;
        
        private Map<Integer,String> department;
        
        private Map<Integer,String> role;
        
        private Map<Integer,String> user;
        
    }
    
    
}
