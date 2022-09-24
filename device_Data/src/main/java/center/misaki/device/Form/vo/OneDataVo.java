package center.misaki.device.Form.vo;

import center.misaki.device.domain.Pojo.Form;
import center.misaki.device.Form.pojo.DataModifyLog;
import center.misaki.device.Form.pojo.Field;
import center.misaki.device.Form.pojo.FormData;
import lombok.Data;

import java.util.List;

/**
 * @author Misaki
 * 表格中一条数据展开来的详细视图
 */
@Data
public class OneDataVo {
    private List<Form.FormFieldsDto> form;
    private List<Field> fields;
    private FormData data;
    private List<DataModifyLog> logs;

    @Data
    public static class OneFieldValue{
        private String fieldId;
        private String fieldName;
        private String fieldValue;
    }
    
    @Data
    public static class OneFormLinkValue{
        List<FormData> values;
        private String fieldId;

        private List<String> watchFieldIds;
    }
    
}
