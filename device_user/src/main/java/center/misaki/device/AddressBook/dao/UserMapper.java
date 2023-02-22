package center.misaki.device.AddressBook.dao;

import center.misaki.device.domain.Pojo.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    
    @Select({"select username from user where id =#{arg0}"})
    String selectUsernameById(Integer userId);
    
    @Select({"select email from user where id =#{arg0}"})
    String selectEmailById(Integer userId);

    @Select({"select max(tenement_id) from user"})
    int selectMaxTenementId();

    @Select({"select id from user where username = #{arg0} and tenement_id =#{arg1}"})
    int selectIdByUsername(String username ,int tenementId);
}
