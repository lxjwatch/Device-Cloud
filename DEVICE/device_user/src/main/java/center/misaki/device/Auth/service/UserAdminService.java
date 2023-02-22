package center.misaki.device.Auth.service;

import center.misaki.device.Auth.NorAdminGVo;
import center.misaki.device.Auth.dto.AuthDto;

import java.util.List;
import java.util.Map;

/**
 * 管理员Service
 */
public interface UserAdminService {
    
    
    //获取所有的系统管理员
    Map<Integer,Object> getAllSysAdmin();
    
    
    //获取所有的普通管理组以及管理组的成员
    List<NorAdminGVo> getAllNormalGroupAdmin();
   
    //更新普通管理员组配置
    void updateNormalAdminConfig(AuthDto authDto,Integer groupId);

    //修改普通管理组名
    void updateNormalAdminName(Integer groupId,String name);
    
    //新增一个系统管理员
    boolean addSysAdmins(List<Integer> userId);
    
    //删除一个系统管理员
    boolean deleteSysAdmin(Integer userId);
    
    
    //创造一个普通管理员组
    boolean addNorAdminGroup(AuthDto authDto);
    
    
    //设置用户为普通管理员
    boolean setupAdmins(List<Integer> userIds,Integer groupId);
    
    
    //移除一个用户从当前管理员组中
    boolean deleteNormalAdmin(Integer userId);
    
    
    //删除普通管理员组
    boolean deleteNormalGroup(Integer groupId);
    
    
    
}
