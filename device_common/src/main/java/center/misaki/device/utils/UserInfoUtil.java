package center.misaki.device.utils;

import center.misaki.device.domain.Dto.AuthDto;
import center.misaki.device.domain.Dto.JwtUserDto;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * 解析JWT令牌中的用户信息
 * 解析字符串必须是解压缩过后的用户信息字符串
 */
public class UserInfoUtil {
    
    public static Boolean isCreater(String userInfo){
        JSONObject user = JSONUtils.jsonToBean(userInfo);
        //判断user是否为空，空则抛出AssertionError异常
        assert user != null;
        return user.getBoolean("Creater");
    }
    
    public static Boolean isSysAdmin(String userInfo){
        JSONObject user = JSONUtils.jsonToBean(userInfo);
        assert user != null;
        return user.getBoolean("SysAdmin");
    }
    
    public static JSONObject getObject(String userInfo){
        JSONObject user = JSONUtils.jsonToBean(userInfo);
        assert user != null;
        return user;
    }
    
    
    public static Integer getTenementId(String userInfo){
        JSONObject user = JSONUtils.jsonToBean(userInfo);
        assert user != null;
        return user.getInteger("tenementId");
    }
    
    public static String getUserNickName(String userInfo){
        JSONObject user = JSONUtils.jsonToBean(userInfo);
        assert user != null;
        return user.getString("nickName");
    }
    
    public static String getUserName(String userInfo){
        JSONObject user = JSONUtils.jsonToBean(userInfo);
        assert user != null;
        return user.getString("username");
    }

    public static  Integer getUserId(String userInfo){
        JSONObject user = JSONUtils.jsonToBean(userInfo);
        assert user != null;
        return user.getInteger("userId");
    }
    
    
    public static AuthDto authDto(String userInfo){
        JwtUserDto jwtUserDto = JSON.parseObject(userInfo, JwtUserDto.class);
        return jwtUserDto.getAuthDto();
    }
    
    
    
    public static String getToken(String userInfo){
        JSONObject user = JSONUtils.jsonToBean(userInfo);
        assert user!=null;
        return "Bearer "+user.getString("token");
    }

}
