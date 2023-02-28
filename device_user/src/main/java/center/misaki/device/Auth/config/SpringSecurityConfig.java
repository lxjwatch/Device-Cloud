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
 */
@Configuration
@EnableWebSecurity
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
//    下面代码有bug已不再需要
//    @Override
//    public void configure(WebSecurity web) throws Exception{
////        这样配置之后指定的路径会绕过Security管理的所有Filter(注册所有人都可以放行)
//        web.ignoring()
//                .antMatchers("/uaa/user/registerEmployee")
//                .antMatchers("/uaa/user/registerUser");
//    }
}
