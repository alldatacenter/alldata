/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.server.service.impl;

import datart.core.base.exception.BaseException;
import datart.core.base.exception.Exceptions;
import datart.core.base.exception.ServerException;
import datart.core.common.Application;
import datart.core.entity.Organization;
import datart.core.entity.User;
import datart.security.base.InviteToken;
import datart.security.base.PasswordToken;
import datart.security.util.JwtUtils;
import datart.security.util.SecurityUtils;
import datart.server.service.BaseService;
import datart.server.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.messageresolver.SpringMessageResolver;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Date;

@Slf4j
@Component
public class MailServiceImpl extends BaseService implements MailService {

    /**
     * 邮件模板参数
     */
    private static final String USERNAME_KEY = "username";

    public static final String TOKEN_KEY = "token";

    public static final String HOST_KEY = "host";

    private static final String INVITER = "inviter";

    private static final String ORG_NAME = "orgName";

    private static final String MESSAGE = "message";

    private static final String VERIFY_CODE = "verifyCode";


    /**
     * 邮件模板
     */
    private static final String USER_ACTIVE_TEMPLATE = "mail/UserActiveMailTemplate";

    private static final String USER_INVITE_TEMPLATE = "mail/InviteMailTemplate";

    private static final String FIND_PASSWORD_TEMPLATE = "mail/RestPasswordEmailTemplate";


    private JavaMailSender mailSender;

    private final TemplateEngine templateEngine;


    @Value("${spring.mail.fromAddress:null}")
    private String fromAddress;

    @Value("${spring.mail.username:null}")
    private String username;

    @Value("${spring.mail.senderName:Datart}")
    private String senderName;

    @Value("${datart.user.active.expire-hours:48}")
    private int activeExpireHours;

    @Value("${datart.user.invite.expire-hours:48}")
    private int inviteExpireHours;

    public MailServiceImpl(TemplateEngine templateEngine, MessageSource messageSource) {
        SpringMessageResolver springMessageResolver = new SpringMessageResolver();
        springMessageResolver.setMessageSource(messageSource);
        templateEngine.setMessageResolver(springMessageResolver);
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendSimpleMail(SimpleMailMessage simpleMailMessage) {

    }

    @Override
    public void sendMimeMessage(MimeMessage mimeMessage) {
        checkMailSender();
        mailSender.send(mimeMessage);
    }

    @Override
    public MimeMessage createMimeMessage() throws MessagingException, UnsupportedEncodingException {
        checkMailSender();
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setFrom(getFromAddress(), senderName);
        return mimeMessage;
    }

    @Override
    public void sendActiveMail(User user) throws MessagingException, UnsupportedEncodingException {
        checkMailSender();
        MimeMessage activeMimeMessage = createActiveMimeMessage(user);
        mailSender.send(activeMimeMessage);
        log.info("Activate Mail was sent to {}({})", user.getUsername(), user.getEmail());
    }

    @Override
    public void sendInviteMail(User user, Organization org) throws UnsupportedEncodingException, MessagingException {
        checkMailSender();
        MimeMessage inviteMimeMessage = createInviteMimeMessage(user, org);
        mailSender.send(inviteMimeMessage);
        log.info("Invite Mail was sent to {}({})", user.getUsername(), user.getEmail());
    }

    @Override
    public void sendVerifyCode(User user) throws UnsupportedEncodingException, MessagingException {
        checkMailSender();
        MimeMessage verifyCodeMimeMessage = createVerifyCodeMimeMessage(user);
        mailSender.send(verifyCodeMimeMessage);
        log.info("Verify Code Mail was sent to {}({})", user.getUsername(), user.getEmail());
    }

    private MimeMessage createVerifyCodeMimeMessage(User user) throws UnsupportedEncodingException, MessagingException {
        Context context = new Context(LocaleContextHolder.getLocale());
        context.setVariable(VERIFY_CODE, user.getPassword());
        context.setVariable(MESSAGE, getMessages("message.user.reset.password.mail.message", user.getUsername(), SecurityUtils.VERIFY_CODE_TIMEOUT_MIN));
        String mailContent = templateEngine.process(FIND_PASSWORD_TEMPLATE, context);
        return createMimeMessage(user.getEmail(), getMessage("message.user.reset.password.mail.subject"), mailContent, true);
    }

    private MimeMessage createInviteMimeMessage(User user, Organization org) throws MessagingException, UnsupportedEncodingException {
        InviteToken inviteToken = new InviteToken();
        inviteToken.setOrgId(org.getId());
        inviteToken.setUserId(user.getId());
        inviteToken.setCreateTime(System.currentTimeMillis());
        inviteToken.setInviter(getCurrentUser().getUsername());
        inviteToken.setExp(DateUtils.addHours(new Date(), inviteExpireHours));
        String tokenString = JwtUtils.toJwtString(inviteToken);

        Context context = new Context(LocaleContextHolder.getLocale());
        context.setVariable(TOKEN_KEY, tokenString);
        context.setVariable(USERNAME_KEY, user.getUsername());
        context.setVariable(INVITER, inviteToken.getInviter());
        context.setVariable(ORG_NAME, org.getName());
        String activeUrl = Application.getWebRootURL() + "/confirminvite";
        context.setVariable(HOST_KEY, activeUrl);
        String mailContent = templateEngine.process(USER_INVITE_TEMPLATE, context);
        return createMimeMessage(user.getEmail(), getMessage("message.user.invite.mail.subject"), mailContent, true);

    }

    private MimeMessage createActiveMimeMessage(User user) throws MessagingException, UnsupportedEncodingException {

        PasswordToken passwordToken = new PasswordToken();
        passwordToken.setSubject(user.getUsername());
        passwordToken.setCreateTime(System.currentTimeMillis());
        passwordToken.setExp(DateUtils.addHours(new Date(), activeExpireHours));
        passwordToken.setPassword(user.getPassword());
        String tokenString = JwtUtils.toJwtString(passwordToken);

        Context context = new Context(LocaleContextHolder.getLocale());
        context.setVariable(USERNAME_KEY, user.getUsername());
        context.setVariable(TOKEN_KEY, tokenString);
        String activeUrl = Application.getWebRootURL() + "/activation";
        context.setVariable(HOST_KEY, activeUrl);
        String mailContent = templateEngine.process(USER_ACTIVE_TEMPLATE, context);
        return createMimeMessage(user.getEmail(), getMessage("message.user.active.mail.subject"), mailContent, true);
    }

    private MimeMessage createMimeMessage(String to, String subject, String content, boolean html) throws MessagingException, UnsupportedEncodingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setFrom(getFromAddress(), senderName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, html);
        return mimeMessage;
    }

    private String getFromAddress() throws ServerException {
        String from = StringUtils.isBlank(username) ? fromAddress : username;
        if (StringUtils.isBlank(from)) {
            log.error("The mail from address is empty" +
                    ",Please configure spring.mail.username/fromAddress in application properties");
            Exceptions.msg("The mail from address can not be empty");
        }
        return from;
    }

    @Autowired(required = false)
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void checkMailSender() {
        if (mailSender == null) {
            Exceptions.tr(BaseException.class, "message.email.sender.error");
        }
    }

}
