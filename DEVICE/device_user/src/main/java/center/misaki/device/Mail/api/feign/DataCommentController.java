package center.misaki.device.Mail.api.feign;

import center.misaki.device.Mail.Dto.DataCommentDto;
import center.misaki.device.base.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Misaki
 */
@FeignClient(value = "device-data")
public interface DataCommentController {
    
    @RequestMapping(value ="/comment",method = RequestMethod.POST)
    Result<?> addComment(@RequestBody DataCommentDto dataCommentDto, @RequestParam("dataId") Integer dataId);
    
}
