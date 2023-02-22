package center.misaki.device.AddressBook.pojo;

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
public class Department {
    @TableId(type = IdType.AUTO)
    private  Integer id;
    private Integer tenementId;
    private Integer preId;
    private String name;
    private Integer ownId;

    /**
     * 字段填充策略FieldFill
     * DEFAULT:默认不处理
     * INSERT:插入时自动填充
     * UPDATE:更新时自动填充
     * INSERT_UPDATE:插入和更新时自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    
    
}
