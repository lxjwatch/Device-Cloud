package center.misaki.device.AddressBook.api;
import center.misaki.device.AddressBook.AuthScope;
import center.misaki.device.AddressBook.dto.Head;
import center.misaki.device.AddressBook.dto.UserDto;
import center.misaki.device.AddressBook.service.UserService;
import center.misaki.device.AddressBook.vo.UserVo;
import center.misaki.device.Annotation.AuthOnCondition;
import center.misaki.device.Auth.SecurityUtils;
import center.misaki.device.Mail.MailService;
import center.misaki.device.base.Result;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author Misaki
 */
@RestController
@RequestMapping("/user")
public class UserController {
    
    private final UserService userService;
    private final MailService mailService;
    
    public UserController(UserService userService, MailService mailService) {
        this.userService = userService;
        this.mailService = mailService;
    }
    
    public static final String INVITE_URL="";

    //保留，用户邀请接口，待到邮件消息推送功能写完再写
    @PostMapping("/invite")
    @AuthOnCondition
    @Transactional
    public Result<?> invite(@Valid @RequestBody UserDto userDto) {
        Integer userId = userService.addUser(userDto);
        if(userId==-1) return Result.error("用户名已被占用");
        //发送邮件
        Map<String, Object> args = new HashMap<>();
        args.put("name", userDto.getName());
        args.put("url", INVITE_URL + "/user/register?userName=" + userDto.getUserName()+"&userId="+userId);
        mailService.sycSendTemplateMail(userDto.getEmail(), "邀请注册", args, "invite.ftl");
        return Result.ok(null, "邀请成功");
    }

    //获取本人信息接口
    @GetMapping("/showSelf")
    public Result<UserVo.SingleUserVo>showUserSelf(){
        UserVo.SingleUserVo userVo = userService.getOneUserDetail(SecurityUtils.getCurrentUser().getUserId());
        return Result.ok(userVo,"获取成功");
    }
    
    //查看所有用户接口
    @GetMapping("/show")
//    @AuthOnCondition(NeedSysAdmin = false)
    public Result<List<UserVo>> showAllUser(){
        List<UserVo> allUser = userService.getAllUser();
        return Result.ok(allUser,"获取成功");
    }
    
    //获取单独用户的详细信息
    @GetMapping ("/getOne")
//    @AuthOnCondition
    public Result<UserVo.SingleUserVo> showOne(@Valid @NotNull Integer userId){
        UserVo.SingleUserVo userVo = userService.getOneUserDetail(userId);
        return Result.ok(userVo,"获取成功");
    }
    
    //在全局搜索用户
    @PostMapping("/searchAll")
//    @AuthOnCondition
    public Result<List<UserVo>> searchAllUser(@Valid @NotNull String userInfo){
        List<UserVo> userVos = userService.searchUser(userInfo);
        return Result.ok(userVos,"获取成功");
    }
    
    //在部门下搜索用户
    @PostMapping("/searchDepart")
//    @AuthOnCondition(NeedSysAdmin = false)
    @AuthScope(department = true)
    public Result<List<UserVo>> searchDepartUser(@Valid @NotNull String userInfo, @Valid @NotNull Integer departmentId){
        List<UserVo> userVos = userService.searchUser(userInfo, departmentId);
        return Result.ok(userVos,"获取成功");
    }
    
    //更改用户信息
    @PostMapping("/changeUserInfo")
//    @AuthOnCondition
    public Result<?> changeUserInfo(@Valid @RequestBody UserDto.ChangeUserInfoDto changeUserInfoDto){
        if(userService.changeUserInfo(changeUserInfoDto)){
            return Result.ok(null,"修改成功");
        }else return Result.error("修改失败");
    }
    
    //用户接受请求后初始化用户信息
    @PostMapping("/initUserInfo")
    public Result<?> InitUserInfo(@Valid @RequestBody UserDto.InitialUserDto initialUserDto){
        if(userService.changeUserInfo(initialUserDto)){
            return Result.ok(null,"初始化成功");
        }else return Result.error("初始化失败");
    }
    
    
    //从Head中获取用户ID集合
    @PostMapping("/getHeadUserIds")
    public Result<Set<Integer>> getUserIdsFromHead(@RequestBody Head head){
        Set<Integer> userIds = userService.getUserIdsFromHead(head);
        return Result.ok(userIds,"获取成功");
    }
    
    //用户创建企业接口，同时附带有大量的模板
    @PostMapping("/createSystem")
    public Result<?> createSystem(){
        return null;
    }

    //用户注册接口
    @PostMapping("/registerUser")
    public Result<UserVo.registerUserVo> registerUser(@Valid @RequestBody UserDto.RegisterUserDto registerUserDto) {
        UserVo.registerUserVo registerUserVo = userService.registerUser(registerUserDto);
        if (registerUserVo != null){
            return Result.ok(registerUserVo,"注册成功");
        }else return Result.error("注册失败，用户名已存在");
    }

    //员工注册接口
    @PostMapping("/registerEmployee")
    public Result<?> registerEmployee(@Valid @RequestBody UserDto.RegisterEmployeeDto registerEmployeeDto) {
        int result = userService.registerEmployee(registerEmployeeDto);
        if (result == 0) {
            return Result.ok(null, "注册成功");
        } else {
            return Result.error(result == 1 ? "注册失败，用户名已存在" : "注册失败，加入的公司不存在");
        }
    }
}
