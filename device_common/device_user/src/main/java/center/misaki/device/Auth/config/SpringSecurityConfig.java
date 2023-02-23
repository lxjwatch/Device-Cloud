package center.misaki.device.Auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * @author Misaki
 *
 * 要认证跟用户相关的信息，一般用 AuthenticationManager
 * 在接口中我们通过AuthenticationManager的authenticate方法来进行用户认证,
 * 所以需要在SpringSecurityConfig中配置把AuthenticationManager注入容器。
 */
@Configuration
@EnableWebSecurity//使安全配置生效
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {

    /**
     * 把AuthenticationManager暴露为bean
     * @return
     * @throws Exception
     */
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        //这样配置之后指定的路径会绕过Security管理的所有Filter(注册所有人都可以放行)
        web.ignoring().antMatchers("/register");
    }
}
