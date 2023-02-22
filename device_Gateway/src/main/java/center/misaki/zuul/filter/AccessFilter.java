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

    /**
     * 过滤器类型
     * pre
     * routing
     * post
     * error
     *
     * @return
     */
    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    /**
     * 执行顺序
     * 数值越小，优先级越高
     *
     * @return
     */
    @Override
    public int filterOrder() {
        return 0;
    }

    /**
     * 执行条件
     * true 开启
     * false 关闭
     *
     * @return
     */
    @Override
    public boolean shouldFilter() {
        return Boolean.TRUE;
    }

    /**
     * 动作（具体操作）
     * 具体逻辑
     *
     * @return
     * @throws ZuulException
     */
    @Override
    public Object run() throws ZuulException {
        // 获取请求上下文
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        HttpServletResponse response = ctx.getResponse();
        //日志记录每次请求的 (1)方法 和 (2)URI
        log.info("request {}:{}",request.getMethod(),request.getRequestURI());

        //检测请求是否以指定的前缀(/uaa)开始
        if (StringUtils.startsWithAny(request.getRequestURI(), new String[]{"/uaa"})) {
            //如果以指定的前缀(/uaa)开始，则放行请求
            ctx.setSendZuulResponse(Boolean.TRUE);
            return null;
        }

        //预检请求处理
        if (request.getMethod().equals(HttpMethod.OPTIONS.name())){//根据请求方式判断请求是否为预检请求
            //获取并记录预检请求的来源
            String curOrigin = request.getHeader("Origin");
            log.info("###跨域过滤器->当前访问来源->{}###",curOrigin);

            /**
             * Access-Control-Allow-Origin
             * 指定允许其他域名访问
             *
             * 这里使用为：限制只有当前HTTP请求可以放行
             */
            response.setHeader("Access-Control-Allow-Origin",request.getHeader("Origin"));

            /**
             * Access-Control-Allow-Credentials
             * 该字段可选。它的值是一个布尔值，表示是否允许发送Cookie。
             * 默认情况下，Cookie不包括在CORS请求之中。
             * 设为true，即表示服务器明确许可，Cookie可以包含在请求中，一起发给服务器。
             * 这个值也只能设为true，如果服务器不要浏览器发送Cookie，删除该字段即可。
             */
            response.setHeader("Access-Control-Allow-Credentials","true");

            /**
             * Access-Control-Allow-Headers
             * 用于 preflight request（预检请求）中，
             * 列出了将会在正式请求的 Access-Control-Request-Headers 字段中出现的首部信息。
             *
             * 这里使用为：除了 CORS 安全清单列出的请求标头外，
             * 对服务器的 CORS 请求还支持名为 authorization, x-requested-with,content-type 的自定义标头
             */
            response.setHeader("Access-Control-Allow-Headers","authorization, x-requested-with,content-type");

            /**
             * Access-Control-Allow-Methods
             * 对 preflight request.（预检请求）的应答中
             * 明确了客户端所要访问的资源允许使用的 方法 或 方法列表。
             *
             * 这里使用为：只允许Post请求访问资源
             */
            response.setHeader("Access-Control-Allow-Methods","POST");

            // 请求结束，不在继续向下请求
            ctx.setSendZuulResponse(Boolean.FALSE);

            // 响应状态码，HTTP 200 表示预检请求后端成功
            ctx.setResponseStatusCode(200);
            return null;
        }

        //其余请求 皆做鉴权处理（从客户端请求携带的token中反解出用户信息放到请求参数中转发给微服务服务器）
        ResponseEntity<Result> stringResponseEntity;
        try{
            HttpHeaders headers = new HttpHeaders();
            //Authorization:token
            headers.add("Authorization", request.getHeader("Authorization"));
            HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

            //此处转发请求至device_user\src\main\java\center.misaki.device\Auth\api\AuthController
            //获取用户的所有信息包括权限
            stringResponseEntity = restTemplate.exchange("http://localhost:8000/uaa/isAccessed", HttpMethod.GET, requestEntity, Result.class);
            //如果获取不成功，抛出异常（SUCCESS = 0）
            if(stringResponseEntity.getBody().getCode()!=0){
                throw new HttpClientErrorException(HttpStatus.resolve(stringResponseEntity.getBody().getCode()),stringResponseEntity.getBody().getMsg());
            }
            //获取成功，将用户信息放到 请求的属性中
            Map<String, String[]> parameterMap = request.getParameterMap();
            //用户信息为User对象
            parameterMap.put("userInfo",new String[]{(String) stringResponseEntity.getBody().getData()});
            Map<String, List<String>> params = new HashMap<>(10);
            //将用户信息转成List集合存储
            parameterMap.forEach((key, value) -> params.put(key, Arrays.asList(value)));
            //将压缩后的用户信息放到请求的属性中（后续到请求接口之前需要先解压用户信息再将用户信息作为参数传入接口）
            ctx.setRequestQueryParams(params);
        }catch (HttpClientErrorException e) {
            ctx.setSendZuulResponse(Boolean.FALSE);
            ctx.set("error.status_code", e.getRawStatusCode());
            throw new ZuulException(e,e.getMessage(),e.getRawStatusCode(),"");
        }
        return null;
    }
}
