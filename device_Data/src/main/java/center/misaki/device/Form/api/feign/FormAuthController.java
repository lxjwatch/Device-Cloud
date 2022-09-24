package center.misaki.device.Form.api.feign;

import center.misaki.device.base.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;

/**
 * @author Misaki
 */
@FeignClient(name = "auth-center",contextId = "authCenter1")
public interface FormAuthController {
    
    @RequestMapping(value = "/authF/show",method = RequestMethod.GET)
    Result<Set<Integer>> showAuthForm(@RequestParam("formId") Integer formId, @RequestHeader(name = "Authorization") String token);
  
}
