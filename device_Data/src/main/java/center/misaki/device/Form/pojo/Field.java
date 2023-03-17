package center.misaki.device.Form.pojo;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author Misaki
 * 字段类，对应着字段表格
 */
@Data
public class Field implements Serializable {
    @TableId
    private String id;
    
    @JSONField(serialize = false)
    private Integer tenementId;
    @JSONField(serialize = false)
    private Integer formId;
    private Integer typeId;
    private String name;
    /**
     * 字段详细的属性文档，由    序列化而来
     */
    private String  detailJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
