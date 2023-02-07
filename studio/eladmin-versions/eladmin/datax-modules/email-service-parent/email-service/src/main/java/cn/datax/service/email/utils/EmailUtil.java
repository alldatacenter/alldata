package cn.datax.service.email.utils;

import cn.datax.service.email.api.entity.EmailEntity;
import cn.hutool.core.collection.CollUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.List;

@Component
public class EmailUtil {

    @Value("${spring.mail.username}")
    private String from;

    @Autowired
    private JavaMailSender mailSender;

    public void sendEmail(EmailEntity email) throws Exception {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setFrom(from);
        helper.setSubject(email.getSubject());
        helper.setText(email.getText(),true);
        List<String> tos = email.getTos();
        helper.setTo((String[])tos.toArray(new String[tos.size()]));
        List<String> ccs = email.getCcs();
        if(CollUtil.isNotEmpty(ccs)){
            helper.setCc((String[])ccs.toArray(new String[ccs.size()]));
        }
        List<String> bccs = email.getBccs();
        if(CollUtil.isNotEmpty(bccs)){
            helper.setBcc((String[])bccs.toArray(new String[bccs.size()]));
        }
        List<File> files = email.getFiles();
        if(CollUtil.isNotEmpty(files)){
            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                helper.addAttachment(file.getName(), file);
            }
        }
        mailSender.send(mimeMessage);
    }
}
