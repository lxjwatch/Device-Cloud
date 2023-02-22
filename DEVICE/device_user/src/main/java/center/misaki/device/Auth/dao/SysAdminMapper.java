package center.misaki.device.Auth.dao;

import center.misaki.device.Auth.pojo.SysAdmin;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

@Mapper
public interface SysAdminMapper extends BaseMapper<SysAdmin> {

        /**
         * 获取系统管理员的 Map结构
         */
        @MapKey("id")
        Map<Integer, Object> selectMapSysAdmin(Integer tenementId);
}