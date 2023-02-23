package center.misaki.device.Auth.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Set;

/**
 * @author Misaki
 * 表单应用权限的权限组
 */
@Data
public class GroupFormDto {
    @NotNull
    private String name;
    
    private String description;

    //权限数组
    private Set<Integer> operations;
    @NotNull
    private Integer formId;
    
    private Integer id;
    
    //序列化的结果，和数据联动类似
    private String data;
    
    @Data
    public static class GroupUserDto{
        private Set<Integer> userIds;
        
        private Integer groupId;
    }
    
    
    
}
