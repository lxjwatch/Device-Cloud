package center.misaki.device.Form.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @author wwx
 */
@Mapper
public interface DepartmentMapper {

    @Select({"select id from department where tenement_id=#{arg0} and pre_id=#{arg1}"})
    Long selectIdByTenementIdAndPreId(Integer tenementId,Integer preId);
}
