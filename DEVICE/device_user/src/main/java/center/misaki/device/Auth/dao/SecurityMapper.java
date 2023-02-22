package center.misaki.device.Auth.dao;

import center.misaki.device.domain.Pojo.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Misaki
 */
@Mapper
public interface SecurityMapper extends BaseMapper<User> {
    
    
    User selectByUserName(String userName);
    User selectByWxOpenId(String wxOpenId);
    
}
