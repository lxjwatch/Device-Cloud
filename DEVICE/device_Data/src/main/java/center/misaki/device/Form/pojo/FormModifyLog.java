package center.misaki.device.Form.pojo;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author Misaki
 * 多条数据修改日志
 */
@Data
public class FormModifyLog implements Serializable {
    @TableId(type= IdType.AUTO)
    private Integer id;
    @JSONField(serialize = false)
    private Integer tenementId;
    private Integer formId;
    @JSONField(serialize = false)
    private String fieldId;
    /**
     * 更新之后的值JSON序列化的结果
     */
    private String newValue;
    private Integer modifyNum;
    private Integer successNum;
    private Integer failNum;
    private String createPerson;
    private LocalDateTime createTime;
}
