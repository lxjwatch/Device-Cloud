package center.misaki.device.Auth.pojo;

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
public class NormalAdmin {
    
    @TableId(type = IdType.AUTO)
    private  Integer id;
    
    private String name;
    
    private Integer tenementId;

    /**
     * @see center.misaki.device.Auth.dto.AuthDto 的序列化结果
     */
    private String config;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    
}
