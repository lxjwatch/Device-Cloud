package center.misaki.device.Auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.oauth2.provider.expression.OAuth2WebSecurityExpressionHandler;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
    private final OAuth2WebSecurityExpressionHandler expressionHandler;
    private final OAuth2AuthenticationEntryPoint oAuth2AuthenticationEntryPoint;
    private final ResponseExceptionTranslator  responseExceptionTranslator;

    public ResourceServerConfig(OAuth2WebSecurityExpressionHandler expressionHandler, OAuth2AuthenticationEntryPoint oAuth2AuthenticationEntryPoint, ResponseExceptionTranslator responseExceptionTranslator) {
        this.expressionHandler = expressionHandler;
        this.oAuth2AuthenticationEntryPoint = oAuth2AuthenticationEntryPoint;
        this.responseExceptionTranslator = responseExceptionTranslator;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                //下面这五个接口放行
                .authorizeRequests()
                .antMatchers("/error").permitAll()
                .antMatchers("/login/token").permitAll()
                .antMatchers("/wx/login/token").permitAll()
                .antMatchers("/user/registerEmployee").permitAll()
                .antMatchers("/user/registerUser").permitAll()

                //其他请求都需要做鉴权
                .anyRequest()
                .access("@securityService.hasPermission(request,authentication)")

                // 不创建会话
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
        //自定义认证失败，处理逻辑 默认返回体为{"error":"unauthorized","error_token":"token is invalid:"}
        resources.authenticationEntryPoint(oAuth2AuthenticationEntryPoint);
    }

}
