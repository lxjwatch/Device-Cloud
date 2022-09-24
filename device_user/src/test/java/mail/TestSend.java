package mail;

import center.misaki.device.Mail.MailServiceImpl;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Misaki
 */
public class TestSend {
    
    @Test
    public void SendTome() throws TemplateException, IOException, InterruptedException {
        Map<String, Object> map = new HashMap<>();
        map.put("name","Misaki");
        map.put("code","123456");
        MailServiceImpl mailService = new MailServiceImpl(new FreeMarkerConfigurer());
        mailService.asyncSendTemplateMail("1926653120@qq.com","验证码",map, "code.ftl");
        Thread.sleep(10000);
    }
    
    @Test
    public void TConn() throws TemplateException, IOException, InterruptedException {
        MailServiceImpl mailService = new MailServiceImpl(new FreeMarkerConfigurer());
        mailService.testConnection();
        Thread.sleep(10000);
    }
    
    
    
}
