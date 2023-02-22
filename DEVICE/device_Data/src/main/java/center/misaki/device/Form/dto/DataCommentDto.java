package center.misaki.device.Form.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Misaki
 */
@Data
public class DataCommentDto implements Serializable {
    private static final long serialVersionUID = -1L;
    
    private String userName;
    
    private String content;
    
}
