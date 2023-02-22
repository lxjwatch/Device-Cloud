package center.misaki.device.Form.service;

import center.misaki.device.Form.dto.FormStrucDto;
import center.misaki.device.Form.vo.FormStrucVo;
import center.misaki.device.Form.vo.MenuFormVo;
import center.misaki.device.Form.vo.SimpleFieldVo;

import java.util.List;

/**
 * 表单结构操作实现类
 */
public interface StructureService {
    
    //获取表单结构，表单配置文档，字段配置文档
    FormStrucVo getFormStruc(int formId,String userInfo);
    
    //修改表单结构,以及字段配置文档,以及表单属性
    boolean changeFormStruc(FormStrucDto formStrucDto,String userInfo);
    
    
    //数据联动快捷获得表单字段集合
    List<SimpleFieldVo> getFieldsInForm(int formId,String userInfo);
    
    
    //获取菜单中的简单表单信息
    List<MenuFormVo> getSimpleFormsInMenu(String userInfo);
    
    //修改菜单名字
    boolean changeMenuName(int menuId,String menuName,String userInfo);
    
    //修改表单名字
    boolean changeFormName(int formId,String formName,String userInfo);

    //用户注册初始化表单
    boolean createForm(Integer menuId, Integer formType, String name, int tenementId, String formFields);

    //创建表单（这里方法名可以改成createForm）
    FormStrucDto createNormalForm(Integer menuId,Integer formType,String userInfo);

    //修改一个表单的类型
    boolean changeFormType(int formId,int formType,String userInfo);

    //删除一个表单
    boolean deleteForm(int formId,String userInfo);
    
    
    //获取这个公司里的所有表单的简单结构
    List<FormStrucVo.FormSimpleVo> getFormSimpleStruc(String userInfo);
    
    
}
