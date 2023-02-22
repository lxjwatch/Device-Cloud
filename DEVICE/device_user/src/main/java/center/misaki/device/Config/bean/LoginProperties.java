package center.misaki.device.Config.bean;

import lombok.Data;

@Data
public class LoginProperties {

    /**
     * 账号单用户 登录
     */
    private boolean singleLogin = false;
    private boolean wxSingleLogin=false;
    public boolean isSingleLogin() {
        return singleLogin;
    }
    
    public boolean isWxSingleLogin(){
        return wxSingleLogin;
    }

}
