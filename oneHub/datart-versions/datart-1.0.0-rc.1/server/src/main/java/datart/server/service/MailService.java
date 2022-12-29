package datart.server.service;

import datart.core.entity.Organization;
import datart.core.entity.User;
import org.springframework.mail.SimpleMailMessage;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

public interface MailService {

    void sendSimpleMail(SimpleMailMessage simpleMailMessage);

    void sendMimeMessage(MimeMessage mimeMessage);

    MimeMessage createMimeMessage() throws MessagingException, UnsupportedEncodingException;

    void sendActiveMail(User user) throws MessagingException, UnsupportedEncodingException;

    void sendInviteMail(User user, Organization organization) throws UnsupportedEncodingException, MessagingException;

    void sendVerifyCode(User user) throws UnsupportedEncodingException, MessagingException;

}
