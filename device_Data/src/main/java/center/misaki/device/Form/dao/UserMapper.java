package center.misaki.device.Form.dao;

import center.misaki.device.domain.Pojo.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @author wwx
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select({"select id from user where tenement_id=#{arg0}"})
    int selectIdByTenementId(int tenementId);

    @Select({"select username from user where tenement_id=#{arg0}"})
    String selectUserNameByTenementId(int tenementId);
}
