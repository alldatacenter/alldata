/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.email;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-12-4
 */
public class SendMail {

    private static final Logger logger = LoggerFactory.getLogger(SendMail.class);

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {

    }

    // // 邮箱服务器
    // private String host = "smtp.aliyun.com";
    // // // 这个是你的邮箱用户名
    // private String username = "baisui@2dfire.com";
    // // 你的邮箱密码:
    // private String password = "flzxsqc85815545!";
    //
    // private String mail_from = "baisui@2dfire.com";
    // 邮箱服务器
    private String host = "smtp.126.com";

    // // 这个是你的邮箱用户名
    private String username = "mozhenghua19811109@126.com";
    // 你的邮箱密码:
    private String password = "";

    private String mail_from = "mozhenghua19811109@126.com";

    // private String host = "vpn.gecareers.cn";
    // // // 这个是你的邮箱用户名
    // private String username = "tis@2dfire.tech";
    // // 你的邮箱密码:
    // private String password = "nX2izEe4xEmkQP";
    //
    // private String mail_from = "tis@2dfire.tech";
    private String personalName = "TIS日报表";

    private static final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";

    /**
     * 此段代码用来发送普通电子邮件
     */
    /**
     * @param subject
     * @param content
     * @throws Exception
     */
    public void send(String subject, String content, String mail_to2) throws Exception {
        if (1 == 1) {
            // FIXME 功能还未开放，先跳过
            return;
        }
        try {
            // 获取系统环境
            Properties props = new Properties();
            // 进行邮件服务器用户认证
            Authenticator auth = new Email_Autherticator();
            props.put("mail.smtp.host", host);
            props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
            props.setProperty("mail.smtp.socketFactory.fallback", "false");
            props.setProperty("mail.smtp.port", "465");
            props.setProperty("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.auth", "true");
            Session session = Session.getDefaultInstance(props, auth);
            // session.setDebug(true);
            MimeMessage message = new MimeMessage(session);
            // message.setHeader("Content-Type", "text/html;charset=utf8");
            // message.setHeader("Content-Transfer-Encoding", "utf8");
            // 设置邮件主题
            message.setSubject(MimeUtility.encodeText(subject, "gb2312", "B"));
            // message.setHeader(name, value); // 设置邮件正文
            // message.setHeader(mail_head_name, mail_head_value); // 设置邮件标题
            // 设置邮件发送日期
            message.setSentDate(new Date());
            Address address = new InternetAddress(mail_from, personalName);
            // 设置邮件发送者的地址
            message.setFrom(address);
            message.setContent(content, "text/html;charset=gb2312");
            logger.info("sendto email:" + mail_to2);
            String[] destAddress = StringUtils.split(mail_to2, ",");
            for (String to : destAddress) {
                Address toAddress2 = new InternetAddress(to);
                message.addRecipient(Message.RecipientType.TO, toAddress2);
            }
            // 发送邮件
            Transport.send(message);
            System.out.println("send ok!");
        } catch (Exception ex) {
            throw new Exception(ex.getMessage(), ex);
        }
    }

    /**
     * 用来进行服务器对用户的认证
     */
    private class Email_Autherticator extends Authenticator {

        public Email_Autherticator() {
            super();
        }

        // Email_Autherticator(String user, String pwd) {
        // super();
        // username = user;
        // password = pwd;
        // }
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
        }
    }
}
