package center.misaki.device.Flow.api.feign;

import center.misaki.device.Flow.Flow;
import center.misaki.device.base.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Set;

/**
 * @author Misaki
 */
@FeignClient(name = "auth-center",contextId = "authCenter2")
public interface FlowFeignController {
    @RequestMapping(value = "/user/getHeadUserIds",method = RequestMethod.POST)
    Result<Set<Integer>> getHeadUserIds(@RequestBody Flow.Head head, @RequestHeader(name = "Authorization") String token);
     
    @RequestMapping(value = "/mail/flow",method = RequestMethod.POST)
    Result<?> flowAdvice(@RequestBody FlowMailDto flowMailDto ,@RequestHeader(name = "Authorization") String token);
    
    @RequestMapping(value = "/mail/flow/end",method = RequestMethod.POST)
    Result<?> flowRejectAdvice(@RequestBody FlowMailDto flowMailDto ,@RequestHeader(name = "Authorization") String token);
    
    @RequestMapping(value = "/mail/flow/copy",method = RequestMethod.POST)
    Result<?> flowCopyAdvice(@RequestBody FlowMailDto flowMailDto ,@RequestHeader(name = "Authorization") String token);
    
}
