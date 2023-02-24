package center.misaki.device.Auth.dao;

import center.misaki.device.Auth.pojo.NormalAdmin;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NormalAdminMapper extends BaseMapper<NormalAdmin> {
    
    @Update("update normalAdmin set config = #{arg0} where id = #{arg1}")
    int updateConfig(String config,Integer id);
    
    
    
}
