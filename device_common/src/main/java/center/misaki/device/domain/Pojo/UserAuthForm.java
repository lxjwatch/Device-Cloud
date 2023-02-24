package center.misaki.device.domain.Pojo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Misaki
 */
@Data
public class UserAuthForm {
    
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private Integer tenementId;
    
    private Integer userId;
    
    private Integer formId;
    
    private Boolean submit;
    
    private Boolean submitSelf;
    
    private Boolean manage;
    
    private Boolean watch;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
}
