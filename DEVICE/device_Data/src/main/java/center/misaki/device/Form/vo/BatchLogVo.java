package center.misaki.device.Form.vo;

import center.misaki.device.Form.pojo.FormModifyLog;
import lombok.Data;

/**
 * @author Misaki
 * 批量修改的日志视图类
 */
@Data
public class BatchLogVo {
    
    private FormModifyLog formModifyLog;
    private String fieldName;
}
