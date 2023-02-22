package center.misaki.device.Config;

import center.misaki.device.Auth.SecurityUtils;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * @author Misaki
 * 拦截 Mybatis中 通用审计字段
 */
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        boolean createDate = metaObject.hasSetter("createTime");
        boolean updateDate = metaObject.hasSetter("updateTime");
        boolean createPerson = metaObject.hasSetter("createPerson");
        boolean updatePerson = metaObject.hasSetter("updatePerson");
        if(createDate||updateDate){
            LocalDateTime now = LocalDateTime.now();
            if(createDate) this.setFieldValByName("createTime",now,metaObject);
            if(updateDate)  this.setFieldValByName("updateTime",now,metaObject);
        }
        if(createPerson||updatePerson){
            String username = SecurityUtils.getCurrentUsername();
            if(createPerson)this.setFieldValByName("createPerson",username,metaObject);
            if(updatePerson)this.setFieldValByName("updatePerson",username,metaObject);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        boolean updateDate = metaObject.hasSetter("updateTime");
        boolean updatePerson = metaObject.hasSetter("updatePerson");
        if(updateDate) {
            LocalDateTime now = LocalDateTime.now();
            this.setFieldValByName("updateTime", now, metaObject);
        }
        if(updatePerson){
            String username = SecurityUtils.getCurrentUsername();
            this.setFieldValByName("updatePerson",username,metaObject);
        }
        
    }
}
