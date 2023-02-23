package center.misaki.device.Form.dao;

import center.misaki.device.Form.pojo.DataModifyLog;
import center.misaki.device.Form.pojo.FormModifyLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 接口处理单条和多条日志与数据库交互
 */
@Mapper
public interface DataLogMapper extends BaseMapper<DataModifyLog>{
    
    //根据dataID 查询这一条数据的日志，遵循时间降序
    List<DataModifyLog> selectByDataId(Integer dataId);
    
    
    //根据表ID  查询这一张表的批量修改日志
    List<FormModifyLog> selectByFormId(Integer formId,Integer tenementId);
    
    
    //添加一张表的批量修改日志
    int insertBatchModifyLog(FormModifyLog formModifyLog);
    
    
    
}
