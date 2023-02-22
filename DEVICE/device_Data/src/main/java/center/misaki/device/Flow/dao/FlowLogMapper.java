package center.misaki.device.Flow.dao;

import center.misaki.device.Flow.FlowLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FlowLogMapper extends BaseMapper<FlowLog> {
    
    @Select({"select flow_id from  flowLog where id = #{id}"})
    Integer getFlowId(Integer id);
    
    @Select({"select id from flowLog where flow_id = #{flowId}"})
    List<Integer> getIds(Integer flowId);
    
}
