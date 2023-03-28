package center.misaki.device.Form.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @author wwx
 */
@Mapper
public interface DepartmentMapper {

    @Select({"select department.id from department where department.tenement_id=#{arg0} and department.pre_id=#{arg1}"})
    Long selectIdByTenementIdAndPreId(Integer tenementId,Integer preId);
}
