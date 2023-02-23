package center.misaki.device.Form.vo;

import center.misaki.device.Form.pojo.Field;
import center.misaki.device.Form.pojo.FormData;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author Misaki
 * 一张表单中的，数据视图
 */
@Data
public class FormDataVo implements Serializable {
    private List<Field> fields;
    private List<FormData> fieldsValue;
}
