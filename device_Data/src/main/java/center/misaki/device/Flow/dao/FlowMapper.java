package center.misaki.device.Flow.dao;

import center.misaki.device.Flow.Flow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FlowMapper extends BaseMapper<Flow> {
    
    @Select({"select flow.view_data from flow where flow.id = #{id}"})
    String selectViewData(Integer flowId);
    
    @Select({"select flow.id from flow where flow.form_id = #{formId} and flow.enable = true"})
    Integer selectFlowIdForFormId(Integer formId);
    
    @Select({"select flow.flow_property from flow where flow.id = #{flowId}"})
    String selectFlowProperty(Integer flowId);
    
}
