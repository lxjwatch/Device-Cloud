package center.misaki.device.Auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;

import java.util.ArrayList;
import java.util.List;

@Configuration //这是一个配置类
@EnableAuthorizationServer //当前应用是一个认证服务器
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {//AuthorizationServerConfigurerAdapter：认证服务器适配器

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder; //Spring 对密码加密的封装，自己配置下
    private final UserDetailsService userDetailsService;
    private final RedisTokenStore tokenStore;
    private final JwtAccessTokenConverter accessTokenConverter;
    public AuthorizationServerConfig(AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder, 
                                     UserDetailsService userDetailsService, RedisTokenStore tokenStore, JwtAccessTokenConverter accessTokenConverter) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.tokenStore = tokenStore;
        this.accessTokenConverter = accessTokenConverter;
    }

    /**
     * 1，配置客户端的信息，让认证服务器知道有哪些客户端来申请令牌。
     *
     * ClientDetailsServiceConfigurer：客户端的详情服务的配置
     * @param clients
     * @throws Exception
     */
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        // 配置一个客户端，放在内存中，可以放在数据库中
        // 客户端ID // 客户端安全码 // 允许请求范围 此三种属性 唯一限定一个客户端
        clients.inMemory()//配置在内存里

                // ======注册 客户端 ，使客户端能够访问认证服务器======
                .withClient("pc")//客户端ID
                .secret(passwordEncoder.encode("secret"))//客户端安全码
                .scopes("all")//客户端pc有什么权限
                .authorizedGrantTypes("password")//授权方式：在给客户端pc授权的时候可以使用哪种授权方式
                .accessTokenValiditySeconds(60 * 60 * 2)//token有效期：2小时
                .and()
                .withClient("wx")
                .secret(passwordEncoder.encode("secret"))
                .scopes("all")
                //微信登陆后面选择的方式无效
                .authorizedGrantTypes("password")
                .accessTokenValiditySeconds(60*60*2);
    }

    /**
     *  2、令牌访问端点、令牌服务（怎么存、怎么管）
     *     配置授权服务器属性：token存储、token格式、授权模式
     *
     *  (1) 用户登录：userDetailsService，获取用户详细信息；
     *  (2) Token
     */
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
                .tokenStore(tokenStore)// 指定token的存储方法（令牌存储策略）

                //传给他一个authenticationManager用来校验传过来的用户信息是不是合法的,注进来一个，自己实现
                .authenticationManager(authenticationManager)// 指定认证管理器（用于密码模式的认证）

                .userDetailsService(userDetailsService)
                .reuseRefreshTokens(false);//是否产生刷新令牌
        //JWT转换器
        if (accessTokenConverter != null && tokenEnhancer() != null) {
            TokenEnhancerChain enhancerChain = new TokenEnhancerChain();
            List<TokenEnhancer> enhancers = new ArrayList<>();
            enhancers.add(tokenEnhancer());//添加token增强器
            enhancers.add(accessTokenConverter);//添加token转换器
            enhancerChain.setTokenEnhancers(enhancers);
            // 一个处理链，先添加，再转换
            endpoints
                    .tokenEnhancer(enhancerChain)
                    .accessTokenConverter(accessTokenConverter);
        }
    }

    /**
     * 3、令牌访问端点安全策略
     *   配置请求授权服务器安全性：security，即开启接口的访问权限
     *   即配置资源服务器过来验token 的规则
     *
     * （1）配置请求token权限：
     *
     * （2）配置请求checkToken权限：
     *      过来验令牌有效性的请求，不是谁都能验的，必须要是经过身份认证的。
     *      所谓身份认证就是，必须携带clientId，clientSecret，
     *      否则随便一请求过来验token是不验的
     *
     */
    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {

        security
                // 允许所有人请求token
                .tokenKeyAccess("permitAll()")
                // 已验证的用户才能请求check_token端点
                .checkTokenAccess("isAuthenticated()")
                // 允许客户端表单认证
                .allowFormAuthenticationForClients();
    }

    /**
     * token增强器
     *
     * @return
     */
    @Bean
    public TokenEnhancer tokenEnhancer(){
        return new JwtTokenEnhancer();
    }
    

}
