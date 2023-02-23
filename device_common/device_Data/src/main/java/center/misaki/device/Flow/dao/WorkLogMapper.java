package center.misaki.device.Flow.dao;

import center.misaki.device.Flow.WorkLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

@Mapper
public interface WorkLogMapper extends BaseMapper<WorkLog> {
    
    
    String getFlowNodes(Integer flowLogId);
    
    Integer getStartUserId(Integer flowLogId);
    
    LocalDateTime getWorkStartTime(Integer workId);
    
}
