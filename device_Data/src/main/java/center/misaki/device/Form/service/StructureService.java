package center.misaki.device.Form.service;

import center.misaki.device.Form.dto.FormStrucDto;
import center.misaki.device.Form.vo.FormStrucVo;
import center.misaki.device.Form.vo.MenuFormVo;
import center.misaki.device.Form.vo.SimpleFieldVo;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 表单结构操作实现类
 */
public interface StructureService {
    
    //获取表单结构，表单配置文档，字段配置文档
    FormStrucVo getFormStruc(int formId,String userInfo);
    
    //修改表单结构或者其中
    boolean changeFormStruc(FormStrucDto formStrucDto,String userInfo);

    //数据联动快捷获得表单字段集合
    List<SimpleFieldVo> getFieldsInForm(int formId,String userInfo);

    //获取菜单中的简单表单信息
    List<MenuFormVo> getSimpleFormsInMenu(String userInfo);
    
    //修改菜单名字
    boolean changeMenuName(int menuId,String menuName,String userInfo);
    
    //修改表单名字
    boolean changeFormName(int formId,String formName,String userInfo);
    
    //创建表单
    FormStrucDto createNormalForm(Integer menuId,Integer formType,String userInfo);

    //修改表单类型
    boolean changeFormType(int formId,int formType,String userInfo);

    //删除表单
    boolean deleteForm(int formId,String userInfo);

    //获取这个公司里的所有表单的简单结构
    List<FormStrucVo.FormSimpleVo> getFormSimpleStruc(String userInfo);

    //创建菜单和空表单模板
    boolean createMenu(int tenementId);

    //注册时创建空表单
    boolean createMenuForm(Integer menuId,Integer formType,Integer tenementId,String formName);

    //创建字段
    boolean createField(String id,Integer tenementId,Integer formId,String name,Integer typeId,String detailJsom);

    //生成表单id
    String getFieldsId(Integer typeId);

    //用户注册选择创建的模板
    boolean createFormTemplate(int tenementId,int templateTenementId);
//    boolean createFormTemplate(int tenementId);
}
