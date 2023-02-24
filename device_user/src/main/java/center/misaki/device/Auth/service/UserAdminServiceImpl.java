package center.misaki.device.Auth.service;

import center.misaki.device.AddressBook.dao.UserMapper;
import center.misaki.device.AddressBook.vo.UserVo;
import center.misaki.device.Auth.NorAdminGVo;
import center.misaki.device.Auth.SecurityUtils;
import center.misaki.device.Auth.dao.NormalAdminMapper;
import center.misaki.device.Auth.dao.SysAdminMapper;
import center.misaki.device.Auth.dto.AuthDto;
import center.misaki.device.Auth.pojo.NormalAdmin;
import center.misaki.device.Auth.pojo.SysAdmin;
import center.misaki.device.domain.Pojo.User;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Misaki
 */
@Service
@Slf4j
public class UserAdminServiceImpl implements UserAdminService {
    
    private final UserMapper userMapper;
    private final SysAdminMapper sysAdminMapper;
    private final NormalAdminMapper normalAdminMapper;
    

    public UserAdminServiceImpl(UserMapper userMapper, SysAdminMapper sysAdminMapper, 
                                NormalAdminMapper normalAdminMapper) {
        this.userMapper = userMapper;
        this.sysAdminMapper = sysAdminMapper;
        this.normalAdminMapper = normalAdminMapper;
    }

    @Override
    public Map<Integer, Object> getAllSysAdmin() {
        return sysAdminMapper.selectMapSysAdmin(SecurityUtils.getCurrentUser().getTenementId());
    }

    @Override
    public List<NorAdminGVo> getAllNormalGroupAdmin() {
        List<User> users = userMapper.selectList(new QueryWrapper<User>().eq("tenement_id", SecurityUtils.getCurrentUser().getTenementId()).ne("normal_admin_group_id", -1));
        List<NormalAdmin> normalAdmins = normalAdminMapper.selectList(new QueryWrapper<NormalAdmin>().eq("tenement_id", SecurityUtils.getCurrentUser().getTenementId()));
        Map<Integer, List<UserVo.SimpleUserVo>> userMap = new HashMap<>();
        users.forEach(u->{
            if(!userMap.containsKey(u.getNormalAdminGroupId())){
                userMap.put(u.getNormalAdminGroupId(),new ArrayList<>());
            }
            UserVo.SimpleUserVo simpleUserVo = new UserVo.SimpleUserVo();
            simpleUserVo.setUserName(u.getUsername());
            simpleUserVo.setUserId(u.getId());
            userMap.get(u.getNormalAdminGroupId()).add(simpleUserVo);
        });
        
        
        List<NorAdminGVo> ans = new ArrayList<>();
        for (NormalAdmin normalAdmin : normalAdmins) {
            NorAdminGVo norAdminGVo = new NorAdminGVo();
            if(userMap.containsKey(normalAdmin.getId())){
                norAdminGVo.setAdmins(userMap.get(normalAdmin.getId()));
            }else norAdminGVo.setAdmins(new ArrayList<>());
            
            norAdminGVo.setAuthDto(JSON.parseObject(normalAdmin.getConfig(),AuthDto.class));
            norAdminGVo.setName(normalAdmin.getName());
            norAdminGVo.setId(normalAdmin.getId());
            ans.add(norAdminGVo);
        }
        return ans;
    }

    @Override
    @Async
    @Transactional
    public void updateNormalAdminConfig(AuthDto authDto, Integer groupId) {
        int i = normalAdminMapper.updateConfig(JSON.toJSONString(authDto), groupId);
        log.info("更新了{}条记录的config",i);
    }

    @Override
    @Transactional
    @Async
    public void updateNormalAdminName(Integer groupId, String name) {
        NormalAdmin normalAdmin = normalAdminMapper.selectById(groupId);
        int i = normalAdminMapper.update(null, new UpdateWrapper<NormalAdmin>().eq("id", groupId).set("name", name));
        log.info("更新了{}条记录的name",i);
        AuthDto authDto = JSON.parseObject(normalAdmin.getConfig(), AuthDto.class);
        authDto.setName(name);
        updateNormalAdminConfig(authDto, groupId);
    }

    @Override
    @Transactional
    public boolean addSysAdmins(List<Integer> userIds) {
        int sum=userIds.size();
        int succ=0;
        for (Integer userId : userIds) {
            SysAdmin sysAdmin = new SysAdmin();
            sysAdmin.setUserId(userId);
            sysAdmin.setTenementId(SecurityUtils.getCurrentUser().getTenementId());
            int i = sysAdminMapper.insert(sysAdmin);
            succ+=i;
        }
        log.info("需要创造{}个系统管理员，由用户 {}，创造了 {} 个系统管理员",sum,SecurityUtils.getCurrentUsername(),succ);
        return true;
    }

    @Override
    @Transactional
    public boolean deleteSysAdmin(Integer userId) {
        int i = sysAdminMapper.delete(new QueryWrapper<SysAdmin>().eq("tenement_id", SecurityUtils.getCurrentUser().getTenementId()).eq("user_id", userId));
        return i>0;
    }

    @Override
    @Transactional
    public boolean addNorAdminGroup(AuthDto authDto) {
        NormalAdmin normalAdmin = new NormalAdmin();
        normalAdmin.setConfig(JSON.toJSONString(authDto));
        normalAdmin.setName(authDto.getName());
        normalAdmin.setTenementId(SecurityUtils.getCurrentUser().getTenementId());
        int i = normalAdminMapper.insert(normalAdmin);
        log.info("由用户 {},{}创造了一个普通管理组",SecurityUtils.getCurrentUsername(),i>0?"成功":"失败");
        return i>0;
    }

    @Override
    @Transactional
    public boolean setupAdmins(List<Integer> userIds,Integer groupId) {
        int sum=userIds.size();
        int succ=0;
        for (Integer userId : userIds) {
            int i = userMapper.update(null, new UpdateWrapper<User>().eq("id", userId).eq("is_creater", false).set("normal_admin_group_id", groupId));
            succ += i;
        }
        log.info("需要将 {} 名用户设置为普通管理员，实际成功设置了 {} 名",sum,succ);
        return true;
    }

    @Override
    @Transactional
    public boolean deleteNormalAdmin(Integer userId) {
        int i = userMapper.update(null, new UpdateWrapper<User>().eq("id", userId).set("normal_admin_group_id", -1));
        return i>0;
    }

    @Override
    @Transactional
    public boolean deleteNormalGroup(Integer groupId) {
        List<User> users = userMapper.selectList(new QueryWrapper<User>().eq("tenement_id", SecurityUtils.getCurrentUser().getTenementId())
                .eq("normal_admin_group_id", groupId));
        if (users != null &&!users.isEmpty()) return false;
        int i = normalAdminMapper.deleteById(groupId);
        return i>0;
    }


}
