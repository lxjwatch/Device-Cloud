package center.misaki.zuul.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;

import javax.servlet.http.HttpServletResponse;

/**
 * @author :Misaki
 * @description: 将异常信息做转发 ，异常处理 交给 SendErrorFilter类或者其子类处理
 */
@Slf4j
public class ErrorFilter extends ZuulFilter {
    @Override
    public String filterType() {
        return FilterConstants.ERROR_TYPE;
    }

    @Override
    public int filterOrder() {
        return 10;
    }

    @Override
    public boolean shouldFilter() {
        return Boolean.TRUE;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext currentContext = RequestContext.getCurrentContext();
        Throwable throwable = currentContext.getThrowable();
        if(!currentContext.containsKey("error.status_code"))
        {
            //如果是zuul的异常则设置状态码为zuul的异常状态码
            if(throwable instanceof ZuulException)
            {
                currentContext.set("error.status_code", ((ZuulException)throwable).nStatusCode);
            }else {
                //如果不是zuul的异常则设置状态码为500
                currentContext.set("error.status_code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            currentContext.set("error.exception", throwable.getCause());
            currentContext.set("error.message","网关内部错误");
        }
        return null;
    }
}
