package center.misaki.device.Auth.service;

import center.misaki.device.AddressBook.dao.DepartmentMapper;
import center.misaki.device.AddressBook.dao.RoleMapper;
import center.misaki.device.AddressBook.dao.UserMapper;
import center.misaki.device.AddressBook.dto.Head;
import center.misaki.device.AddressBook.service.UserService;
import center.misaki.device.Auth.SecurityUtils;
import center.misaki.device.Auth.dao.*;
import center.misaki.device.Auth.dto.FormSingleAuthDto;
import center.misaki.device.Auth.dto.GroupFormDto;
import center.misaki.device.Auth.dto.JwtUserDto;
import center.misaki.device.Enum.FormAuthEnum;
import center.misaki.device.domain.Pojo.DepartmentAuthForm;
import center.misaki.device.domain.Pojo.GroupAuthForm;
import center.misaki.device.domain.Pojo.RoleAuthForm;
import center.misaki.device.domain.Pojo.UserAuthForm;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Misaki
 */
@Service
@Slf4j
public class FormCrudAuthServiceImpl implements FormCrudAuthService {
    
    private final FormAuthMapper formAuthMapper;
    private final DepartmentMapper departmentMapper;
    private final RoleMapper roleMapper;
    private final UserMapper userMapper;
    private final DepartmentAuthMapper departmentAuthMapper;
    private final GroupAuthMapper groupAuthMapper;
    private final RoleAuthMapper roleAuthMapper;
    private final UserAuthMapper userAuthMapper;
    @Autowired
    private UserService userService;

    public FormCrudAuthServiceImpl(FormAuthMapper formAuthMapper, DepartmentMapper departmentMapper, RoleMapper roleMapper, UserMapper userMapper, DepartmentAuthMapper departmentAuthMapper,
                                   GroupAuthMapper groupAuthMapper, RoleAuthMapper roleAuthMapper, UserAuthMapper userAuthMapper) {
        this.formAuthMapper = formAuthMapper;
        this.departmentMapper = departmentMapper;
        this.roleMapper = roleMapper;
        this.userMapper = userMapper;
        this.departmentAuthMapper = departmentAuthMapper;
        this.groupAuthMapper = groupAuthMapper;
        this.roleAuthMapper = roleAuthMapper;
        this.userAuthMapper = userAuthMapper;
    }
    
    @Override
    @Transactional
    public Set<Integer> getFormAuth(Integer formId) {
        JwtUserDto currentUser = SecurityUtils.getCurrentUser();
        Set<Integer> ans = new HashSet<>();
//        if(currentUser.isCreater()||currentUser.isSysAdmin()){
//            ans.add(FormAuthEnum.SUBMIT.operation);
//            ans.add(FormAuthEnum.SUBMIT_SELF.operation);
//            ans.add(FormAuthEnum.MANAGE.operation);
//            ans.add(FormAuthEnum.WATCH.operation);
//            return ans;
//        }
        //获取用户所在的部门ID集合
        List<Integer> departmentIds = departmentMapper.selectUserDepartIds
                (currentUser.getUserId(), SecurityUtils.getCurrentUser().getTenementId())
                .stream().map(m -> ((Long) m.get("departmentId")).intValue()).collect(Collectors.toList());
        List<Integer> roleIds = roleMapper.selectRoleIdsByUserId
                (currentUser.getUserId(), SecurityUtils.getCurrentUser()
                        .getTenementId()).stream()
                .map(m -> ((Long) m.get("roleId")).intValue()).collect(Collectors.toList());

        UserAuthForm userAuthForm = userAuthMapper.selectOne(new QueryWrapper<UserAuthForm>().eq("user_id", currentUser.getUserId()).eq("form_id", formId));
        if(userAuthForm!=null){
            if(userAuthForm.getSubmit()) ans.add(FormAuthEnum.SUBMIT.operation);
            if(userAuthForm.getSubmitSelf()) ans.add(FormAuthEnum.SUBMIT_SELF.operation);
            if(userAuthForm.getManage()) ans.add(FormAuthEnum.MANAGE.operation);
            if(userAuthForm.getWatch()) ans.add(FormAuthEnum.WATCH.operation);
        }
        departmentIds.forEach(departmentId -> {
            DepartmentAuthForm departmentAuthForm = departmentAuthMapper.selectOne(new QueryWrapper<DepartmentAuthForm>().eq("department_id", departmentId).eq("form_id", formId));
            if(departmentAuthForm!=null){
                if(departmentAuthForm.getSubmit()) ans.add(FormAuthEnum.SUBMIT.operation);
                if(departmentAuthForm.getSubmitSelf()) ans.add(FormAuthEnum.SUBMIT_SELF.operation);
                if(departmentAuthForm.getManage()) ans.add(FormAuthEnum.MANAGE.operation);
                if(departmentAuthForm.getWatch()) ans.add(FormAuthEnum.WATCH.operation);
            }
        });
        roleIds.forEach(roleId -> {
            RoleAuthForm roleAuthForm = roleAuthMapper.selectOne(new QueryWrapper<RoleAuthForm>().eq("role_id", roleId).eq("form_id", formId));
            if(roleAuthForm!=null){
                if(roleAuthForm.getSubmit()) ans.add(FormAuthEnum.SUBMIT.operation);
                if(roleAuthForm.getSubmitSelf()) ans.add(FormAuthEnum.SUBMIT_SELF.operation);
                if(roleAuthForm.getManage()) ans.add(FormAuthEnum.MANAGE.operation);
                if(roleAuthForm.getWatch()) ans.add(FormAuthEnum.WATCH.operation);
            }
        });

        List<GroupAuthForm> groupAuthForms = formAuthMapper.selectUserFormGroupId(currentUser.getUserId(), formId);
        groupAuthForms.forEach(g->{
            if(g.getSubmit())ans.add(FormAuthEnum.SUBMIT.operation);
            if(g.getSubmitSelf()) ans.add(FormAuthEnum.SUBMIT_SELF.operation);
            if(g.getManage())  ans.add(FormAuthEnum.MANAGE.operation);
            if(g.getWatch()) ans.add(FormAuthEnum.WATCH.operation);
        });
        
        return ans;
    }

    @Override
    @Async
    @Transactional
    public void updateSubmitAuth(FormSingleAuthDto formSingleAuthDto) {
        JwtUserDto currentUser = SecurityUtils.getCurrentUser();
        Set<Integer> department = formSingleAuthDto.getDepartment();
        Set<Integer> user = formSingleAuthDto.getUser();
        Set<Integer> role = formSingleAuthDto.getRole();

        Map<Long, Object> departAuthMap = formAuthMapper.selectDepartAuthMap(SecurityUtils.getCurrentUser().getTenementId(), formSingleAuthDto.getFormId());
        Map<Long, Object> userAuthMap = formAuthMapper.selectUserAuthMap(SecurityUtils.getCurrentUser().getTenementId(), formSingleAuthDto.getFormId());
        Map<Long, Object> roleAuthMap = formAuthMapper.selectRoleAuthMap(SecurityUtils.getCurrentUser().getTenementId(), formSingleAuthDto.getFormId());
        
        //增加权限
        department.forEach(d->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getDepartment().contains(-1)
                    ||currentUser.getAuthDto().getScope().getDepartment().contains(d)){
                if(departAuthMap.containsKey(d.longValue())){
                    HashMap<String,Object> value = (HashMap) departAuthMap.get(d.longValue());
                    if(!(Boolean) value.get("submit")){
                        departmentAuthMapper.update(null,new UpdateWrapper<DepartmentAuthForm>()
                                .eq("id",value.get("id")).set("submit",true));
                    }
                    //防止重复提交
                    departAuthMap.remove(d.longValue());
                }else{
                    DepartmentAuthForm authForm = new DepartmentAuthForm();
                    authForm.setFormId(formSingleAuthDto.getFormId());
                    authForm.setDepartmentId(d);
                    authForm.setTenementId(SecurityUtils.getCurrentUser().getTenementId());
                    authForm.setSubmit(true);
                    departmentAuthMapper.insert(authForm);
                }
            }else log.warn("此用户：{} 进行了非法越权操作,已阻止，添加departmentID 为：{} 的部门有 表单id为 {}的直接添加数据权限",
                    SecurityUtils.getCurrentUsername(),d,formSingleAuthDto.getFormId());
        });
        role.forEach(r->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getRole().contains(-1)
                    ||currentUser.getAuthDto().getScope().getRole().contains(r)){
                if(roleAuthMap.containsKey(r.longValue())){
                    HashMap<String,Object> value = (HashMap) roleAuthMap.get(r.longValue());
                    if(!(Boolean) value.get("submit")){
                        roleAuthMapper.update(null,new UpdateWrapper<RoleAuthForm>()
                                .eq("id",value.get("id")).set("submit",true));
                    }
                    roleAuthMap.remove(r.longValue());
                }else{
                    RoleAuthForm authForm = new RoleAuthForm();
                    authForm.setFormId(formSingleAuthDto.getFormId());
                    authForm.setRoleId(r);
                    authForm.setTenementId(SecurityUtils.getCurrentUser().getTenementId());
                    authForm.setSubmit(true);
                    roleAuthMapper.insert(authForm);
                }
            }else log.warn("此用户：{} 进行了非法越权操作,已阻止，添加roleID 为：{} 的角色有 表单id为 {}的直接添加数据权限",
                    SecurityUtils.getCurrentUsername(),r,formSingleAuthDto.getFormId());
        });
        user.forEach(u->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getDepartment().contains(-1)
                    ||currentUser.getAuthDto().getScope().getDepartment().stream().anyMatch(d->departmentMapper.existUserInDepart(d,u))){
                if(userAuthMap.containsKey(u.longValue())){
                    HashMap<String,Object> value = (HashMap) userAuthMap.get(u.longValue());
                    if(!(Boolean) value.get("submit")){
                        userAuthMapper.update(null,new UpdateWrapper<UserAuthForm>()
                                .eq("id",value.get("id")).set("submit",true));
                    }
                    userAuthMap.remove(u.longValue());
                }else{
                    UserAuthForm authForm = new UserAuthForm();
                    authForm.setFormId(formSingleAuthDto.getFormId());
                    authForm.setUserId(u);
                    authForm.setTenementId(SecurityUtils.getCurrentUser().getTenementId());
                    authForm.setSubmit(true);
                    userAuthMapper.insert(authForm);
                }
            }else log.warn("此用户：{} 进行了非法越权操作,已阻止，添加userID 为：{} 的用户有 表单id为 {}的直接添加数据权限",
                    SecurityUtils.getCurrentUsername(),u,formSingleAuthDto.getFormId());
        });
        
        //删除权限
        departAuthMap.forEach((k,v)->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getDepartment().contains(-1)
                    ||currentUser.getAuthDto().getScope().getDepartment().contains(k.intValue())) {
                HashMap<String,Object> value= (HashMap) v;
                if((Boolean) value.get("submit")){
                    departmentAuthMapper.update(null,new UpdateWrapper<DepartmentAuthForm>()
                            .eq("id",value.get("id")).set("submit",false));
                }
            }
        });
        roleAuthMap.forEach((k,v)->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getRole().contains(-1)
                    ||currentUser.getAuthDto().getScope().getRole().contains(k.intValue())) {
                HashMap<String,Object> value= (HashMap) v;
                if((Boolean) value.get("submit")){
                    roleAuthMapper.update(null,new UpdateWrapper<RoleAuthForm>()
                            .eq("id",value.get("id")).set("submit",false));
                }
            }
        });
        userAuthMap.forEach((k,v)->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getDepartment().contains(-1)
                    ||currentUser.getAuthDto().getScope().getDepartment().stream().anyMatch(d->departmentMapper.existUserInDepart(d,k.intValue()))) {
                HashMap<String,Object> value= (HashMap) v;
                if((Boolean) value.get("submit")){
                    userAuthMapper.update(null,new UpdateWrapper<UserAuthForm>()
                            .eq("id",value.get("id")).set("submit",false));
                }
            }
        });
        
    }

    @Override
    @Async
    @Transactional
    public void updateSubmitAndSelfAuth(FormSingleAuthDto formSingleAuthDto) {

        JwtUserDto currentUser = SecurityUtils.getCurrentUser();
        Set<Integer> department = formSingleAuthDto.getDepartment();
        Set<Integer> user = formSingleAuthDto.getUser();
        Set<Integer> role = formSingleAuthDto.getRole();

        Map<Long, Object> departAuthMap = formAuthMapper.selectDepartAuthMap(SecurityUtils.getCurrentUser().getTenementId(), formSingleAuthDto.getFormId());
        Map<Long, Object> userAuthMap = formAuthMapper.selectUserAuthMap(SecurityUtils.getCurrentUser().getTenementId(), formSingleAuthDto.getFormId());
        Map<Long, Object> roleAuthMap = formAuthMapper.selectRoleAuthMap(SecurityUtils.getCurrentUser().getTenementId(), formSingleAuthDto.getFormId());

        //增加权限
        department.forEach(d -> {
            if (currentUser.isCreater() || currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getDepartment().contains(-1)
                    || currentUser.getAuthDto().getScope().getDepartment().contains(d)) {
                if (departAuthMap.containsKey(d.longValue())) {
                    HashMap<String,Object> value = (HashMap) departAuthMap.get(d.longValue());
                    if (!(Boolean) value.get("submit_self")) {
                        departmentAuthMapper.update(null, new UpdateWrapper<DepartmentAuthForm>()
                                .eq("id", value.get("id")).set("submit_self", true));
                    }
                    departAuthMap.remove(d.longValue());
                } else {
                    DepartmentAuthForm authForm = new DepartmentAuthForm();
                    authForm.setFormId(formSingleAuthDto.getFormId());
                    authForm.setDepartmentId(d);
                    authForm.setTenementId(SecurityUtils.getCurrentUser().getTenementId());
                    authForm.setSubmitSelf(true);
                    departmentAuthMapper.insert(authForm);
                }
            } else log.warn("此用户：{} 进行了非法越权操作,已阻止，添加departmentID 为：{} 的部门有 表单id为 {}的提交并管理本人数据权限",
                    SecurityUtils.getCurrentUsername(),d, formSingleAuthDto.getFormId());
        });
        role.forEach(r->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getRole().contains(-1)
                    ||currentUser.getAuthDto().getScope().getRole().contains(r)){
                if(roleAuthMap.containsKey(r.longValue())){
                    HashMap<String,Object> value = (HashMap) roleAuthMap.get(r.longValue());
                    if(!(Boolean) value.get("submit_self")){
                        roleAuthMapper.update(null,new UpdateWrapper<RoleAuthForm>()
                                .eq("id",value.get("id")).set("submit_self",true));
                    }
                    roleAuthMap.remove(r.longValue());
                }else{
                    RoleAuthForm authForm = new RoleAuthForm();
                    authForm.setFormId(formSingleAuthDto.getFormId());
                    authForm.setRoleId(r);
                    authForm.setTenementId(SecurityUtils.getCurrentUser().getTenementId());
                    authForm.setSubmitSelf(true);
                    roleAuthMapper.insert(authForm);
                }
            }else log.warn("此用户：{} 进行了非法越权操作,已阻止，添加roleID 为：{} 的角色有 表单id为 {}的提交并管理本人数据权限",
                    SecurityUtils.getCurrentUsername(),r,formSingleAuthDto.getFormId());
        });
        user.forEach(u->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getDepartment().contains(-1)
                    ||currentUser.getAuthDto().getScope().getDepartment().stream().anyMatch(d->departmentMapper.existUserInDepart(d,u))){
                if(userAuthMap.containsKey(u.longValue())){
                    HashMap<String,Object> value = (HashMap) userAuthMap.get(u.longValue());
                    if(!(Boolean)value.get("submit_self")){
                        userAuthMapper.update(null,new UpdateWrapper<UserAuthForm>()
                                .eq("id",value.get("id")).set("submit_self",true));
                    }
                    userAuthMap.remove(u.longValue());
                }else{
                    UserAuthForm authForm = new UserAuthForm();
                    authForm.setFormId(formSingleAuthDto.getFormId());
                    authForm.setUserId(u);
                    authForm.setTenementId(SecurityUtils.getCurrentUser().getTenementId());
                    authForm.setSubmitSelf(true);
                    userAuthMapper.insert(authForm);
                }
            }else log.warn("此用户：{} 进行了非法越权操作,已阻止，添加userID 为：{} 的用户有 表单id为 {}的提交并管理本人数据权限",
                    SecurityUtils.getCurrentUsername(),u,formSingleAuthDto.getFormId());
        });

        //删除权限
        departAuthMap.forEach((k,v)->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getDepartment().contains(-1)
                    ||currentUser.getAuthDto().getScope().getDepartment().contains(k.intValue())) {
                HashMap<String,Object> value= (HashMap) v;
                if((Boolean) value.get("submit_self")){
                    departmentAuthMapper.update(null,new UpdateWrapper<DepartmentAuthForm>()
                            .eq("id",value.get("id")).set("submit_self",false));
                }
            }
        });
        roleAuthMap.forEach((k,v)->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getRole().contains(-1)
                    ||currentUser.getAuthDto().getScope().getRole().contains(k)) {
                HashMap<String,Object> value= (HashMap) v;
                if((Boolean) value.get("submit_self")){
                    roleAuthMapper.update(null,new UpdateWrapper<RoleAuthForm>()
                            .eq("id",value.get("id")).set("submit_self",false));
                }
            }
        });
        userAuthMap.forEach((k,v)->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getDepartment().contains(-1)
                    ||currentUser.getAuthDto().getScope().getDepartment().stream().anyMatch(d->departmentMapper.existUserInDepart(d,k.intValue()))) {
                HashMap<String,Object> value= (HashMap) v;
                if((Boolean) value.get("submit_self")){
                    userAuthMapper.update(null,new UpdateWrapper<UserAuthForm>()
                            .eq("id",value.get("id")).set("submit_self",false));
                }
            }
        });

    }

    @Override
    @Async
    @Transactional
    public void updateManageAuth(FormSingleAuthDto formSingleAuthDto) {
        JwtUserDto currentUser = SecurityUtils.getCurrentUser();
        Set<Integer> department = formSingleAuthDto.getDepartment();
        Set<Integer> user = formSingleAuthDto.getUser();
        Set<Integer> role = formSingleAuthDto.getRole();

        Map<Long, Object> departAuthMap = formAuthMapper.selectDepartAuthMap(SecurityUtils.getCurrentUser().getTenementId(), formSingleAuthDto.getFormId());
        Map<Long, Object> userAuthMap = formAuthMapper.selectUserAuthMap(SecurityUtils.getCurrentUser().getTenementId(), formSingleAuthDto.getFormId());
        Map<Long, Object> roleAuthMap = formAuthMapper.selectRoleAuthMap(SecurityUtils.getCurrentUser().getTenementId(), formSingleAuthDto.getFormId());

        //增加权限
        department.forEach(d->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getDepartment().contains(-1)
                    ||currentUser.getAuthDto().getScope().getDepartment().contains(d)){
                if(departAuthMap.containsKey(d.longValue())){
                    HashMap<String,Object> value = (HashMap) departAuthMap.get(d.longValue());
                    if(!(Boolean) value.get("manage")){
                        departmentAuthMapper.update(null,new UpdateWrapper<DepartmentAuthForm>()
                                .eq("id",value.get("id")).set("manage",true));
                    }
                    departAuthMap.remove(d.longValue());
                }else{
                    DepartmentAuthForm authForm = new DepartmentAuthForm();
                    authForm.setFormId(formSingleAuthDto.getFormId());
                    authForm.setDepartmentId(d);
                    authForm.setTenementId(SecurityUtils.getCurrentUser().getTenementId());
                    authForm.setManage(true);
                    departmentAuthMapper.insert(authForm);
                }
            }else log.warn("此用户：{} 进行了非法越权操作,已阻止，添加departmentID 为：{} 的部门有 表单id为 {}的管理全部数据权限",
                    SecurityUtils.getCurrentUsername(),d,formSingleAuthDto.getFormId());
        });
        role.forEach(r->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getRole().contains(-1)
                    ||currentUser.getAuthDto().getScope().getRole().contains(r)){
                if(roleAuthMap.containsKey(r.longValue())){
                    HashMap<String,Object> value = (HashMap) roleAuthMap.get(r.longValue());
                    if(!(Boolean) value.get("manage")){
                        roleAuthMapper.update(null,new UpdateWrapper<RoleAuthForm>()
                                .eq("id",value.get("id")).set("manage",true));
                    }
                    roleAuthMap.remove(r.longValue());
                }else{
                    RoleAuthForm authForm = new RoleAuthForm();
                    authForm.setFormId(formSingleAuthDto.getFormId());
                    authForm.setRoleId(r);
                    authForm.setTenementId(SecurityUtils.getCurrentUser().getTenementId());
                    authForm.setManage(true);
                    roleAuthMapper.insert(authForm);
                }
            }else log.warn("此用户：{} 进行了非法越权操作,已阻止，添加roleID 为：{} 的角色有 表单id为 {}的管理全部数据权限",
                    SecurityUtils.getCurrentUsername(),r,formSingleAuthDto.getFormId());
        });
        user.forEach(u->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getDepartment().contains(-1)
                    ||currentUser.getAuthDto().getScope().getDepartment().stream().anyMatch(d->departmentMapper.existUserInDepart(d,u))){
                if(userAuthMap.containsKey(u.longValue())){
                    HashMap<String,Object> value = (HashMap) userAuthMap.get(u.longValue());
                    if(!(Boolean) value.get("manage")){
                        userAuthMapper.update(null,new UpdateWrapper<UserAuthForm>()
                                .eq("id",value.get("id")).set("manage",true));
                    }
                    userAuthMap.remove(u.longValue());
                }else{
                    UserAuthForm authForm = new UserAuthForm();
                    authForm.setFormId(formSingleAuthDto.getFormId());
                    authForm.setUserId(u);
                    authForm.setTenementId(SecurityUtils.getCurrentUser().getTenementId());
                    authForm.setManage(true);
                    userAuthMapper.insert(authForm);
                }
            }else log.warn("此用户：{} 进行了非法越权操作,已阻止，添加userID 为：{} 的用户有 表单id为 {}的管理全部数据权限",
                    SecurityUtils.getCurrentUsername(),u,formSingleAuthDto.getFormId());
        });

        //删除权限
        departAuthMap.forEach((k,v)->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getDepartment().contains(-1)
                    ||currentUser.getAuthDto().getScope().getDepartment().contains(k.intValue())) {
                HashMap<String,Object> value= (HashMap) v;
                if((Boolean) value.get("manage")){
                    departmentAuthMapper.update(null,new UpdateWrapper<DepartmentAuthForm>()
                            .eq("id",value.get("id")).set("manage",false));
                }
            }
        });
        roleAuthMap.forEach((k,v)->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getRole().contains(-1)
                    ||currentUser.getAuthDto().getScope().getRole().contains(k.intValue())) {
                HashMap<String,Object> value= (HashMap) v;
                if((Boolean) value.get("manage")){
                    roleAuthMapper.update(null,new UpdateWrapper<RoleAuthForm>()
                            .eq("id",value.get("id")).set("manage",false));
                }
            }
        });
        userAuthMap.forEach((k,v)->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getDepartment().contains(-1)
                    ||currentUser.getAuthDto().getScope().getDepartment().stream().anyMatch(d->departmentMapper.existUserInDepart(d,k.intValue()))) {
                HashMap<String,Object> value= (HashMap) v;
                if((Boolean) value.get("manage")){
                    userAuthMapper.update(null,new UpdateWrapper<UserAuthForm>()
                            .eq("id",value.get("id")).set("manage",false));
                }
            }
        });
    }

    @Override
    @Async
    @Transactional
    public void updateWatchAuth(FormSingleAuthDto formSingleAuthDto) {
        JwtUserDto currentUser = SecurityUtils.getCurrentUser();
        Set<Integer> department = formSingleAuthDto.getDepartment();
        Set<Integer> user = formSingleAuthDto.getUser();
        Set<Integer> role = formSingleAuthDto.getRole();

        Map<Long, Object> departAuthMap = formAuthMapper.selectDepartAuthMap(SecurityUtils.getCurrentUser().getTenementId(), formSingleAuthDto.getFormId());
        Map<Long, Object> userAuthMap = formAuthMapper.selectUserAuthMap(SecurityUtils.getCurrentUser().getTenementId(), formSingleAuthDto.getFormId());
        Map<Long, Object> roleAuthMap = formAuthMapper.selectRoleAuthMap(SecurityUtils.getCurrentUser().getTenementId(), formSingleAuthDto.getFormId());

        //增加权限
        department.forEach(d->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getDepartment().contains(-1)
                    ||currentUser.getAuthDto().getScope().getDepartment().contains(d)){
                if(departAuthMap.containsKey(d.longValue())){
                    HashMap<String,Object> value = (HashMap) departAuthMap.get(d.longValue());
                    if(!(Boolean) value.get("watch")){
                        departmentAuthMapper.update(null,new UpdateWrapper<DepartmentAuthForm>()
                                .eq("id",value.get("id")).set("watch",true));
                    }
                    departAuthMap.remove(d.longValue());
                }else{
                    DepartmentAuthForm authForm = new DepartmentAuthForm();
                    authForm.setFormId(formSingleAuthDto.getFormId());
                    authForm.setDepartmentId(d);
                    authForm.setTenementId(SecurityUtils.getCurrentUser().getTenementId());
                    authForm.setWatch(true);
                    departmentAuthMapper.insert(authForm);
                }
            }else log.warn("此用户：{} 进行了非法越权操作,已阻止，添加departmentID 为：{} 的部门有 表单id为 {}的查看全部数据权限",
                    SecurityUtils.getCurrentUsername(),d,formSingleAuthDto.getFormId());
        });
        role.forEach(r->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getRole().contains(-1)
                    ||currentUser.getAuthDto().getScope().getRole().contains(r)){
                if(roleAuthMap.containsKey(r.longValue())){
                    HashMap<String,Object> value = (HashMap) roleAuthMap.get(r.longValue());
                    if(!(Boolean) value.get("watch")){
                        roleAuthMapper.update(null,new UpdateWrapper<RoleAuthForm>()
                                .eq("id",value.get("id")).set("watch",true));
                    }
                    roleAuthMap.remove(r.longValue());
                }else{
                    RoleAuthForm authForm = new RoleAuthForm();
                    authForm.setFormId(formSingleAuthDto.getFormId());
                    authForm.setRoleId(r);
                    authForm.setTenementId(SecurityUtils.getCurrentUser().getTenementId());
                    authForm.setWatch(true);
                    roleAuthMapper.insert(authForm);
                }
            }else log.warn("此用户：{} 进行了非法越权操作,已阻止，添加roleID 为：{} 的角色有 表单id为 {}的查看全部数据权限",
                    SecurityUtils.getCurrentUsername(),r,formSingleAuthDto.getFormId());
        });
        user.forEach(u->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getDepartment().contains(-1)
                    ||currentUser.getAuthDto().getScope().getDepartment().stream().anyMatch(d->departmentMapper.existUserInDepart(d,u))){
                if(userAuthMap.containsKey(u.longValue())){
                    HashMap<String,Object> value = (HashMap) userAuthMap.get(u.longValue());
                    if(!(Boolean) value.get("watch")){
                        userAuthMapper.update(null,new UpdateWrapper<UserAuthForm>()
                                .eq("id",value.get("id")).set("watch",true));
                    }
                    userAuthMap.remove(u);
                }else{
                    UserAuthForm authForm = new UserAuthForm();
                    authForm.setFormId(formSingleAuthDto.getFormId());
                    authForm.setUserId(u);
                    authForm.setTenementId(SecurityUtils.getCurrentUser().getTenementId());
                    authForm.setWatch(true);
                    userAuthMapper.insert(authForm);
                }
            }else log.warn("此用户：{} 进行了非法越权操作,已阻止，添加userID 为：{} 的用户有 表单id为 {}的查看全部数据权限",
                    SecurityUtils.getCurrentUsername(),u,formSingleAuthDto.getFormId());
        });

        //删除权限
        departAuthMap.forEach((k,v)->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getDepartment().contains(-1)
                    ||currentUser.getAuthDto().getScope().getDepartment().contains(k.intValue())) {
                HashMap<String,Object> value= (HashMap) v;
                if((Boolean) value.get("watch")){
                    departmentAuthMapper.update(null,new UpdateWrapper<DepartmentAuthForm>()
                            .eq("id",value.get("id")).set("watch",false));
                }
            }
        });
        roleAuthMap.forEach((k,v)->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getRole().contains(-1)
                    ||currentUser.getAuthDto().getScope().getRole().contains(k.intValue())) {
                HashMap<String,Object> value= (HashMap) v;
                if((Boolean) value.get("watch")){
                    roleAuthMapper.update(null,new UpdateWrapper<RoleAuthForm>()
                            .eq("id",value.get("id")).set("watch",false));
                }
            }
        });
        userAuthMap.forEach((k,v)->{
            if(currentUser.isCreater()||currentUser.isSysAdmin()
                    || currentUser.getAuthDto().getScope().getDepartment().contains(-1)
                    ||currentUser.getAuthDto().getScope().getDepartment().stream().anyMatch(d->departmentMapper.existUserInDepart(d,k.intValue()))) {
                HashMap<String,Object> value= (HashMap) v;
                if((Boolean) value.get("watch")){
                    userAuthMapper.update(null,new UpdateWrapper<UserAuthForm>()
                            .eq("id",value.get("id")).set("watch",false));
                }
            }
        });
    }
    
    @Override
    @Transactional
    public List<FormSingleAuthDto.FormSingleAuthVo> getAuthForOneForm(Integer formId) {

        List<DepartmentAuthForm> departmentAuthForms = formAuthMapper.selectDepartAuthForFormId(SecurityUtils.getCurrentUser().getTenementId(), formId);
        List<RoleAuthForm> roleAuthForms = formAuthMapper.selectRoleAuthForFormId(SecurityUtils.getCurrentUser().getTenementId(), formId);
        List<UserAuthForm> userAuthForms = formAuthMapper.selectUserAuthForFormId(SecurityUtils.getCurrentUser().getTenementId(), formId);
        Map<Integer, List<Object>> stringListHashMap = new HashMap<>();
        stringListHashMap.put(FormAuthEnum.SUBMIT.operation,new ArrayList<>());
        stringListHashMap.put(FormAuthEnum.SUBMIT_SELF.operation, new ArrayList<>());
        stringListHashMap.put(FormAuthEnum.MANAGE.operation, new ArrayList<>());
        stringListHashMap.put(FormAuthEnum.WATCH.operation, new ArrayList<>());



        departmentAuthForms.forEach(d->{
            if(d.getSubmit()) stringListHashMap.get(FormAuthEnum.SUBMIT.operation).add(d);
            if(d.getSubmitSelf()) stringListHashMap.get(FormAuthEnum.SUBMIT_SELF.operation).add(d);
            if(d.getManage()) stringListHashMap.get(FormAuthEnum.MANAGE.operation).add(d);
            if(d.getWatch()) stringListHashMap.get(FormAuthEnum.WATCH.operation).add(d);
        });
        roleAuthForms.forEach(d->{
            if(d.getSubmit()) stringListHashMap.get(FormAuthEnum.SUBMIT.operation).add(d);
            if(d.getSubmitSelf()) stringListHashMap.get(FormAuthEnum.SUBMIT_SELF.operation).add(d);
            if(d.getManage()) stringListHashMap.get(FormAuthEnum.MANAGE.operation).add(d);
            if(d.getWatch()) stringListHashMap.get(FormAuthEnum.WATCH.operation).add(d);
        });
        userAuthForms.forEach(d->{
            if(d.getSubmit()) stringListHashMap.get(FormAuthEnum.SUBMIT.operation).add(d);
            if(d.getSubmitSelf()) stringListHashMap.get(FormAuthEnum.SUBMIT_SELF.operation).add(d);
            if(d.getManage()) stringListHashMap.get(FormAuthEnum.MANAGE.operation).add(d);
            if(d.getWatch()) stringListHashMap.get(FormAuthEnum.WATCH.operation).add(d);
        });

        List<FormSingleAuthDto.FormSingleAuthVo> ans = new ArrayList<>();
        stringListHashMap.forEach((k,v)->{
            FormSingleAuthDto.FormSingleAuthVo formSingleAuthVo = new FormSingleAuthDto.FormSingleAuthVo();
            formSingleAuthVo.setOperation(k);
            formSingleAuthVo.setDepartment(new HashMap<>());
            formSingleAuthVo.setUser(new HashMap<>());
            formSingleAuthVo.setRole(new HashMap<>());
            v.forEach(v1->{
                if(v1 instanceof DepartmentAuthForm){
                    DepartmentAuthForm value= (DepartmentAuthForm) v1;
                    formSingleAuthVo.getDepartment().put(value.getDepartmentId(),departmentMapper.selectNameById(value.getDepartmentId()));
                }else if(v1 instanceof RoleAuthForm){
                    RoleAuthForm value= (RoleAuthForm) v1;
                    formSingleAuthVo.getRole().put(value.getRoleId(),roleMapper.selectNameById(value.getRoleId()));
                }else {
                    UserAuthForm value= (UserAuthForm) v1;
                    formSingleAuthVo.getUser().put(value.getUserId(),userMapper.selectUsernameById(value.getUserId()));
                }
            });
            ans.add(formSingleAuthVo);
        });
        return ans;
    }

    @Override
    @Transactional
    public boolean createAuthGroup(GroupFormDto groupFormDto) {
        GroupAuthForm groupAuthForm = new GroupAuthForm();
        groupAuthForm.setFormId(groupFormDto.getFormId());
        groupAuthForm.setGroupDescription(groupFormDto.getDescription());
        groupAuthForm.setDataLink(groupFormDto.getData());
        groupAuthForm.setTenementId(SecurityUtils.getCurrentUser().getTenementId());
        groupAuthForm.setName(groupFormDto.getName());
        Set<Integer> operations = groupFormDto.getOperations();
        if(operations.contains(FormAuthEnum.SUBMIT.operation)){
            groupAuthForm.setSubmit(true);
        }
        if(operations.contains(FormAuthEnum.SUBMIT_SELF.operation)){
            groupAuthForm.setSubmitSelf(true);
        }
        if(operations.contains(FormAuthEnum.MANAGE.operation)){
            groupAuthForm.setManage(true);
        }
        if(operations.contains(FormAuthEnum.WATCH.operation)){
            groupAuthForm.setWatch(true);
        }
        int i = groupAuthMapper.insert(groupAuthForm);
        return i>0;
    }

    @Override
    @Async
    @Transactional
    public void updateGroupAuth(GroupFormDto groupFormDto) {
        GroupAuthForm groupAuthForm = groupAuthMapper.selectById(groupFormDto.getId());
        groupAuthForm.setGroupDescription(groupFormDto.getDescription());
        groupAuthForm.setDataLink(groupFormDto.getData());
        groupAuthForm.setName(groupFormDto.getName());
        Set<Integer> operations = groupFormDto.getOperations();
        if(operations.contains(FormAuthEnum.SUBMIT.operation)){
            groupAuthForm.setSubmit(true);
        }
        if(operations.contains(FormAuthEnum.SUBMIT_SELF.operation)){
            groupAuthForm.setSubmitSelf(true);
        }
        if(operations.contains(FormAuthEnum.MANAGE.operation)){
            groupAuthForm.setManage(true);
        }
        if(operations.contains(FormAuthEnum.WATCH.operation)){
            groupAuthForm.setWatch(true);
        }
        int i = groupAuthMapper.updateById(groupAuthForm);
        log.info("更新一个权限组：{}，更新内容：{}",i>0?"成功":"失败",groupFormDto);
        
    }

    @Override
    @Async
    @Transactional
    public void updateUserGroup(Head head,Integer groupId) {
        List<Integer> originIds = formAuthMapper.selectUserIdsGroup(groupId);
        Set<Integer> userIds = userService.getUserIdsFromHead(head);
        int n=userIds.size();
        int succ=0;
        int del=0;
        for (Integer originId : originIds) {
            if(!userIds.contains(originId)){
                int i = formAuthMapper.deleteOneUserGroup(groupId,originId);
                del+=i;
            }else userIds.remove(originId);
        }
        for (Integer userId : userIds) {
            int i = formAuthMapper.insertIntoUserGroup(SecurityUtils.getCurrentUser().getTenementId(), userId,groupId);
            succ+=i;
        }
        log.info("更新表单权限组人数完成,传入人数：{} 人，组ID：{},新增：{} 人，删除：{} 人",n,groupId,succ,del);
    }

    
    @Override
    @Transactional
    public List<GroupAuthForm> getFormAuthGroup(Integer formId) {
        return groupAuthMapper.selectList(new QueryWrapper<GroupAuthForm>().eq("form_id", formId));
    }

}
