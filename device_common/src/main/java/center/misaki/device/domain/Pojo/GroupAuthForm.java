package center.misaki.device.domain.Pojo;

import com.alibaba.fastjson.annotation.JSONField;
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
public class GroupAuthForm {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @JSONField(serialize = false)
    private Integer tenementId;
    
    private String name;
    
    private Integer formId;
    
    private String groupDescription;
    
    private Boolean submit;
    
    private Boolean submitSelf;
    
    private String dataLink;
    
    private Boolean manage;
    
    private Boolean watch;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
