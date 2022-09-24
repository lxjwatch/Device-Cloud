package center.misaki.device.Form.dao;

import center.misaki.device.Form.vo.MenuFormVo;
import center.misaki.device.domain.Pojo.Form;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FormMapper extends BaseMapper<Form> {
    
    //查询一张表单中正在使用的字段序号集合(被序列化过的)
    String selectOneFormFields(Integer formId,Integer tenementId);
    
    //查询一张表单中的表单类型
    Integer selectType(Integer formId);
    
    //查询表单简单信息
    List<MenuFormVo.SimpleFormVo> selectSimpleForms(Integer menuId); 
    
    
}
