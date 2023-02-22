package center.misaki.device.domain.Dto;

import org.springframework.security.core.userdetails.User;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author Misaki
 */
public class JwtUserDto extends User implements Serializable {

    private static final long serialVersionUID = -12;

    private final Integer userId;

    private final Integer tenementId;

    private final boolean isCreater;

    private boolean isSysAdmin;

    private boolean isNormalAdmin;

    private AuthDto authDto;

    private String token;

    public JwtUserDto(center.misaki.device.domain.Pojo.User user){
        super(user.getUsername(), user.getPwd(),!user.getIsForbidden(), !user.getIsDelete(), true,true,new ArrayList<>());
        this.userId=user.getId();
        this.tenementId=user.getTenementId();
        this.isCreater=user.getIsCreater();
    }


    public Integer getUserId() {
        return userId;
    }

    public Integer getTenementId() {
        return tenementId;
    }


    public AuthDto getAuthDto() {
        return authDto;
    }

    public void setAuthDto(AuthDto authDto) {
        this.authDto = authDto;
    }

    public boolean isCreater() {
        return isCreater;
    }

    public boolean isSysAdmin() {
        return isSysAdmin;
    }

    public void setSysAdmin(boolean sysAdmin) {
        isSysAdmin = sysAdmin;
    }

    public boolean isNormalAdmin() {
        return isNormalAdmin;
    }

    public void setNormalAdmin(boolean normalAdmin) {
        isNormalAdmin = normalAdmin;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
