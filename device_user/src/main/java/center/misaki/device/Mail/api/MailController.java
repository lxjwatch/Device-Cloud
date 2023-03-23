package center.misaki.device.Mail.api;

import center.misaki.device.AddressBook.dao.UserMapper;
import center.misaki.device.Auth.SecurityUtils;
import center.misaki.device.Mail.Dto.CommentDto;
import center.misaki.device.Mail.Dto.DataCommentDto;
import center.misaki.device.Mail.Dto.FlowMailDto;
import center.misaki.device.Mail.MailServiceImpl;
import center.misaki.device.Mail.api.feign.DataCommentController;
import center.misaki.device.base.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Misaki
 */
@RestController
@RequestMapping("/mail")
public class MailController {
    
    private final MailServiceImpl mailService;
    private final UserMapper userMapper;
    private final DataCommentController dataCommentController;
    
    public MailController(MailServiceImpl mailService, UserMapper userMapper, DataCommentController dataCommentController) {
        this.mailService = mailService;
        this.userMapper = userMapper;

        this.dataCommentController = dataCommentController;
    }



    //发表评论接口
    @PostMapping("/comment")
    public Result<?> sendComment(@RequestBody CommentDto commentDto){
        Integer[] userIds = commentDto.getUserIds();
        Map<String, Object> args = new HashMap<>();
        args.put("sendUserName", SecurityUtils.getCurrentUsername());
        args.put("formName",commentDto.getFormName());
        args.put("url",commentDto.getUrl());
        args.put("time", LocalDateTime.now().toString());
        args.put("content",commentDto.getContent());
        for (Integer userId : userIds) {
            String email = userMapper.selectEmailById(userId);
            if(email==null||email.equals(""))continue;
            args.put("userName",userMapper.selectUsernameById(userId));
            args.put("userEmail",email);
            mailService.asyncSendTemplateMail(email,"设备运维系统",args,"invite.ftl");
        }
        DataCommentDto dataCommentDto = new DataCommentDto();
        dataCommentDto.setUserName(SecurityUtils.getCurrentUsername());
        dataCommentDto.setContent(commentDto.getContent());
        mailService.getExecutorService().execute(()->{
            dataCommentController.addComment(dataCommentDto,commentDto.getDataId());
        });
        return Result.ok(null,"评论成功");
    }
    
    //流程流转通知接口
    @PostMapping("/flow")
    public Result<?> flowAdvice(@RequestBody FlowMailDto flowMailDto){
        Integer userId = flowMailDto.getUserId();
        Map<String, Object> args = new HashMap<>();
        String userName = userMapper.selectUsernameById(userId);
        String email =userMapper.selectEmailById(userId);
        args.put("userName",userName);
        args.put("url","");
        args.put("formName",flowMailDto.getFormName());
        args.put("time",LocalDateTime.now().toString());
        args.put("word","待办");
        mailService.asyncSendTemplateMail(email,"待办通知",args,"flow.ftl");
        return Result.ok(null,"发送成功");
    }
    
    //流程抄送通知接口
    @PostMapping("/flow/copy")
    public Result<?> flowCopyAdvice(@RequestBody FlowMailDto flowMailDto){
        Integer userId = flowMailDto.getUserId();
        Map<String, Object> args = new HashMap<>();
        String userName = userMapper.selectUsernameById(userId);
        String email =userMapper.selectEmailById(userId);
        args.put("userName",userName);
        args.put("url","");
        args.put("formName",flowMailDto.getFormName());
        args.put("time",LocalDateTime.now().toString());
        args.put("word","抄送");
        mailService.asyncSendTemplateMail(email,"抄送通知",args,"flow.ftl");
        return Result.ok(null,"发送成功");
    }
    
    
    //流程结果通知接口
    @PostMapping("/flow/end")
    public Result<?> flowRejectAdvice(@RequestBody FlowMailDto flowMailDto){
        Integer userId = flowMailDto.getUserId();
        Map<String, Object> args = new HashMap<>();
        String userName = userMapper.selectUsernameById(userId);
        String email =userMapper.selectEmailById(userId);
        args.put("userName",userName);
        args.put("url","");
        args.put("formName",flowMailDto.getFormName());
        args.put("time",LocalDateTime.now().toString());
        args.put("key",flowMailDto.getIsAgree()?"已通过":"未能通过");
        mailService.asyncSendTemplateMail(email,"审核结果通知",args,"flowEnd.ftl");
        return Result.ok(null,"发送成功");
    }
    
    
    
}
