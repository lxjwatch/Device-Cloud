package center.misaki.device.Auth.dao;

import center.misaki.device.domain.Pojo.GroupAuthForm;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupAuthMapper extends BaseMapper<GroupAuthForm> {
}
