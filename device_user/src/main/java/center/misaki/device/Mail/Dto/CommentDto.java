package center.misaki.device.Mail.Dto;

import lombok.Data;

/**
 * @author Misaki
 */
@Data
public class CommentDto {
    
    private Integer[] userIds;
    
    private String content;
    
    private Integer dataId;
    
    private String formName;
    
    private String url;
    
}
