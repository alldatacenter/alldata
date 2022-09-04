package com.alibaba.tesla.authproxy.service.job.elasticjob;

import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyThirdPartyError;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateValidationError;
import com.alibaba.tesla.authproxy.model.mapper.OplogMapper;
import com.alibaba.tesla.authproxy.model.mapper.UserMapper;
import com.alibaba.tesla.authproxy.model.OplogDO;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.service.PrivateAccountService;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import com.alibaba.tesla.authproxy.util.PasswordUtil;
import com.alibaba.tesla.authproxy.util.TeslaUtils;
import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * ElasticJob 定时调度 - 对已经过期的密码进行强制重置操作
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class ResetExpiredAccountJob implements SimpleJob {

    private static final String TEMP_PASSWORD_PREFIX = "_TEMP_";

    private PasswordUtil passwordUtil = new PasswordUtil.PasswordUtilBuilder()
            .useDigits(true)
            .useLower(true)
            .useUpper(true)
            .build();

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TeslaUserService teslaUserService;

    @Autowired
    private OplogMapper oplogMapper;

    @Autowired
    private PrivateAccountService privateAccountService;

    @Autowired
    private AuthProperties authProperties;

    @Override
    public void execute(ShardingContext context) {
        if (!Constants.ENVIRONMENT_DXZ.equals(authProperties.getEnvironment())) {
            return;
        }
        try {
            if (!TeslaUtils.isPrimaryCluster()) {
                log.info("[ElasticJob-ResetExpiredAccountJob] Not primary cluster, skip");
                return;
            }
        } catch (UnknownHostException ignored) {}

        log.info("[ElasticJob] Prepare to reset expired account password");
        LocalDateTime now = LocalDateTime.now();
        List<UserDO> userDos = teslaUserService.selectByName("");
        for (UserDO userDo : userDos) {
            OplogDO op = oplogMapper.getLastByUserAndAction(userDo.getLoginName(),
                    Constants.OP_PASSWORD_CHANGE);
            if (null == op) {
                continue;
            }
            LocalDateTime createDate = op.getGmtCreate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime expireDate = createDate.plusMonths(authProperties.getAasPasswordExpireMonths());
            if (now.isAfter(expireDate)) {
                String loginName = userDo.getLoginName().trim();
                if (loginName.equals(authProperties.getAasSuperUser().trim())) {
                    continue;
                }
                String randomPassword = TEMP_PASSWORD_PREFIX + passwordUtil.generate(10);
                log.info("Prepare to reset user {} password into {}", loginName, randomPassword);
                try {
                    privateAccountService.passwordChange(loginName, randomPassword);
                } catch (PrivateValidationError | AuthProxyThirdPartyError e) {
                    log.error("Reset password failed, user={}, password={}", loginName, randomPassword);
                    continue;
                }
                teslaUserService.update(userDo);
                log.info("[ElasticJob] Reset account {} password", userDo.getLoginName());
            }
        }
        log.info("[ElasticJob] Finished reset expired account password");
    }
}
