package center.misaki.device.Config.bean;


import center.misaki.device.Auth.dto.RedisTokenExpendStore;
import center.misaki.device.Config.MyMetaObjectHandler;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.oauth2.provider.expression.OAuth2WebSecurityExpressionHandler;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;
import org.springframework.web.client.RestTemplate;

/**
 * @apiNote 配置 Bean 类
 */
@Configuration
public class ConfigBeanConfiguration {
    
    private final RedisConnectionFactory connectionFactory;
    
    private final ClientDetailsService clientDetailsService;

    public ConfigBeanConfiguration(RedisConnectionFactory connectionFactory, ClientDetailsService clientDetailsService) {
        this.connectionFactory = connectionFactory;
        this.clientDetailsService = clientDetailsService;
    }

    @Bean
    @ConfigurationProperties(prefix = "login")
    public LoginProperties loginProperties() {
        return new LoginProperties();
    }

    /**
     * 密码编码器：
     * 用于SpringSecurity认证时将用户输入的明文密码编码后与从数据库查询到的密码进行匹配
     */
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

    @Bean
    public OAuth2WebSecurityExpressionHandler oAuth2WebSecurityExpressionHandler(ApplicationContext applicationContext) {
        OAuth2WebSecurityExpressionHandler expressionHandler = new OAuth2WebSecurityExpressionHandler();
        expressionHandler.setApplicationContext(applicationContext);
        return expressionHandler;
    }

    /**
     * 令牌存储策略：token存储在redis
     */
    @Bean
    public RedisTokenStore tokenStore() {
        return new RedisTokenExpendStore(connectionFactory,clientDetailsService);
    }

    /**
     * 对jwt进行签名的key，jwt是明文，签名防篡改。
     * 接收token的人需要用同样的key验签名，需要把这个key通过服务暴漏出去，
     * 使用服务的人才能拿到key
     */
    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        converter.setSigningKey("device0001...");
        return converter;
    }

    @Bean
    public OAuth2AuthenticationEntryPoint oAuth2AuthenticationEntryPoint(){
        return new OAuth2AuthenticationEntryPoint();
    }
    
    @Bean
    public MetaObjectHandler myMetaObjectHandler(){
        return  new MyMetaObjectHandler();
    }




}
