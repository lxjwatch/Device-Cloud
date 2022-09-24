package center.misaki.device.Flow.dao;

import center.misaki.device.Flow.Flow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FlowMapper extends BaseMapper<Flow> {
    
    @Select({"select view_data from flow where id = #{id}"})
    String selectViewData(Integer flowId);
    
    @Select({"select id from flow where form_id = #{formId} and enable = true"})
    Integer selectFlowIdForFormId(Integer formId);
    
    @Select({"select flow_property from flow where id = #{flowId}"})
    String selectFlowProperty(Integer flowId);
    
}
