package mail;

import center.misaki.device.Mail.MailServiceImpl;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.IOException;
import java.util.Arrays;
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
    public boolean lemonadeChange(String str) {
        int count_five = 0;
        int[] arr = Arrays.stream(str.split(" ")).mapToInt(Integer::parseInt).toArray();
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == 5) {
                count_five++;
            } else if (arr[i] == 10) {
                if (count_five > 0) {
                    count_five--;
                } else {
                    return false;
                }
            } else if (arr[i] == 20) {
                if (count_five >= 1 && (arr[i] - 5) <= (count_five * 5)) {
                    count_five--;
                    if (arr[i] - 5 > 0) {
                        count_five -= (arr[i] - 5) / 5;
                    }
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    public void TConn() throws TemplateException, IOException, InterruptedException {

        System.out.println(lemonadeChange("5 5 5 10 20"));
    }
    
    
    
}
