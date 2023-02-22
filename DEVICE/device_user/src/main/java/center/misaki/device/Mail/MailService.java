package center.misaki.device.Mail;

import java.util.Map;

public interface MailService {

    /**
     * Send a simple email
     *
     * @param to recipient
     * @param subject subject
     * @param content content
     */
    void sendTextMail(String to, String subject, String content);

    /**
     * Send a email with html
     *
     * @param to recipient
     * @param subject subject
     * @param content content
     * @param templateName template name
     */
    void asyncSendTemplateMail(String to, String subject, Map<String, Object> content,
                               String templateName);

    void sycSendTemplateMail(String to, String subject, Map<String, Object> content,
                              String templateName);
    /**
     * Send mail with attachments
     *
     * @param to recipient
     * @param subject subject
     * @param content content
     * @param templateName template name
     * @param attachFilePath attachment full path name
     */
    void sendAttachMail(String to, String subject, Map<String, Object> content, String templateName,
                        String attachFilePath);

    /**
     * Test email server connection.
     */
    void testConnection();
}
