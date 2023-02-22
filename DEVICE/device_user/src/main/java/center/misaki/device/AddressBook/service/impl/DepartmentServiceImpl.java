package center.misaki.device.AddressBook.service.impl;

import center.misaki.device.AddressBook.dao.DepartmentMapper;
import center.misaki.device.AddressBook.dao.UserMapper;
import center.misaki.device.AddressBook.dto.DepartmentDto;
import center.misaki.device.AddressBook.pojo.Department;
import center.misaki.device.AddressBook.service.DepartmentService;
import center.misaki.device.AddressBook.vo.DepartmentVo;
import center.misaki.device.AddressBook.vo.UserVo;
import center.misaki.device.Auth.SecurityUtils;
import center.misaki.device.domain.Pojo.User;
import cn.hutool.core.lang.tree.Tree;
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
 * 部门业务实现类
 */
@Service
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;
    private final RoleServiceImpl roleService;
    

    public DepartmentServiceImpl(DepartmentMapper departmentMapper, UserMapper userMapper, RoleServiceImpl roleService) {
        this.departmentMapper = departmentMapper;
        this.userMapper = userMapper;
        this.roleService = roleService;
    }
    
    //获得某个用户所在部门ID集合
    public List<Integer> getDepartIdsForUser(Integer userId){
        List<Map<String,Object>> departIdNames = departmentMapper.selectUserDepartIds(userId, SecurityUtils.getCurrentUser().getTenementId());
        List<Integer> departmentId = departIdNames.stream().map(m -> ((Long) m.get("departmentId")).intValue()).collect(Collectors.toList());
        return departmentId;
    }
    
    //获得某个用户所在部门 ID，Name 的 Map
    public Map<Integer,String> getDepartMapForUser(Integer userId){
        List<Map<String, Object>> departIdNames = departmentMapper.selectUserDepartIds(userId, SecurityUtils.getCurrentUser().getTenementId());
//        System.out.println("-------------departIdNames----------:    " + departIdNames);
        Map<Integer, String> ans = new HashMap<>();
        departIdNames.forEach(d->{
            ans.put((((Long) d.get("departmentId")).intValue()),(String) d.get("name"));
        });
        return ans;
    }
    
    //获取这个部门中所有的用户ID
    public List<Integer> getUserIdsForDepart(Integer departmentId){
       return departmentMapper.selectDepartUserIds(departmentId,SecurityUtils.getCurrentUser().getTenementId());
    }
    

    //考虑重复将一名用户添加到某个部门
    @Async
    @Transactional
    public void addOneUserToDepart(Integer userId,Integer departmentId){
        if(departmentMapper.existUserInDepart(departmentId,userId)){
            log.info("租户ID为 {} 用户Id为 {} 的用户已经存在与 部门ID为 {} 的部门",SecurityUtils.getCurrentUser().getTenementId(),userId,departmentId);
        }
        int i = departmentMapper.insertIntoUserDepartment(departmentId, userId, SecurityUtils.getCurrentUser().getTenementId());
        log.info("添加用户id为 {} 的用户至 部门id为 {} 的部门 ：",userId,departmentId+i>0?"成功":"失败");
    }
    
    //不考虑重复将一名用户添加到某个部门
    @Async
    @Transactional
    public void  insertOneUserToDepart(Integer userId,Integer departmentId){
        int i = departmentMapper.insertIntoUserDepartment(departmentId, userId, SecurityUtils.getCurrentUser().getTenementId());
        log.info("添加用户id为 {} 的用户至 部门id为 {} 的部门 ：",userId,departmentId+i>0?"成功":"失败");
    }
    
    //修改一个用户的加入的部门ID
    @Async
    @Transactional
    public void changeDepartIdsForUser(List<Integer> changeDepartmentIds,List<Integer> originDepartmentIds,Integer userId){
        Set<Integer> originIds = new HashSet<>(originDepartmentIds);
        changeDepartmentIds.forEach(c->{
            if(originIds.contains(c)) originIds.remove(c);
            else{
                insertOneUserToDepart(userId,c);
            }
        });
        originIds.forEach(o->{deleteOneUserToDepart(userId,o);});
    }
    
    @Async
    @Transactional
    public void deleteOneUserToDepart(Integer userId,Integer departmentId){
        int i = departmentMapper.deleteOneUserDepartment(userId,departmentId,SecurityUtils.getCurrentUser().getTenementId());
        log.info("删除用户id为 {} 的用户从 部门id为 {} 的部门 ：",userId,departmentId+i>0?"成功":"失败");
    }
    
    
    //设置某个用户为部门主管
    @Async
    @Transactional
    public void setOwnForOneDepartment(Integer userId,Integer departmentId){
        int i = departmentMapper.update(null, new UpdateWrapper<Department>().eq("id", departmentId).set("own_id", userId));
        log.info(i>0?"成功":"失败"+"设置用户ID为 {} 的用户为部门ID为 {} 主管",userId,departmentId);
    }

    
    

    @Override
    @Transactional
    public boolean addOneDepart(DepartmentDto departmentDto) {
        Department department = new Department();
        department.setName(departmentDto.getName());
        department.setTenementId(SecurityUtils.getCurrentUser().getTenementId());
        department.setPreId(departmentDto.getPreId());
        int i = departmentMapper.insert(department);
        return i>0;
    }

    @Override
    @Transactional
    public boolean changeDepartmentName(DepartmentDto departmentDto) {
        int i = departmentMapper.update(null, new UpdateWrapper<Department>().eq("id", departmentDto.getDepartmentId()).eq("tenement_id", SecurityUtils.getCurrentUser().getTenementId())
                .set("name", departmentDto.getName()));
        return i>0;
    }

    @Override
    @Transactional
    public boolean changePreDepart(DepartmentDto departmentDto) {
        //待优化，对该部门的子部门进行遍历，如果该部门的pre_id为其子部门的id时，会形成回路导致死循环
        int i = departmentMapper.update(null, new UpdateWrapper<Department>().eq("id", departmentDto.getDepartmentId()).eq("tenement_id", SecurityUtils.getCurrentUser().getTenementId())
                .set("pre_id", departmentDto.getPreId()));
        return i>0;
    }

    @Override
    @Transactional
    public boolean deleteDepartment(Integer Id) {
        /**
         * 此处有优化空间，可以把ids判空条件放在子部门的查询之前，
         * 这样就可以优化有成员时直接返回结果，不再从库查询子部门，
         * ids不为空时再进一步判断子部门的情况，可优化一种情况时的时间
         */
        List<Integer> ids = getUserIdsForDepart(Id);
        List<Map<String, Object>> list = departmentMapper.selectSubDepartIds(Id, SecurityUtils.getCurrentUser().getTenementId());
        if(!ids.isEmpty()||!list.isEmpty()){
            log.info("无法删除还有成员,或者子级部门的部门");
            return false;
        }
        int i = departmentMapper.deleteById(Id);
        return i>0;
    }

    @Override
    public DepartmentVo getAllDepartments() {
        //获取所有部门
        List<Department> departments = departmentMapper.selectList(new QueryWrapper<Department>().eq("tenement_id", SecurityUtils.getCurrentUser().getTenementId()));
        Map<Integer, List<Department>> map = new HashMap<>();
        departments.forEach(d->{
            //如果map没有当前部门的上一级部门，就创建一个初始部门，departmentId为-1
            if(!map.containsKey(d.getPreId())) map.put(d.getPreId(),new ArrayList<>());
            //map有当前部门的上一级部门，就将当前部门添加在其部门下
            map.get(d.getPreId()).add(d);
        });
        //初始部门作为根节点
        DepartmentVo root = new DepartmentVo(map.get(-1).get(0));
        DepartmentVo ans = root;
        map.remove(-1);
        ArrayDeque<DepartmentVo> queue = new ArrayDeque<>();
        while(!map.isEmpty()){
            //获取当前部门的子部门
            List<Department> nodes = map.get(root.getId());
            if(nodes!=null&&!nodes.isEmpty()) {//子部门存在时
                //将当前部门移出
                map.remove(root.getId());
                List<DepartmentVo> vos = nodes.stream()
                                              //将每个当前部门的每个子部门都映射为一个DepartmentVo对象
                                              .map(DepartmentVo::new)
                                              .collect(Collectors.toList());
                root.setNodes(vos);//将子部门与当前部门绑定
                queue.addAll(vos);//将子部门全部添加到队列中（从队尾添加元素）
            }
            root=queue.poll();//从队列中取出一个子部门设为当前部门继续遍历其子部门（从队头取出元素）
            if(root==null) break;//所有部门遍历完后结束循环
        }
        return ans;
    }

    @Override
    public List<UserVo> getUserOnDepart(Integer departmentId) {
        List<Integer> userIds = getUserIdsForDepart(departmentId);
        if(userIds==null||userIds.isEmpty()) return new ArrayList<>(0);
        List<User> users = userMapper.selectBatchIds(userIds);
        List<UserVo> userVos = new ArrayList<>(32);
        users.forEach(u->{
            UserVo userVo = new UserVo();
            userVo.setUserId(u.getId());
            userVo.setName(u.getNickName());
            userVo.setPhone(u.getPhone());
            userVo.setEmail(u.getEmail());
            userVo.setRole(roleService.getUserRoleMap(u.getId()));
            userVos.add(userVo);
        });
        return userVos;
    }


}
