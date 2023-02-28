package center.misaki.device.Auth.api;

import center.misaki.device.AddressBook.dao.UserMapper;
import center.misaki.device.Auth.SecurityService;
import center.misaki.device.Auth.SecurityUtils;
import center.misaki.device.Auth.dao.NormalAdminMapper;
import center.misaki.device.Auth.dao.SysAdminMapper;
import center.misaki.device.Auth.dto.AuthDto;
import center.misaki.device.Auth.dto.AuthUserDto;
import center.misaki.device.Auth.dto.JwtUserDto;
import center.misaki.device.Auth.dto.WxAuthUserDto;
import center.misaki.device.Auth.pojo.NormalAdmin;
import center.misaki.device.Auth.pojo.SysAdmin;
import center.misaki.device.Config.bean.LoginProperties;
import center.misaki.device.base.Result;
import center.misaki.device.domain.Pojo.User;
import center.misaki.device.utils.JSONUtils;
import center.misaki.device.utils.StringZipUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.exceptions.UnapprovedClientAuthenticationException;
import org.springframework.security.oauth2.provider.*;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@Slf4j
public class AuthController {
    
    private static final String wxAppId="wx9e1f77ef7b07968f";
    private static final String wxAppSecret="8d658732ffce56de1a4d395e8d9e1309";
    private final PasswordEncoder passwordEncoder;
    private final AuthorizationServerTokenServices authorizationServerTokenServices;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final ClientDetailsService clientDetailsService;
    private final LoginProperties loginProperties;
    private  final TokenStore tokenStore;
    private final SecurityService securityService;
    private final RestTemplate restTemplate;

    public AuthController(PasswordEncoder passwordEncoder, AuthorizationServerTokenServices authorizationServerTokenServices, AuthenticationManagerBuilder authenticationManagerBuilder,
                          ClientDetailsService clientDetailsService, LoginProperties loginProperties, TokenStore tokenStore, SecurityService securityService, RestTemplate restTemplate) {
        this.passwordEncoder = passwordEncoder;
        this.authorizationServerTokenServices = authorizationServerTokenServices;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.clientDetailsService = clientDetailsService;
        this.loginProperties = loginProperties;
        this.tokenStore = tokenStore;
        this.securityService=securityService;
        this.restTemplate = restTemplate;
    }
    
    //PC登陆接口
    @PostMapping("/login/token")
    public OAuth2AccessToken login(@Validated @RequestBody AuthUserDto authUser){
        
        ClientDetails clientDetails = clientDetailsService.loadClientByClientId(authUser.getClientId());
        if (clientDetails == null) {
            throw new UnapprovedClientAuthenticationException("clientId对应的配置信息不存在:" + authUser.getClientId());
        } else if (passwordEncoder.matches(clientDetails.getClientSecret(), authUser.getClientSecret())) {
            throw new UnapprovedClientAuthenticationException("clientSecret不匹配:" + authUser.getClientId());
        }
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(authUser.getUsername(), authUser.getPassword());
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        TokenRequest tokenRequest = new TokenRequest(new HashMap<>(), authUser.getClientId(), clientDetails.getScope(), "user");
        OAuth2Request oAuth2Request = tokenRequest.createOAuth2Request(clientDetails);
        OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, authentication);
        OAuth2AccessToken token = authorizationServerTokenServices.createAccessToken(oAuth2Authentication);

        if(loginProperties.isSingleLogin()){
            String accessToken = token.getValue();
            String refreshToken = token.getRefreshToken().getValue();
            if (StringUtils.isNotBlank(accessToken)) {
                OAuth2AccessToken oAuth2AccessToken = tokenStore.readAccessToken(accessToken);
                if (Objects.nonNull(oAuth2AccessToken)) {
                    tokenStore.removeAccessToken(oAuth2AccessToken);
                }
            }
            if (StringUtils.isNotBlank(refreshToken)) {
                OAuth2RefreshToken oAuth2RefreshToken = tokenStore.readRefreshToken(refreshToken);
                if (Objects.nonNull(oAuth2RefreshToken)) {
                    tokenStore.removeRefreshToken(oAuth2RefreshToken);
                }
            }
            //重新生成
            token = authorizationServerTokenServices.createAccessToken(oAuth2Authentication);
        }
        return token;
    }
    
    //微信登陆接口
    @PostMapping("/wx/login/token")
    public OAuth2AccessToken login(@Validated @RequestBody WxAuthUserDto authUser){
        ClientDetails clientDetails = clientDetailsService.loadClientByClientId(authUser.getClientId());
        if (clientDetails == null) {
            throw new UnapprovedClientAuthenticationException("clientId对应的配置信息不存在:" + authUser.getClientId());
        } else if (passwordEncoder.matches(clientDetails.getClientSecret(), authUser.getClientSecret())) {
            throw new UnapprovedClientAuthenticationException("clientSecret不匹配:" + authUser.getClientId());
        }
        String code =authUser.getCode();
        String url = getUrl(code);
        ResponseEntity<String> wxOpenid = restTemplate.getForEntity(url,String.class);
        //通过openid 获取用户信息   
        String openid = JSON.parseObject(wxOpenid.getBody()).getString("openid");
        log.info("======================openid:=======================\n{}\n",openid);
        UserDetails userDetails = securityService.loadUserByWxOpenId(openid);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        TokenRequest tokenRequest = new TokenRequest(new HashMap<>(), authUser.getClientId(), clientDetails.getScope(), "user");
        OAuth2Request oAuth2Request = tokenRequest.createOAuth2Request(clientDetails);
        OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, authenticationToken);
        OAuth2AccessToken token = authorizationServerTokenServices.createAccessToken(oAuth2Authentication);
        if(loginProperties.isWxSingleLogin()){
            String accessToken = token.getValue();
            String refreshToken = token.getRefreshToken().getValue();
            if (StringUtils.isNotBlank(accessToken)) {
                OAuth2AccessToken oAuth2AccessToken = tokenStore.readAccessToken(accessToken);
                if (Objects.nonNull(oAuth2AccessToken)) {
                    tokenStore.removeAccessToken(oAuth2AccessToken);
                }
            }
            if (StringUtils.isNotBlank(refreshToken)) {
                OAuth2RefreshToken oAuth2RefreshToken = tokenStore.readRefreshToken(refreshToken);
                if (Objects.nonNull(oAuth2RefreshToken)) {
                    tokenStore.removeRefreshToken(oAuth2RefreshToken);
                }
            }
            //重新生成
            token = authorizationServerTokenServices.createAccessToken(oAuth2Authentication);
        }
        return token;
    }
    
    private static String  getUrl(String code) {
        Map<String,Object> map = new HashMap<>(4);
        map.put("appid",wxAppId);
        map.put("secret",wxAppSecret);
        map.put("js_code",code);
        map.put("grant_type","authorization_code");
        String param = "";
        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            param += key + "=" + map.get(key) + "&";
        }
        return "https://api.weixin.qq.com/sns/jscode2session?"+param;
    }
    
    @Autowired
    private NormalAdminMapper normalAdminMapper;
    @Autowired
    private SysAdminMapper sysAdminMapper;
    @Autowired
    private UserMapper userMapper;
    
    @GetMapping("/isAccessed")
    public Result<String> isAccessed(){
        JwtUserDto user;
        try {
            user = SecurityUtils.getCurrentUser();
            
            User user1 = userMapper.selectById(user.getUserId());
            Long count = sysAdminMapper.selectCount(new QueryWrapper<SysAdmin>().eq("user_id", user.getUserId()));
            if(count>0) user.setSysAdmin(true);
            if(user1.getNormalAdminGroupId()!=-1) {
                user.setNormalAdmin(true);
                NormalAdmin normalAdmin = normalAdminMapper.selectById(user1.getNormalAdminGroupId());
                if (normalAdmin != null) user.setAuthDto(JSON.parseObject(normalAdmin.getConfig(), AuthDto.class));
            }
            OAuth2AccessToken accessToken = tokenStore.getAccessToken((OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication());
            user.setToken(accessToken.getValue());
        }catch(Exception e){
            return Result.error("身份信息验证失败");
        }
        
        String data = StringZipUtil.compressData(JSONUtils.beanToJson(user));
        return Result.ok(data,"验证成功");
    }
    
    
//    @GetMapping("/isAdmin")
//    public Result<Map<String,Object>> isAdmin(){
//        JwtUserDto user = SecurityUtils.getCurrentUser();
//        Map<String, Object> ans = new HashMap<>();
//        ans.put("creater",user.isCreater());
//        ans.put("sysAdmin",user.isSysAdmin());
//        ans.put("norAdmin",user.isNormalAdmin());
//        if (user.getAuthDto() == null){
//            String config = "{\"addressBook\":{\"department\":false,\"role\":[false,false]},\"editForm\":false,\"name\":\"无\",\"scope\":{\"department\":[],\"role\":[]}}";
//            user.setAuthDto(JSON.parseObject(config, AuthDto.class));
//        }
//        ans.put("authDetails",user.getAuthDto());
//        return Result.ok(ans,"获取成功");
//    }

    //上面注释代码的优化版
    @GetMapping("/isAdmin")
    public Result<Map<String,Object>> isAdmin(){
        JwtUserDto user = SecurityUtils.getCurrentUser();
        Map<String, Object> ans = new HashMap<>();
        ans.put("creater",user.isCreater());
        ans.put("sysAdmin",user.isSysAdmin());
        ans.put("norAdmin",user.isNormalAdmin());
        AuthDto authDto = user.getAuthDto();
        if (authDto == null){
            authDto = JSON.parseObject("{\"addressBook\":{\"department\":false,\"role\":[false,false]},\"editForm\":false,\"name\":\"无\",\"scope\":{\"department\":[],\"role\":[]}}", AuthDto.class);
            user.setAuthDto(authDto);
        }
        ans.put("authDetails",authDto);
        return Result.ok(ans,"获取成功");
    }
    





}
