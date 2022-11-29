package cn.datax.service.email.service;

import cn.datax.common.base.BaseService;
import cn.datax.service.email.api.dto.EmailDto;
import cn.datax.service.email.api.entity.EmailEntity;

import java.util.List;

public interface EmailService extends BaseService<EmailEntity> {

    EmailEntity getEmailById(String id);

    void saveEmail(EmailDto emailDto);

    void updateEmail(EmailDto emailDto);

    void deleteEmailById(String id);

    void deleteEmailBatch(List<String> ids);

    void sendEmail(String id);
}
