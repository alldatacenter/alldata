package com.alibaba.tesla.authproxy.util.audit;

import com.alibaba.tesla.authproxy.model.UserDO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 审计工具类，用于打审计日志
 */
@Component
@Slf4j
public class AuditUtil {

    private Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

    public void debug(UserDO user, AuditTargetEnum target, AuditActionEnum action, AuditOutcomeEnum outcome,
                      String message, AuditReasonEnum reason) {
        log.debug(gson.toJson(getMessage(user, target, action, outcome, message, reason)));
    }

    public void info(UserDO user, AuditTargetEnum target, AuditActionEnum action, AuditOutcomeEnum outcome,
                     String message, AuditReasonEnum reason) {
        log.info(gson.toJson(getMessage(user, target, action, outcome, message, reason)));
    }

    public void warn(UserDO user, AuditTargetEnum target, AuditActionEnum action, AuditOutcomeEnum outcome,
                     String message, AuditReasonEnum reason) {
        log.warn(gson.toJson(getMessage(user, target, action, outcome, message, reason)));
    }

    public void error(UserDO user, AuditTargetEnum target, AuditActionEnum action, AuditOutcomeEnum outcome,
                      String message, AuditReasonEnum reason) {
        log.error(gson.toJson(getMessage(user, target, action, outcome, message, reason)));
    }

    private AuditMessage getMessage(UserDO user, AuditTargetEnum target, AuditActionEnum action,
                                    AuditOutcomeEnum outcome, String message, AuditReasonEnum reason) {
        String userStr = user == null ? "NULL" : user.getLoginName();
        return AuditMessage.builder()
                .action(action.toString())
                .user(userStr)
                .target(target.toString())
                .outcome(outcome.toString())
                .reason(reason.toString())
                .message(message)
                .build();
    }

}

