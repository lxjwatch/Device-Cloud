package center.misaki.device.Mail;

import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.Assert;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Misaki
 */
@Slf4j
public abstract  class AbstractMailService implements MailService {
    
    private static final int DEFAULT_POOL_SIZE = 3;

    private JavaMailSender cachedMailSender;

    private MailProperties cachedMailProperties;

    private static final String cachedFromName="DEVICE";

    @Nullable
    private ExecutorService executorService;

    protected AbstractMailService(){
    }

    @NonNull
    public ExecutorService getExecutorService() {
        if (this.executorService == null) {
            this.executorService = new ThreadPoolExecutor(DEFAULT_POOL_SIZE,2*DEFAULT_POOL_SIZE,
                    100, TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>()
                    ,new DefaultThreadFactory("mailService"), new ThreadPoolExecutor.CallerRunsPolicy());
        }
        return executorService;
    }

    public void setExecutorService(@Nullable ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Test connection with email server.
     */
    @Override
    public void testConnection() {
        JavaMailSender javaMailSender = getMailSender();
        if (javaMailSender instanceof JavaMailSenderImpl) {
            JavaMailSenderImpl mailSender = (JavaMailSenderImpl) javaMailSender;
            try {
                mailSender.testConnection();
            } catch (Throwable e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }


    /**
     * Get java mail sender.
     *
     * @return java mail sender
     */
    @NonNull
    private synchronized JavaMailSender getMailSender() {
        if (this.cachedMailSender == null) {
            // create mail sender factory
            MailSenderFactory mailSenderFactory = new MailSenderFactory();
            // get mail sender
            this.cachedMailSender = mailSenderFactory.getMailSender(getMailProperties());
        }

        return this.cachedMailSender;
    }

    /**
     * 发送邮件，外界调用函数
     * @param callback callback message handler
     */
    protected void AnsycSendMailTemplate(@Nullable Consumer<MimeMessageHelper> callback) {
        ExecutorService executorService = getExecutorService();
        executorService.execute(()->sendMailTemplate(callback));
    }
    
    protected void sychSendMailTemplate(@Nullable Consumer<MimeMessageHelper> callback) {
        sendMailTemplate(callback);
    }


    /**
     * 实际发送邮件，目标函数
     * @param callback mime message callback.
     */
    protected void sendMailTemplate(@Nullable Consumer<MimeMessageHelper> callback) {
        if (callback == null) {
            log.info("Callback is null, skip to send email");
            return;
        }

        // get mail sender
        JavaMailSender mailSender = getMailSender();

        // create mime message helper
        MimeMessageHelper messageHelper = new MimeMessageHelper(mailSender.createMimeMessage());

        try {
            // set from-name
            messageHelper.setFrom(getFromAddress(mailSender));
            // 回掉单独处理消息集
            callback.accept(messageHelper);

            // get mime message
            MimeMessage mimeMessage = messageHelper.getMimeMessage();
            // send email
            mailSender.send(mimeMessage);

            log.info("Sent an email to [{}] successfully, subject: [{}], sent date: [{}]",
                    Arrays.toString(mimeMessage.getAllRecipients()),
                    mimeMessage.getSubject(),
                    mimeMessage.getSentDate());
        } catch (Exception e) {
            throw new RuntimeException("邮件发送失败，请检查 SMTP 服务配置是否正确", e);
        }
    }

    /**
     * 拿到地址
     * @param javaMailSender java mail sender.
     * @return from-name internet address
     * @throws UnsupportedEncodingException throws when you give a wrong character encoding
     */
    private synchronized InternetAddress getFromAddress(@NonNull JavaMailSender javaMailSender)
            throws UnsupportedEncodingException {
        Assert.notNull(javaMailSender, "Java mail sender must not be null");

        if (javaMailSender instanceof JavaMailSenderImpl) {
            // get user name(email)
            JavaMailSenderImpl mailSender = (JavaMailSenderImpl) javaMailSender;
            String username = mailSender.getUsername();

            // build internet address
            return new InternetAddress(username, cachedFromName,
                    mailSender.getDefaultEncoding());
        }

        throw new UnsupportedOperationException(
                "Unsupported java mail sender: " + javaMailSender.getClass().getName());
    }


    /**
     * Get mail properties.
     *
     * @return mail properties
     */
    @NonNull
    private synchronized MailProperties getMailProperties() {
        if (cachedMailProperties == null) {
            // create mail properties
            this.cachedMailProperties = new MailProperties(log.isDebugEnabled());
        }

        return this.cachedMailProperties;
    }


    

}
