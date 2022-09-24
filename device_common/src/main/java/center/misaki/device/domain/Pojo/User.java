package center.misaki.device.domain.Pojo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;


@Getter
@Setter
public class User implements Serializable{

    @TableId(type= IdType.AUTO)
    private Integer id;
    private Integer tenementId;
    private String username;
    
    private String nickName;
    private String pwd;
    
    private Integer state;
    
    private String email;
    private String wxOpenId;
    
    private String gender;
    
    private String phone;
    
    private Integer normalAdminGroupId;
    
    private Boolean isCreater;    
    
    private Boolean isDelete;
    
    private Boolean isForbidden;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return this.username.equals(user.getUsername())&&this.pwd.equals(user.getPwd())
                || this.wxOpenId.equals(user.getWxOpenId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(username,id);
    }
}
