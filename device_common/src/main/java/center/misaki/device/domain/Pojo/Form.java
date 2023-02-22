package center.misaki.device.domain.Pojo;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 表单
 */
@Getter
@Setter
public class Form  implements Serializable{
    @TableId(type= IdType.AUTO)
    private Integer id;
    @JSONField(serialize = false)//指定该属性不被序列化
    private Integer tenementId;
    
    private Integer formType;

    private Integer menuId;
    
    private String formName;

    /**
     * @see FormFieldsDto 序列化后的结果
     *  List<FormFieldsDto> 序列化结果
     */
    private String formFields;
    
    private String properties;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime updateTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime createTime;

    
    
    /**
     * @author Misaki
     * 此类用于序列化至 form 表中的 form_fields字段,此类代表表中一条目的 fields结构
     */
    @Data
    public static class FormFieldsDto implements Serializable{
        //表单内部子表名称
        private String name;
        //子表内部的字段ID
        private List<String> fieldsId;
    }
}
