package center.misaki.device.AddressBook.service.impl;

import center.misaki.device.AddressBook.dao.DepartmentMapper;
import center.misaki.device.AddressBook.dao.UserMapper;
import center.misaki.device.AddressBook.dto.Head;
import center.misaki.device.AddressBook.dto.UserDto;
import center.misaki.device.AddressBook.dto.UserRegisterDto;
import center.misaki.device.AddressBook.pojo.Department;
import center.misaki.device.AddressBook.service.UserService;
import center.misaki.device.AddressBook.vo.UserVo;
import center.misaki.device.Auth.SecurityUtils;
import center.misaki.device.Enum.UserStateEnum;
import center.misaki.device.Form.dao.MenuMapper;
import center.misaki.device.Form.service.StructureService;
import center.misaki.device.domain.Pojo.Menu;
import center.misaki.device.domain.Pojo.User;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Misaki
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {
    
    private final UserMapper userMapper;
    private final MenuMapper menuMapper;
    private final DepartmentMapper departmentMapper;
    private final DepartmentServiceImpl departmentServiceImpl;
    private final StructureService structureService;
    private final PasswordEncoder passwordEncoder;
    private final RoleServiceImpl roleServiceImpl;

    public UserServiceImpl(UserMapper userMapper, MenuMapper menuMapper, DepartmentMapper departmentMapper, DepartmentServiceImpl departmentServiceImpl, StructureService structureService, PasswordEncoder passwordEncoder, RoleServiceImpl roleServiceImpl) {
        this.userMapper = userMapper;
        this.menuMapper = menuMapper;
        this.departmentMapper = departmentMapper;
        this.departmentServiceImpl = departmentServiceImpl;
        this.structureService = structureService;
        this.passwordEncoder = passwordEncoder;
        this.roleServiceImpl = roleServiceImpl;
    }


    @Override
    @Transactional
    public boolean registerUser(UserRegisterDto userRegisterDto) {
        User user = new User();
        //默认名字为用户的用户名
        user.setNickName(userRegisterDto.getUsername());

        user.setUsername(userRegisterDto.getUsername());

        //密码进行加密后再存入数据库
        user.setPwd(passwordEncoder.encode(userRegisterDto.getPassword()));

        //新用户随机生成一个公司
        //方案1：获取原有的公司id的最大值，在其之上+1
        int tenementId = userMapper.selectMaxTenementId()+1;
        //方案2：uuid（string）
        //方案3：雪花算法生成id（Long类型）

        user.setTenementId(tenementId);

        //默认到岗状态
        user.setState(UserStateEnum.READY.ordinal());
        user.setIsForbidden(false);

        user.setPhone(null);
        user.setEmail(null);

        int i = userMapper.insert(user);

        //给用户添加默认的菜单
        Menu menu = new Menu();
        menu.setTenementId(tenementId);
        String[] menuNames = new String[]{"经营分析报表","设备点检巡检","设备维修保修","设备维护保养","备品备件管理","基础信息"};
        int menuId = menuMapper.selectIdMax()+1;
        for (int j = 0;j<menuNames.length;j++){
            menu.setId(menuId++);
            menu.setName(menuNames[j]);
            menuMapper.insert(menu);
        }


        //菜单初始化
        //根据tenementId 和 菜单名字 获取菜单Id,再根据菜单id创建表单
        menuId = menuMapper.selectIdByTenementIdAndName(tenementId,"设备点检巡检");
        String formFields = "[{\"fieldsId\":[],\"name\":\"root\"},{\"fieldsId\":[\"3_3cHzCxWF5wt_cMUFWJp\",\"20_8FttfwpqtJBBAR2A5sk\"],\"name\":\"巡检信息\"},{\"fieldsId\":[\"15_QOuGZdBEihJLyA9LDAZ\",\"4_TOkQ1lPQNl1PbSnr7si\",\"6_rcWxLdL0N-FmMmM3S7t\",\"1_uQHXTkvZb4E6c9EQWuF\"],\"name\":\"巡检记录\"},{\"fieldsId\":[],\"name\":\"报修维修\"},{\"fieldsId\":[\"15_DGh3lBLuRt7CaYjFRtM\"],\"name\":\"设备信息\"}]";
        structureService.createForm(menuId,1,"设备巡检单",tenementId,formFields);
        formFields = "[{\"fieldsId\":[\"0_7cBVlrH4x_GNNgeG7FM\",\"6_AVOj1krOqO_NIF9xAAh\",\"1_lgV7QmM30NV-lO-tFDW\"],\"name\":\"root\"}]";
        structureService.createForm(menuId,0,"巡检方案",tenementId,formFields);

        menuId = menuMapper.selectIdByTenementIdAndName(tenementId,"设备维修保修");
        formFields = "[{\"fieldsId\":[],\"name\":\"root\"},{\"fieldsId\":[\"0_knVRqfPqx_0ksEKOGwJ\",\"15_uvpqMJpuWbkM55tPAWT\",\"3_Hf4chVvg77_53hp-13h\",\"1_QeziDvhgHM-hTLh0JdX\",\"20_L21ZmsbKVRStzgWyf2W\"],\"name\":\"故障维修\"},{\"fieldsId\":[\"3_FuDmP4fn5KLVb9ot81P\",\"4_C24fkBPtvsJqGOAd9CK\",\"6_YK_mlIZpkFoNJuSRbQy\",\"6_ArYfWSzTiMw8zQhC1_q\",\"20_deqVY_e73DQOPU7hf6z\"],\"name\":\"维修派工\"},{\"fieldsId\":[\"3_XgbYwb-r-F48VbGlihU\",\"6_8P5wVRY0nNSC7xb3ms7\",\"4_em5Jjr4bfCCwB5mRREP\",\"1_244MsKvy3fZgNue0gTM\"],\"name\":\"维修结果\"},{\"fieldsId\":[\"15_pE6eRVfd9RkdQKI7CQ0\",\"2_8OjT5wIYzHsCgzQMBEk\"],\"name\":\"备件更换\"}]";
        structureService.createForm(menuId,1,"设备报修单",tenementId,formFields);

        menuId = menuMapper.selectIdByTenementIdAndName(tenementId,"设备维护保养");
        formFields = "[{\"fieldsId\":[],\"name\":\"root\"},{\"fieldsId\":[\"169\",\"170\"],\"name\":\"设备信息\"},{\"fieldsId\":[\"171\",\"172\",\"216\",\"174\",\"175\",\"176\",\"177\",\"178\",\"179\"],\"name\":\"保养内容\"}]";
        structureService.createForm(menuId,0,"设备保养单",tenementId,formFields);
        formFields = "[{\"fieldsId\":[],\"name\":\"root\"},{\"fieldsId\":[\"15_MkZBwKSDpeZ2ty59P9b\"],\"name\":\"设备信息\"},{\"fieldsId\":[\"0_VLkVu3s5XjobgAhPvUm\",\"20__RfrtpD0tBGwmlwgzs1\",\"6_VeeVz4EmHPoHx789Wv6\",\"6_UnEv6tkVaoFjImtP0PL\",\"1_GGNaeohj57P6cGcPOcP\"],\"name\":\"保养信息\"}]";
        structureService.createForm(menuId,0,"保养计划基础表",tenementId,formFields);

        menuId = menuMapper.selectIdByTenementIdAndName(tenementId,"备品备件管理");
        formFields = "[{\"fieldsId\":[\"0_BiGk_HSCHc_t1eUrS6t\",\"3_DR4h8HkDsXgLI2XwgHM\",\"6_YT2_60Mo7eGCOot45Ea\",\"20_kagPwvR0F-1jwmw2O7s\",\"14_rMkwHL4speD77FyQz0G\",\"0_2-trjsdxsXKS63yBadL\"],\"name\":\"root\"}]";
        structureService.createForm(menuId,0,"备件入库单",tenementId,formFields);
        formFields = "[{\"fieldsId\":[\"0_BcqxOR8lQFMMVuzEKvt\",\"3_gnADXOPGLRq5tmjCcEd\",\"20_OAlsAro26h4POvhOrtI\",\"4_kv8wYfSfBQv-n4cTERc\",\"15_9MrRr5ynHpl201wKwlZ\",\"2_0X3t96TF-SwNKnnuVTQ\"],\"name\":\"root\"}]";
        structureService.createForm(menuId,0,"备件领用单",tenementId,formFields);
        formFields = "[{\"fieldsId\":[\"0_KY5IggHq95W8p7psNJs\",\"0_82b2mWbsK2QaE-JF2bz\",\"0_TSwolwUqsaAurH47KQA\",\"6_KO3_hf3R3Lpxeumm9U8\",\"0_Zcgb4IX2oRRvmjQtKfD\",\"1_4OaInSmyL61wlr-SRFy\"],\"name\":\"root\"}]";
        structureService.createForm(menuId,0,"备件台账",tenementId,formFields);

        menuId = menuMapper.selectIdByTenementIdAndName(tenementId,"基础信息");
        formFields = "[{\"fieldsId\":[\"0_ssFvScH60A9bAMm63bq\",\"5_aOjH5DKjedeGn6v0u1n\",\"3_TWsPiynTYF1i1aRSkl2\",\"7_g-mZsudiKhfzgbARzQY\",\"14_1NOWE2bAk_CKB9S0MGd\",\"9_jHmMJqicv92BSuSZbey\",\"20_LmU7st-RZOZk024brHS\"],\"name\":\"root\"}]";
        structureService.createForm(menuId,0,"机房",tenementId,formFields);
        formFields = "[{\"fieldsId\":[\"0_3yRQTs7Rz4HzwTVnefs\",\"15_LqsCb530zvfX0sUANom\"],\"name\":\"root\"}]";
        structureService.createForm(menuId,0,"设备类型",tenementId,formFields);
        formFields = "[{\"fieldsId\":[\"0_Y1qWyvk3OndKnMIr0F0\"],\"name\":\"root\"}]";
        structureService.createForm(menuId,0,"部门",tenementId,formFields);
        formFields = "[{\"fieldsId\":[\"0_e8g1sXktJ0n_apPC2iI\"],\"name\":\"root\"}]";
        structureService.createForm(menuId,0,"设备状态",tenementId,formFields);
        formFields = "[{\"fieldsId\":[\"0_34bmUZYNojZLihmLIS8\",\"0_8ig-mTOFdFaLZIi-j9o\",\"0_f_cawzYCD389Zs_HNFi\"],\"name\":\"root\"}]";
        structureService.createForm(menuId,0,"保养等级与频次",tenementId,formFields);
        formFields = "[{\"fieldsId\":[\"0_f_lUtGLU_LxkL9ZHgPE\"],\"name\":\"root\"}]";
        structureService.createForm(menuId,0,"仓库",tenementId,formFields);
        formFields = "[{\"fieldsId\":[\"0_FnSoRRsbbPuZqG65FwN\"],\"name\":\"root\"}]";
        structureService.createForm(menuId,0,"单位",tenementId,formFields);
        formFields = "[{\"fieldsId\":[\"0_QaIwHLD_K4Wu8LqLryf\"],\"name\":\"root\"}]";
        structureService.createForm(menuId,0,"安装地点",tenementId,formFields);
        formFields = "[{\"fieldsId\":[\"0_JzF73gi4DkpF7Ul5BC_\",\"0_ZWJMtVO2hb_IP3rTnIq\",\"0_nnmrAsbtrUe_Ez7EaI4\",\"0_36OyairN0QUuP4MHBC0\",\"6_kddKuYbTbtNN19y9jGk\",\"6_PmlO3p2IdPFNH1xP__R\",\"6_mOjct_7x1G8NJ_heFdw\",\"6_EqGFRm7G5bjros5w7NE\",\"3_cas5Y2h44-KaHcwKMmY\",\"3_TIjomgdxy5E-qEf8L3d\"],\"name\":\"root\"}]";
        structureService.createForm(menuId,0,"设备信息",tenementId,formFields);


        //给用户创建默认的公司
        Department department = new Department();
        department.setTenementId(tenementId);
        department.setName(userRegisterDto.getUsername()+"的公司");
        department.setPreId(-1);

        departmentMapper.insert(department);

        //将用户添加到默认的公司下
        int userId = userMapper.selectIdByUsername(userRegisterDto.getUsername(),tenementId);
        int departmentId = departmentMapper.selectIdByName(userRegisterDto.getUsername()+"的公司");
        departmentMapper.insertIntoUserDepartment(departmentId,userId,tenementId);


        return i>0;
    }

    @Override
    @Transactional
    public Integer addUser(UserDto userDto) {
        if(checkUserNameIsUsed(userDto.getUserName())) return -1;
        User user = new User();
        user.setEmail(userDto.getEmail());
        
        user.setNickName(userDto.getName());
        user.setPhone(userDto.getPhone());
        user.setUsername(userDto.getUserName());
        user.setTenementId(SecurityUtils.getCurrentUser().getTenementId());
        
        //插入到数据库中，默认状态是未到岗
        user.setState(UserStateEnum.NO_ARRIVE.ordinal());
        user.setIsForbidden(true);
        
        int i = userMapper.insert(user);
        if(userDto.getDepartmentIds()!=null){
            userDto.getDepartmentIds().forEach(d->{
                departmentServiceImpl.insertOneUserToDepart(user.getId(),d);
            });
        }
        return user.getId();
    }
    
    public boolean checkUserNameIsUsed(String userName){
        return userMapper.exists(new QueryWrapper<User>().eq("username",userName));
    }
    
    

    @Override
    public boolean changeUserInfo(UserDto.ChangeUserInfoDto changeUserInfoDto) {
        //拿到用户原来的信息
        User user = userMapper.selectById(changeUserInfoDto.getUserId());
        List<Integer> originRoleIds = roleServiceImpl.getUserRoleIds(changeUserInfoDto.getUserId());
        //如果前端传入的数据跟原来的不一样就更新为新数据
        if(!changeUserInfoDto.getRoleIds().equals(originRoleIds)){
            roleServiceImpl.changeRoleIdForUser(changeUserInfoDto.getRoleIds(),originRoleIds, changeUserInfoDto.getUserId());
        }
        if(!changeUserInfoDto.getState().equals(user.getState())){
            user.setState(changeUserInfoDto.getState());
        }
        if(!changeUserInfoDto.getName().equals(user.getNickName())){
            user.setNickName(changeUserInfoDto.getName());
        }
        List<Integer> originDepartIds = departmentServiceImpl.getDepartIdsForUser(changeUserInfoDto.getUserId());
        if(!changeUserInfoDto.getDepartmentIds().equals(originDepartIds)){
            departmentServiceImpl.changeDepartIdsForUser(changeUserInfoDto.getDepartmentIds(),originDepartIds, changeUserInfoDto.getUserId());
        }
        int i = userMapper.updateById(user);
        return i>0;
    }

    @Override
    public boolean changeUserInfo(UserDto.InitialUserDto initialUserDto) {
        User user = userMapper.selectById(initialUserDto.getUserId());
        user.setPhone(null);
        user.setEmail(null);
        if(initialUserDto.getEmail()!=null||!initialUserDto.getEmail().equals("")) user.setEmail(initialUserDto.getEmail());
        user.setState(1);
        if(initialUserDto.getPhone()!=null || !initialUserDto.getPhone().equals("")) user.setPhone(initialUserDto.getPhone());
        user.setIsForbidden(false);
        user.setGender(initialUserDto.getGender());
        user.setPwd(passwordEncoder.encode(initialUserDto.getPwd()));
        int i = userMapper.updateById(user);
        return i>0;
    }

    @Override
    public boolean resignationUser(Integer userId) {
        int i = userMapper.update(null, new UpdateWrapper<User>().eq("id", userId).eq("tenement_id", SecurityUtils.getCurrentUser().getTenementId())
                .set("state", 2));
        return i>0;
    }

    @Override
    public boolean deleteUser(Integer userId) {
        int i = userMapper.update(null, new UpdateWrapper<User>().eq("id", userId).eq("tenement_id", SecurityUtils.getCurrentUser().getTenementId())
                .set("is_delete", true));
        return i>0;
    }

    @Override
    public List<UserVo> getAllUser() {
        List<User> users = userMapper.selectList(new UpdateWrapper<User>().eq("tenement_id", SecurityUtils.getCurrentUser().getTenementId()).eq("is_delete", false));
        List<UserVo> userVos = new ArrayList<>(32);
        users.forEach(u->{
            UserVo userVo = new UserVo();
            userVo.setUserId(u.getId());
            userVo.setName(u.getNickName());
            userVo.setPhone(u.getPhone());
            userVo.setEmail(u.getEmail());
            userVo.setState(u.getState());
            userVo.setRole(roleServiceImpl.getUserRoleMap(u.getId()));
            userVos.add(userVo);
        });
        return userVos;
    }

    @Override
    public List<UserVo> searchUser(String userInfo) {
        List<User> users = userMapper.selectList(new QueryWrapper<User>().like("username", userInfo).or().like("nick_name", userInfo).or().like("email", userInfo).or().like("phone", userInfo));
        List<UserVo> userVos = new ArrayList<>(32);
        users.forEach(u->{
            UserVo userVo = new UserVo();
            userVo.setUserId(u.getId());
            userVo.setName(u.getNickName());
            userVo.setPhone(u.getPhone());
            userVo.setEmail(u.getEmail());
            userVo.setRole(roleServiceImpl.getUserRoleMap(u.getId()));
            userVo.setState(u.getState());
            userVos.add(userVo);
        });
        return userVos;
    }

    @Override
    public List<UserVo> searchUser(String userInfo, Integer departmentId) {
        List<UserVo> userVos = searchUser(userInfo);
        Set<Integer> userIds = new HashSet<>(departmentServiceImpl.getUserIdsForDepart(departmentId));
        return userVos.stream()
                      .filter(userVo -> userIds.contains(userVo.getUserId()))
                      .collect(Collectors.toList());
    }

    
    @Override
    public void setUserForDepartOwn(Integer userId, Integer departmentId) {
        departmentServiceImpl.setOwnForOneDepartment(userId,departmentId);
    }

    @Override
    public UserVo.SingleUserVo getOneUserDetail(Integer userId) {
        User user = userMapper.selectById(userId);
        //获取到的用户信息包含密码等敏感信息不能直接返回，只把用到的用户信息返回
        UserVo.SingleUserVo userVo = new UserVo.SingleUserVo();
        userVo.setUserName(user.getUsername());
        userVo.setName(user.getNickName());
        userVo.setEmail(user.getEmail());
        userVo.setPhone(user.getPhone());
        userVo.setDepartments(departmentServiceImpl.getDepartMapForUser(userId));
        userVo.setRoles(roleServiceImpl.getUserRoleMap(userId));
        return userVo;
    }

    @Override
    public Set<Integer> getUserIdsFromHead(Head head) {
        Set<Integer> ans = new HashSet<>(Arrays.asList(head.getUser()));
        Integer[] department = head.getDepartment();
        //获取每个部门中的用户id
        for (Integer departmentId : department) {
            ans.addAll(departmentServiceImpl.getUserIdsForDepart(departmentId));
        }
        Integer[] role = head.getRole();
        //获取每个角色对应的用户id
        for (Integer roleId : role) {
            ans.addAll(roleServiceImpl.getUserIdsForRole(roleId));
        }
        return ans;
    }
}
