package center.misaki.device.AddressBook.dao;

import center.misaki.device.domain.Pojo.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    
    @Select({"select user.username from user where user.id =#{arg0}"})
    String selectUsernameById(Integer userId);
    
    @Select({"select user.email from user where user.id =#{arg0}"})
    String selectEmailById(Integer userId);

    @Select({"select * from user where user.username =#{arg0}"})
    List<User> selectAllByUsername(String username);

    @Select({"select * from user where user.tenement_id =#{arg0}"})
    List<User> selectAllByTenementId(Integer tenementId);

    @Select({"select max(user.tenement_id) from user"})
    Integer selectMaxTenementId();

    @Select({"select user.id from user where user.username =#{arg0}"})
    Integer selectIdByUsername(String username);

    @Select({"select user.tenement_id from user where user.username =#{arg0}"})
    Integer selectTenementIdByUsername(String username);
}
