/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.notification.plugin.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.mail.smtp.SMTPProvider;
import io.datavines.common.utils.JSONUtils;
import io.datavines.common.utils.StringUtils;
import io.datavines.notification.api.entity.SlaNotificationResultRecord;
import io.datavines.notification.api.entity.SlaSenderMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.mail.HtmlEmail;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import java.util.*;

import static java.util.Objects.requireNonNull;

@Slf4j
@EqualsAndHashCode
@Data
public class EMailSender {

    private String mailProtocol = "SMTP";

    private String mailSmtpHost;

    private String mailSmtpPort;

    private String mailSenderEmail;

    private String enableSmtpAuth;

    private String mailUser;

    private String mailPasswd;

    private String mailUseStartTLS;

    private String mailUseSSL;

    private String sslTrust;

    private String mustNotNull = "must not be null";

    public EMailSender(SlaSenderMessage senderMessage) {

        String configString = senderMessage.getConfig();
        Map<String, String> config = JSONUtils.toMap(configString);

        mailSmtpHost = config.get("serverHost");
        requireNonNull(mailSmtpHost, "serverHost" + mustNotNull);

        mailSmtpPort = config.get("serverPort");
        requireNonNull(mailSmtpPort, "serverPort" + mustNotNull);

        mailSenderEmail = config.get("sender");
        requireNonNull(mailSenderEmail, "sender" + mustNotNull);

        enableSmtpAuth = config.get("enableSmtpAuth");

        mailUser = config.get("user");
        requireNonNull(mailUser, "user" + mustNotNull);

        mailPasswd = config.get("passwd");
        requireNonNull(mailPasswd, "passwd" + mustNotNull);

        mailUseStartTLS = config.get("starttlsEnable");
        requireNonNull(mailUseStartTLS, "starttlsEnable" + mustNotNull);

        mailUseSSL = config.get("sslEnable");
        requireNonNull(mailUseSSL, "sslEnable" + mustNotNull);

        sslTrust = config.get("smtpSslTrust");
        requireNonNull(sslTrust, "smtpSslTrust" + mustNotNull);
    }

    public SlaNotificationResultRecord sendMails(Set<String> receiverSet, Set<String> copyReceiverSet, String subject, String message){
        SlaNotificationResultRecord result = new SlaNotificationResultRecord();
        // if there is no receivers && no receiversCc, no need to process
        if (CollectionUtils.isEmpty(receiverSet)) {
            return result;
        }
        receiverSet.removeIf(StringUtils::isEmpty);
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        // send email
        HtmlEmail email = new HtmlEmail();
        try {
            Session session = getSession();
            email.setMailSession(session);
            email.setFrom(mailSenderEmail);
            email.setCharset("UTF-8");
            if (CollectionUtils.isNotEmpty(receiverSet)) {
                // receivers mail
                for (String receiver : receiverSet) {
                    email.addTo(receiver);
                }
            }
            if (CollectionUtils.isNotEmpty(copyReceiverSet)) {
                //cc
                for (String receiverCc : copyReceiverSet) {
                    email.addCc(receiverCc);
                }
            }
            // sender mail
            String textMessageContent = getTextTypeMessage(message);
            email.setMsg(textMessageContent);
            email.setSubject(subject);
            // send
            email.setDebug(true);
            email.send();
            result.setStatus(true);
            return result;

        } catch (Exception e) {
            log.error("email send error", e);
        }
        return result;
    }

    private Session getSession() {
        // support multiple email format
        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
        mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
        CommandMap.setDefaultCommandMap(mc);

        Properties props = new Properties();
        props.setProperty("mail.smtp.host", mailSmtpHost);
        props.setProperty("mail.smtp.port", mailSmtpPort);
        props.setProperty("mail.smtp.auth", enableSmtpAuth);
        props.setProperty("mail.transport.protocol", mailProtocol);
        props.setProperty("mail.smtp.starttls.enable", mailUseStartTLS);
        props.setProperty("mail.smtp.ssl.enable", mailUseSSL);
        props.setProperty("mail.smtp.ssl.trust", sslTrust);

        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                // mail username and password
                return new PasswordAuthentication(mailUser, mailPasswd);
            }
        };

        Session session = Session.getInstance(props, auth);
        session.addProvider(new SMTPProvider());
        return session;
    }

    private String getTextTypeMessage(String content) {
        if (StringUtils.isNotEmpty(content)) {
            ArrayNode list = JSONUtils.parseArray(content);
            StringBuilder contents = new StringBuilder(100);
            for (JsonNode jsonNode : list) {
                contents.append(EmailConstants.TR);
                contents.append(EmailConstants.TD).append(jsonNode.toString().replace("\"", "")).append(EmailConstants.TD_END);
                contents.append(EmailConstants.TR_END);
            }
            return EmailConstants.HTML_HEADER_PREFIX + contents.toString() + EmailConstants.TABLE_HTML_TAIL + EmailConstants.BODY_HTML_TAIL;
        }
        return content;
    }
}
