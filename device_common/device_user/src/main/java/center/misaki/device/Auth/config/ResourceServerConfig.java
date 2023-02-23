package center.misaki.device.Auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.oauth2.provider.expression.OAuth2WebSecurityExpressionHandler;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@EnableResourceServer//告诉userApp，这是资源服务器
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
    private final OAuth2WebSecurityExpressionHandler expressionHandler;
    private final OAuth2AuthenticationEntryPoint oAuth2AuthenticationEntryPoint;
    private final ResponseExceptionTranslator  responseExceptionTranslator;

    public ResourceServerConfig(OAuth2WebSecurityExpressionHandler expressionHandler, OAuth2AuthenticationEntryPoint oAuth2AuthenticationEntryPoint, ResponseExceptionTranslator responseExceptionTranslator) {
        this.expressionHandler = expressionHandler;
        this.oAuth2AuthenticationEntryPoint = oAuth2AuthenticationEntryPoint;
        this.responseExceptionTranslator = responseExceptionTranslator;
    }

    /**
     * 进入userApp的所有请求，哪些要拦截，哪些要放过，在这里配置
     */
    public void configure(HttpSecurity http) throws Exception {
        http.
                //关闭csrf
                csrf().disable()

                // 对于登录接口允许匿名访问
                .authorizeRequests()
                .antMatchers("/error").permitAll()
                .antMatchers("/login/token").permitAll()
                .antMatchers("/wx/login/token").permitAll()

                // 对于注册接口允许匿名访问(仍需要经过过滤链)
                .antMatchers("/register").permitAll()

                // 除上面外的所有请求全部需要鉴权认证
                .anyRequest()
                .access("@securityService.hasPermission(request,authentication)")

                // 不创建会话，即不通过Session获取SecurityContext
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

    }

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        resources.expressionHandler(expressionHandler);
        AccessDeniedHandler accessDeniedHandler = resources.getAccessDeniedHandler();
        if(accessDeniedHandler instanceof OAuth2AccessDeniedHandler)
        {
            ((OAuth2AccessDeniedHandler) accessDeniedHandler).setExceptionTranslator(responseExceptionTranslator);
        }
        //自定义认证失败，处理逻辑
        resources.authenticationEntryPoint(oAuth2AuthenticationEntryPoint);
    }

}
