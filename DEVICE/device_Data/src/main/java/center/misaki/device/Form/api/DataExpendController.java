package center.misaki.device.Form.api;

import center.misaki.device.Annotation.FormAuthCondition;
import center.misaki.device.Form.dto.DataCommentDto;
import center.misaki.device.Form.dto.DataLinkOtherDto;
import center.misaki.device.Form.service.impl.FormDataService;
import center.misaki.device.Form.vo.OneDataVo;
import center.misaki.device.base.Result;
import center.misaki.device.Form.dto.DataScreenDto;
import center.misaki.device.Form.dto.OneDataDto;
import center.misaki.device.Form.service.FormService;
import center.misaki.device.Form.service.StructureService;
import center.misaki.device.Form.vo.FormDataVo;
import center.misaki.device.Form.vo.SimpleFieldVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author Misaki
 * 数据辅助功能额外接口
 */
@RestController
public class DataExpendController {
    
    private final FormService formService;
    private final StructureService structureService;
    private final FormDataService formDataService;

    public DataExpendController(FormService formService, StructureService structureService, FormDataService formDataService) {
        this.formService = formService;
        this.structureService = structureService;
        this.formDataService = formDataService;
    }


    //带有重复校验去提交一条数据
    @PostMapping("/submit/check")
    @FormAuthCondition(NeedSubmitSelf = true,NeedManage = true,NeedSubmit = true)
    public Result<List<String>> submitPlus(@Valid @RequestBody OneDataDto.OneDataDtoPlus dataDto, String userInfo){
        List<String> checkFailIds = formService.addOneData(dataDto, userInfo);
        if(checkFailIds.isEmpty()){
            return Result.ok(checkFailIds,"添加成功！");
        }else return Result.error(checkFailIds,"添加失败");
    }
    
    
    //带有重复校验去修改一条数据
    @PostMapping("/change/check")
    @FormAuthCondition(NeedSubmitSelf = true,NeedManage = true)
    public Result<List<String>> changePlus(@Valid @RequestBody OneDataDto.OneDataDtoPlus dataDto, String userInfo){
        List<String> checkFailIds = formService.changeOneData(dataDto, userInfo);
        if(checkFailIds.isEmpty()){
            return Result.ok(checkFailIds,"修改成功！");
        }else return Result.error(checkFailIds,"修改失败");
    }
    
    
    //全部数据筛选接口
    @PostMapping("/screen/all")
    @FormAuthCondition(NeedManage = true,NeedWatch = true)
    public Result<FormDataVo> screenDataAll(@Valid @RequestBody DataScreenDto dataDto,String userInfo){
        try {
            FormDataVo afterScreen = formService.getAllDataAfterScreen(dataDto, userInfo);
            return Result.ok(afterScreen,"筛选成功！");
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return Result.error("筛选服务出现问题，请联系开发者");
        }
    }
    
    
    //只有我管理的数据筛选接口
    @PostMapping("/screen/user")
    @FormAuthCondition(NeedManage = true,NeedWatch = true,NeedSubmitSelf = true)
    public Result<FormDataVo> screenDataUser(@Valid @RequestBody DataScreenDto dataDto ,String userInfo){
        try {
            FormDataVo afterScreen = formService.getUserDataAfterScreen(dataDto, userInfo);
            return Result.ok(afterScreen,"筛选成功！");
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return Result.error("筛选服务出现问题，请联系开发者");
        }
    }
    
    
    //数据联动快捷获得表单简单结构接口
    @GetMapping("/link/formStruc")
    public Result<List<SimpleFieldVo>> formStruc(Integer formId, String userInfo){
        List<SimpleFieldVo> fieldsInForm = structureService.getFieldsInForm(formId, userInfo);
        return Result.ok(fieldsInForm,"获取成功");
    }
    
    //数据联动接口数据接口
    @PostMapping("/link")
    public Result<List<OneDataVo.OneFieldValue>> dataLink(@RequestBody List<DataScreenDto> dataScreenDtos, String userInfo){
        try {
            List<OneDataVo.OneFieldValue> values = formService.dataLink(dataScreenDtos, userInfo);
            return Result.ok(values,"联动成功");
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return Result.error("数据联动服务出现问题，请联系开发者");
        }
    }
    
    
    //数据评论接口
    @PostMapping("/comment")
    public Result<?> dataComment(@RequestBody DataCommentDto dataCommentDto,Integer dataId){
        formDataService.addComment(dataCommentDto,dataId);
        return Result.ok(null,"评论成功");
    }
    
    
    //关联其他表单数据结构
    @PostMapping("/link/other")
    public Result<List<String>> dataLinkOther(@RequestBody DataLinkOtherDto dataLinkOtherDto,String userInfo){
        List<String> otherFormDataByField = formDataService.getOtherFormDataByField(dataLinkOtherDto, userInfo);
        return Result.ok(otherFormDataByField,"获取成功");
    }
    
    //关联查询数据
    @PostMapping("/link/form/search")
    public Result<List<OneDataVo.OneFormLinkValue>> dataLinkFormSearch(@RequestBody List<DataScreenDto> dataScreenDto, String userInfo){
        List<OneDataVo.OneFormLinkValue> dataLinkFormSearch = null;
        try {
            dataLinkFormSearch = formService.getDataLinkFormSearch(dataScreenDto, userInfo);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return Result.error("关联查询服务出现问题，请联系开发者");
        }
        return Result.ok(dataLinkFormSearch,"获取成功");
    }
    
    
}
