package center.misaki.device.Form.dao;

import center.misaki.device.domain.Pojo.Menu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @author Misaki
 */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

    @Select({"select max(id) from menu"})
    int selectIdMax();

    @Select({"select id from menu where tenement_id =#{arg0} and name =#{arg1}"})
    int selectIdByTenementIdAndName(int tenementId, String name);
}
