package center.misaki.device.Form.dao;

import center.misaki.device.Form.pojo.Field;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * @author Misaki
 * 字段Mapper操作接口
 */
@Mapper
public interface FieldMapper extends BaseMapper<Field> {
    Optional<String> selectOneFieldName(String fieldId);
}
