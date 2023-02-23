package center.misaki.device.Form.pojo;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author Misaki
 * 表单数据
 */
@Data
public class FormData implements Serializable {
    @TableId(type= IdType.AUTO)
    private Integer id;
    /**
     * @JSONField(serialize = false)：
     * 该属性不会随该对象序列化返回给前端
     */
    @JSONField(serialize = false)
    private Integer tenementId;
    @JSONField(serialize = false)
    private Integer formId;

    /**
     * 表示单条数据，是由下面这个序列化的结果
     * @see java.util.Map
     */
    private String formData;

    @JSONField(serialize = false)
    private Boolean isFlowData;//数据库默认值为false
    
    private String createPerson;
    
    private String commentContent;
    
    private String updatePerson;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
}
