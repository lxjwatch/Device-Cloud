package center.misaki.device.Form.dao;

import center.misaki.device.Form.vo.MenuFormVo;
import center.misaki.device.domain.Pojo.Form;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FormMapper extends BaseMapper<Form> {
    
    //查询一张表单中正在使用的字段序号集合(被序列化过的)
    String selectOneFormFields(Integer formId,Integer tenementId);
    
    //查询一张表单中的表单类型
    Integer selectType(Integer formId);
    
    //查询表单简单信息
    List<MenuFormVo.SimpleFormVo> selectSimpleForms(Integer menuId); 
    
    //查询一张表单的id
    @Select({"select id from form where tenement_id =#{arg0} and form_name=#{arg1}"})
    Long selectIdByTenementIdAndFormName(Integer tenementId,String formName);

    //查询一张表单的名字
    @Select({"select form_name from form where id=#{arg0}"})
    String selectFormNameById(Integer formId);
}
