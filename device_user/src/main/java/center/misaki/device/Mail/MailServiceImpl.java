package center.misaki.device.Mail;

import center.misaki.device.userApp;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import javax.mail.MessagingException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * @author Misaki
 */
@Service
public class MailServiceImpl extends AbstractMailService{

    private final FreeMarkerConfigurer freeMarker;

    public MailServiceImpl(FreeMarkerConfigurer freeMarker){
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_28);
        configuration.setClassForTemplateLoading(userApp.class,"/templates");
        freeMarker.setConfiguration(configuration);
       this.freeMarker=freeMarker;
    }

    @Override
    public void sendTextMail(String to, String subject, String content) {
        AnsycSendMailTemplate(messageHelper->{
            try{
                messageHelper.setSubject(subject);
                messageHelper.setTo(to);
                messageHelper.setText(content);
            }catch (MessagingException e) {
                throw new RuntimeException("Failed to set message subject, to or test!", e);
            }
        });
    }

    @Override
    public void asyncSendTemplateMail(String to, String subject, Map<String, Object> content, String templateName) {
        AnsycSendMailTemplate(messageHelper -> {
            try {
                Template template = freeMarker.getConfiguration().getTemplate(templateName);
                String contentResult = FreeMarkerTemplateUtils.processTemplateIntoString(template,
                        content);
                messageHelper.setSubject(subject);
                messageHelper.setTo(to);
                messageHelper.setText(contentResult, true);
            }catch (IOException | TemplateException e) {
                throw new RuntimeException("Failed to convert template to html!", e);
            } catch (MessagingException e) {
                throw new RuntimeException("Failed to set message subject, to or test", e);
            }
        });
    }

    @Override
    public void sycSendTemplateMail(String to, String subject, Map<String, Object> content, String templateName) {
        sychSendMailTemplate(messageHelper -> {
            try {
                Template template = freeMarker.getConfiguration().getTemplate(templateName);
                String contentResult = FreeMarkerTemplateUtils.processTemplateIntoString(template,
                        content);
                messageHelper.setSubject(subject);
                messageHelper.setTo(to);
                messageHelper.setText(contentResult, true);
            }catch (IOException | TemplateException e) {
                throw new RuntimeException("Failed to convert template to html!", e);
            } catch (MessagingException e) {
                throw new RuntimeException("Failed to set message subject, to or test", e);
            }
        });
    }


    @Override
    public void sendAttachMail(String to, String subject, Map<String, Object> content, String templateName, String attachFilePath) {
        AnsycSendMailTemplate(messageHelper->{
            try {
                messageHelper.setSubject(subject);
                messageHelper.setTo(to);
                Path attachmentPath = Paths.get(attachFilePath);
                messageHelper.addAttachment(attachmentPath.getFileName().toString(),
                        attachmentPath.toFile());
            }catch (MessagingException e) {
                throw new RuntimeException("Failed to set message subject, to or test", e);
            }
        });
    }

    @Override
    public void testConnection() {
        super.testConnection();
    }
    
    
}
