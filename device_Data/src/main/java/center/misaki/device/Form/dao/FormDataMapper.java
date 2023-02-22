package center.misaki.device.Form.dao;

import center.misaki.device.Form.pojo.FormData;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author Misaki
 */
@Mapper
public interface FormDataMapper extends BaseMapper<FormData> {
    
    //根据表单ID查询一个表单中的全部数据，无论是流程中间产生的，又或者是 已经删除的
    List<FormData> selectOneFormDataAll(Integer formId,Integer tenementId);

    //根据表单ID查询一个表单中的全部数据，排除流程中间产生的，又或者是 已经删除的
    List<FormData> selectOneFormData(Integer formId,Integer tenementId);
    
    //仅仅只查询一个人提交的表单数据
    List<FormData> selectUserFormData(Integer formId,Integer tenementId,String userName);
    
    //删除数据
    int deleteOneData(FormData model);
    
    //查询一条数据中的评论信息
    String selectOneDataComment(Integer dataId);
    
    //更新一条数据的评论信息
    int updateOneDataComment(Integer dataId,String comment);

    //获取一个表单的名字
    String selectOneFormName(Integer dataId);

    //获取一个表单的Id
    Integer selectFormId(Integer dataId);
    
    List<Integer> selectIds(Integer formId);
    
}
