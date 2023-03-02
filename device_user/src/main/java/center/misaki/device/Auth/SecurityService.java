package center.misaki.device.Auth;

import center.misaki.device.Auth.dao.NormalAdminMapper;
import center.misaki.device.Auth.dao.SecurityMapper;
import center.misaki.device.Auth.dao.SysAdminMapper;
import center.misaki.device.Auth.dto.AuthDto;
import center.misaki.device.Auth.dto.JwtUserDto;
import center.misaki.device.Auth.pojo.NormalAdmin;
import center.misaki.device.Auth.pojo.SysAdmin;
import center.misaki.device.domain.Pojo.User;
import center.misaki.device.exception.BadRequestException;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Misaki
 * 安全控制总实现
 */
@Service
@Slf4j
public class SecurityService implements UserDetailsService {
    
    private final SecurityMapper securityMapper;
    private final AntPathMatcher antPathMatcher;
    private final List<String> authorizationUrls;


    private final RedisTokenStore tokenStore;
    private final NormalAdminMapper normalAdminMapper;
    private final SysAdminMapper sysAdminMapper;
    
    
    public SecurityService(SecurityMapper securityMapper, RedisTokenStore redisTokenStore, NormalAdminMapper normalAdminMapper, SysAdminMapper sysAdminMapper) {
        this.securityMapper = securityMapper;
        this.tokenStore = redisTokenStore;
        this.normalAdminMapper = normalAdminMapper;
        this.sysAdminMapper = sysAdminMapper;
        this.antPathMatcher=new AntPathMatcher();
        this.authorizationUrls=new ArrayList<>();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if(username==null||username.equals("")) return null;
        User user = securityMapper.selectByUserName(username);
        if(user==null) throw new UsernameNotFoundException("用户名找不到");
        return new JwtUserDto(user);
    }
    
    //微信验证逻辑
    public UserDetails loadUserByWxOpenId(String wxOpenId){
        if(wxOpenId==null||wxOpenId.equals("")) return null;
        User user = securityMapper.selectByWxOpenId(wxOpenId);
        if(user==null) throw new UsernameNotFoundException("该微信OpenID无效");
        if(user.getIsDelete()|| user.getIsForbidden()){
            throw new BadRequestException("该账户有异常,无法登陆");
        }
        JwtUserDto userDto = new JwtUserDto(user);
        userDto.eraseCredentials();
        return userDto;
    }
    
    
    
    

    
    public boolean hasPermission(HttpServletRequest request, Authentication authentication) {
        String requestURI = request.getRequestURI();
        Object principal = authentication.getPrincipal();
        boolean hasPermission = false;
        //判断请求是否为放行请求
        for (String url : authorizationUrls) {
            if (antPathMatcher.match(url, requestURI)) {
                hasPermission = Boolean.TRUE;
                break;
            }
        }
        if ((principal instanceof UserDetails) && !hasPermission) {
            //当前如果是已登录的用户 就可以放权
            hasPermission = Boolean.TRUE;
            
            JwtUserDto jwtUserDto= (JwtUserDto) principal;
            User user1 = securityMapper.selectById(jwtUserDto.getUserId());
            Long count = sysAdminMapper.selectCount(new QueryWrapper<SysAdmin>().eq("user_id", jwtUserDto.getUserId()));
            if(count>0) jwtUserDto.setSysAdmin(true);
            if(user1.getNormalAdminGroupId()!=-1)
            {
                jwtUserDto.setNormalAdmin(true);
                NormalAdmin normalAdmin = normalAdminMapper.selectById(user1.getNormalAdminGroupId());
                if(normalAdmin!=null) jwtUserDto.setAuthDto(JSON.parseObject(normalAdmin.getConfig(), AuthDto.class));
            }
            
        } else if (principal instanceof String
                && StringUtils.equalsIgnoreCase(principal.toString(), "anonymousUser")) {

            // 如果用户是匿名用户 可以在此处校验他的权限
//            return false;
        }
        return hasPermission;
    }
}
