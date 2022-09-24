package center.misaki.device.Auth.dao;

import center.misaki.device.domain.Pojo.UserAuthForm;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserAuthMapper extends BaseMapper<UserAuthForm> {
}
