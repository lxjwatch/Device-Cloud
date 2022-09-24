package center.misaki.device.Form.pojo;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Misaki
 * 单条数据日志，修改单条数据的时候会呈现
 */
@Data
public class DataModifyLog {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    @JSONField(serialize = false)
    private Integer tenementId;
    @JSONField(serialize = false)
    private Integer dataId;
    
    private Integer changeNum;
    
    //由 Map<Integer,String[]>序列化而来 数组长度为2,第一个为旧值，第二个为新值,如果数组长度为 1,就代表这是新增数据
    private String changeContent;
    
    private String createPerson;
    
    private LocalDateTime createTime;
    
}
