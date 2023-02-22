package center.misaki.device.Form.api;

import center.misaki.device.Annotation.FormAuthCondition;
import center.misaki.device.Form.pojo.FormData;
import center.misaki.device.base.Result;
import center.misaki.device.Form.dto.BatchChangeDto;
import center.misaki.device.Form.dto.BatchDeleteDto;
import center.misaki.device.Form.dto.OneDataDto;
import center.misaki.device.Form.service.FormService;
import center.misaki.device.Form.vo.BatchLogVo;
import center.misaki.device.Form.vo.FormDataVo;
import center.misaki.device.Form.vo.OneDataVo;
import center.misaki.device.utils.StringZipUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.sql.SQLException;
import java.util.List;

/**
 * @author Misaki
 * 数据访问接口
 */
@RestController//@RestController == @Controller + @ResponseBody
@Slf4j
public class DataAccessController {
    
    private final FormService formService;

    public DataAccessController(FormService formService) {
        this.formService = formService;
    }
    
    //展示一张表单所有的数据，不管其是否已经被删除或者是流程中间数据。
    @GetMapping("/queryOneFormAll")
    public Result<FormDataVo> queryOneFormAll(@Valid @NotNull Integer formId, String userInfo){
        FormDataVo oneFormAllData = formService.getOneFormAllData(formId,userInfo);
        return Result.ok(oneFormAllData,"获取成功");
    }
    
    //展示一张表单中，理应正常显示的全部数据，排除被删除和流程中间数据
    @GetMapping("/queryOneForm")
    @FormAuthCondition(NeedWatch = true,NeedManage = true)
    public Result<FormDataVo> queryOneForm(@Valid @NotNull Integer formId,String userInfo){
        FormDataVo oneFormData = formService.getOneFormData(formId,userInfo);
        return Result.ok(oneFormData,"获取成功");
    }
    
    //仅仅展示一张表单中本人提交的数据
    @GetMapping("/queryUserOneForm")
    @FormAuthCondition(NeedSubmitSelf = true,NeedWatch = true,NeedManage = true)
    public Result<FormDataVo> queryUserOneForm(@Valid @NotNull Integer formId,String userInfo){
        FormDataVo oneUserFormData = formService.getOneUserFormData(formId, userInfo);
        return Result.ok(oneUserFormData,"获取成功");
    }
    
    //查询一条数据的详细信息，附带数据日志
    @GetMapping("/queryOneData")
    public Result<OneDataVo> queryOneData(@Valid @NotNull Integer formId,@Valid @NotNull Integer dataId,String userInfo){
        OneDataVo oneData = formService.getOneData(formId, dataId, userInfo);
        return Result.ok(oneData,"获取成功");
    }
    
    //查询一张表的批量修改日志
    @GetMapping("/batchLog")
    public Result<List<BatchLogVo>> queryFormDataBatchLog(@Valid @NotNull Integer formId,String userInfo){
        List<BatchLogVo> logs = formService.getBatchLogs(formId, userInfo);
        return Result.ok(logs,"获取成功");
    }
    
    //普通提交数据
    @PostMapping("/submit")
    @FormAuthCondition(NeedSubmit = true,NeedSubmitSelf = true,NeedManage = true)
    public Result<?> submit(@Valid @RequestBody OneDataDto dataDto,String userInfo){
        if(formService.addOneData(dataDto,userInfo)){
            return Result.ok(null,"添加成功");   
        }
        return Result.error("添加失败");
    }
    
    //修改一条数据
    @PostMapping("/change")
    @FormAuthCondition(NeedSubmitSelf = true,NeedManage = true)
    public Result<?> change(@Valid @RequestBody OneDataDto dataDto,String userInfo){
        if(formService.changeOneData(dataDto,userInfo)){
            return Result.ok(null,"修改成功");
        }
        return Result.error("修改失败");
    }
    
    //批量修改一条数据
    @PostMapping("/batch/change")
    @FormAuthCondition(NeedSubmitSelf = true,NeedManage = true)
    public Result<?> batchChange(@Valid @RequestBody BatchChangeDto dataDto,String userInfo){
        formService.batchChangeData(dataDto,userInfo);
        return Result.ok(null,"修改成功");
    }
    
    //删除一条数据
    @PostMapping("/delete")
    @FormAuthCondition(NeedSubmitSelf = true,NeedManage = true)
    public Result<?> deleteOne(@Valid @NotNull Integer formId,@Valid @NotNull Integer dataId,String userInfo){
        try {
            formService.deleteOneData(dataId,formId,userInfo);
        } catch (SQLException e) {
            log.error("删除数据错误：参数：formId:{},dataId:{},userInfo:{}",formId,dataId,userInfo);
            return Result.error("删除错误");
        }
        return Result.ok(null,"删除成功");
    }
    
    
    //批量删除数据接口
    @PostMapping("/batch/delete")
    @FormAuthCondition(NeedSubmitSelf = true,NeedManage = true)
    public Result<?> batchDelete(@Valid @RequestBody BatchDeleteDto dataDto,String userInfo){
        formService.batchDelete(dataDto,userInfo);
        return Result.ok(null,"删除成功");
    }


    //快速查询数据接口
    @PostMapping("/FastQuery")
    public Result<List<FormData>> fastQuery(@RequestBody List<Integer> dataIds,String userInfo){
        List<FormData> formData = formService.fastQuery(dataIds);
        return Result.ok(formData,"查询成功");
    }
    
    
    
    
    
}
