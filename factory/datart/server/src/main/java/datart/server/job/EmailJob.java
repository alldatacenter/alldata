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
package datart.server.job;

import datart.core.base.consts.AttachmentType;
import datart.core.common.Application;
import datart.server.base.dto.JobFile;
import datart.server.base.dto.ScheduleJobConfig;
import datart.server.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.CollectionUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.List;

@Slf4j
public class EmailJob extends ScheduleJob {

    private final MailService mailService;

    private final String imageHtml = "<img src='cid:$CID$' style='width:100%;height:auto;max-width:100%;display:block'>";

    public EmailJob() {
        mailService = Application.getBean(MailService.class);
    }

    @Override
    public void doSend() throws Exception {
        MimeMessage mimeMessage = createMailMessage(jobConfig, attachments);
        mailService.sendMimeMessage(mimeMessage);
    }

    private MimeMessage createMailMessage(ScheduleJobConfig config, List<JobFile> attachments) throws MessagingException, UnsupportedEncodingException {
        MimeMessage mimeMessage = mailService.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, !CollectionUtils.isEmpty(attachments));
        helper.setSubject(config.getSubject());
        helper.setTo(config.getTo() == null ? null : config.getTo().split(";"));
        if (StringUtils.isNotBlank(config.getCc())) {
            helper.setCc(config.getCc() == null ? null : config.getCc().split(";"));
        }

        String imageStr = buildMailImageContent(attachments);
        helper.setText(config.getTextContent()+imageStr, true);

        putFileIntoMail(helper, attachments);
        return mimeMessage;
    }

    /**
     * 构造图片内嵌html
     * @param attachments
     */
    private String buildMailImageContent(List<JobFile> attachments) {
        StringBuilder builder = new StringBuilder();
        for (JobFile jobFile : attachments) {
            if (jobFile.getType().equals(AttachmentType.IMAGE)) {
                builder.append("<hr><h6>"+jobFile.getFile().getName()+"<h6>");
                builder.append(imageHtml.replace("$CID$", jobFile.getFile().getName()));
            }
        }
        return builder.toString();
    }

    private void putFileIntoMail(MimeMessageHelper helper, List<JobFile> attachments) throws MessagingException {
        if (CollectionUtils.isEmpty(attachments)) {
            return;
        }
        for (JobFile jobFile : attachments) {
            switch (jobFile.getType()) {
                case IMAGE:
                    helper.addInline(jobFile.getFile().getName(), jobFile.getFile());
                    break;
                case EXCEL:
                case PDF:
                default:
                    helper.addAttachment(jobFile.getFile().getName(), jobFile.getFile());
                    break;
            }
        }
    }


}
