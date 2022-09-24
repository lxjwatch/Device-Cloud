package center.misaki.device.Flow.api;

import center.misaki.device.Enum.WorkLogEnum;
import center.misaki.device.Flow.WorkLogVo;
import center.misaki.device.Flow.service.WorkLogService;
import center.misaki.device.base.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Misaki
 */
@RestController
@RequestMapping("/work")
public class WorkLogController {
    
    private final WorkLogService workLogService;

    public WorkLogController(WorkLogService workLogService) {
        this.workLogService = workLogService;
    }

    //查看与我相关的待办日志
    @GetMapping("/queryWorkLog/wait")
    public Result<List<WorkLogVo>> queryWorkLogWait(String userInfo){
        List<WorkLogVo> workLogVos = workLogService.getWorkLogVos(WorkLogEnum.WAIT,userInfo);
        return Result.ok(workLogVos,"获取成功");
    }
    
    //查看与我相关的我发起的日志
    @GetMapping("/queryWorkLog/launch")
    public Result<List<WorkLogVo>> queryWorkLogLunch(String userInfo){
        List<WorkLogVo> workLogVos = workLogService.getWorkLogVos(WorkLogEnum.ORIGIN,userInfo);
        return Result.ok(workLogVos,"获取成功");
    }
    
    //查看与我相关的我处理的日志
    @GetMapping("/queryWorkLog/handle")
    public Result<List<WorkLogVo>> queryWorkLogHandle(String userInfo){
        List<WorkLogVo> workLogVos = workLogService.getWorkLogVos(WorkLogEnum.SOLVED,userInfo);
        return Result.ok(workLogVos,"获取成功");
    }
    
    //查看与我相关的抄送我的日志
    @GetMapping("/queryWorkLog/copy")
    public Result<List<WorkLogVo>> queryWorkLogCopy(String userInfo){
        List<WorkLogVo> workLogVos = workLogService.getWorkLogVos(WorkLogEnum.COPY,userInfo);
        return Result.ok(workLogVos,"获取成功");
    }
    
}
