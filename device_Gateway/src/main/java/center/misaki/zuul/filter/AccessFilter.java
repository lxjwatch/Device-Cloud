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

        if (StringUtils.startsWithAny(request.getRequestURI(), new String[]{"/uaa"})) {
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
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", request.getHeader("Authorization"));
            HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
            stringResponseEntity = restTemplate.exchange("http://localhost:8000/uaa/isAccessed", HttpMethod.GET, requestEntity, Result.class);
            if(stringResponseEntity.getBody().getCode()!=0){
                throw new HttpClientErrorException(HttpStatus.resolve(stringResponseEntity.getBody().getCode()),stringResponseEntity.getBody().getMsg());
            }
            //将用户信息放到 请求的属性中
            Map<String, String[]> parameterMap = request.getParameterMap();
            parameterMap.put("userInfo",new String[]{(String) stringResponseEntity.getBody().getData()});
            Map<String, List<String>> params = new HashMap<>(10);
            parameterMap.forEach((key, value) -> params.put(key, Arrays.asList(value)));
            ctx.setRequestQueryParams(params);
        }catch (HttpClientErrorException e) {
            ctx.setSendZuulResponse(Boolean.FALSE);
            ctx.set("error.status_code", e.getRawStatusCode());
            throw new ZuulException(e,e.getMessage(),e.getRawStatusCode(),"");
        }
        return null;
    }
}
