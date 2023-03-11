package center.misaki.zuul.filter;

import center.misaki.device.base.Result;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class AccessFilter extends ZuulFilter {
    @Autowired
    private RestTemplate restTemplate;


    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return Boolean.TRUE;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        HttpServletResponse response = ctx.getResponse();
        log.info("request {}:{}",request.getMethod(),request.getRequestURI());

        //判断请求路径中是否包含/uaa,/data/form/initMenu，如果有则放行
        if (StringUtils.startsWithAny(request.getRequestURI(), new String[]{"/uaa","/data/form/initMenu","/data/form/initTemplate"})) {
            ctx.setSendZuulResponse(Boolean.TRUE);
            return null;
        }

        if (request.getMethod().equals(HttpMethod.OPTIONS.name())){
            String curOrigin = request.getHeader("Origin");
            log.info("###跨域过滤器->当前访问来源->{}###",curOrigin);
            response.setHeader("Access-Control-Allow-Origin",request.getHeader("Origin"));
            response.setHeader("Access-Control-Allow-Credentials","true");
            response.setHeader("Access-Control-Allow-Headers","authorization, x-requested-with,content-type");
            response.setHeader("Access-Control-Allow-Methods","POST");
            ctx.setSendZuulResponse(Boolean.FALSE);
            ctx.setResponseStatusCode(200);
            return null;
        }

        //其余请求 皆做鉴权处理
        ResponseEntity<Result> stringResponseEntity;
        try{
            //（1）鉴权：通过Token进行鉴权，成功后返回用户信息
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", request.getHeader("Authorization"));
            HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
            stringResponseEntity = restTemplate.exchange("http://localhost:8000/uaa/isAccessed", HttpMethod.GET, requestEntity, Result.class);
            // 判断相应状态码，如果状态码不等于0，就说明是失败并抛出异常(携带状态码code和消息msg)
            if(stringResponseEntity.getBody().getCode()!=0){
                throw new HttpClientErrorException(HttpStatus.resolve(stringResponseEntity.getBody().getCode()),stringResponseEntity.getBody().getMsg());
            }
            // （2）鉴权通过后访问原来请求URI
            // 用户信息
            Map<String, String[]> parameterMap = request.getParameterMap();
            parameterMap.put("userInfo",new String[]{(String) stringResponseEntity.getBody().getData()});

            // 请求参数
            Map<String, List<String>> params = new HashMap<>(10);
            parameterMap.forEach((key, value) -> params.put(key, Arrays.asList(value)));

            // 发送请求: key==userInfo; value==StringZipUtil.compressData(userInfo)
            ctx.setRequestQueryParams(params);
        }catch (HttpClientErrorException e) {
            ctx.setSendZuulResponse(Boolean.FALSE);
            ctx.set("error.status_code", e.getRawStatusCode());
            throw new ZuulException(e,e.getMessage(),e.getRawStatusCode(),"");
        }
        return null;
    }
}
