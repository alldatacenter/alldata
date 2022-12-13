package com.platform.dts.core.conf;

import com.platform.dts.mapper.*;
import com.platform.dts.core.scheduler.JobScheduler;
import com.platform.dts.mapper.JobDatasourceMapper;
import com.platform.dts.mapper.JobGroupMapper;
import com.platform.dts.mapper.JobInfoMapper;
import com.platform.dts.mapper.JobLogMapper;
import com.platform.dts.mapper.JobLogReportMapper;
import com.platform.dts.mapper.JobRegistryMapper;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;


/**
 *
 * @author AllDataDC
 * @date 2022/11/16 11:14
 * @Description: xxl-job config
 **/
@Component
public class JobAdminConfig implements InitializingBean, DisposableBean {

    private static JobAdminConfig adminConfig = null;

    public static JobAdminConfig getAdminConfig() {
        return adminConfig;
    }


    // ---------------------- XxlJobScheduler ----------------------

    private JobScheduler xxlJobScheduler;

    @Override
    public void afterPropertiesSet() throws Exception {
        adminConfig = this;

        xxlJobScheduler = new JobScheduler();
        xxlJobScheduler.init();
    }

    @Override
    public void destroy() throws Exception {
        xxlJobScheduler.destroy();
    }


    // ---------------------- XxlJobScheduler ----------------------

    // conf
    @Value("${eladmin.job.i18n}")
    private String i18n;

    @Value("${eladmin.job.accessToken}")
    private String accessToken;

    @Value("${spring.mail.username}")
    private String emailUserName;

    @Value("${eladmin.job.triggerpool.fast.max}")
    private int triggerPoolFastMax;

    @Value("${eladmin.job.triggerpool.slow.max}")
    private int triggerPoolSlowMax;

    @Value("${eladmin.job.logretentiondays}")
    private int logretentiondays;

    @Value("${datasource.aes.key}")
    private String dataSourceAESKey;

    // dao, service

    @Resource
    private JobLogMapper jobLogMapper;
    @Resource
    private JobInfoMapper jobInfoMapper;
    @Resource
    private JobRegistryMapper jobRegistryMapper;
    @Resource
    private JobGroupMapper jobGroupMapper;
    @Resource
    private JobLogReportMapper jobLogReportMapper;
    @Resource
    private JavaMailSender mailSender;
    @Resource
    private DataSource dataSource;
    @Resource
    private JobDatasourceMapper jobDatasourceMapper;

    public String getI18n() {
        return i18n;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getEmailUserName() {
        return emailUserName;
    }

    public int getTriggerPoolFastMax() {
        return triggerPoolFastMax < 200 ? 200 : triggerPoolFastMax;
    }

    public int getTriggerPoolSlowMax() {
        return triggerPoolSlowMax < 100 ? 100 : triggerPoolSlowMax;
    }

    public int getLogretentiondays() {
        return logretentiondays < 7 ? -1 : logretentiondays;
    }

    public JobLogMapper getJobLogMapper() {
        return jobLogMapper;
    }

    public JobInfoMapper getJobInfoMapper() {
        return jobInfoMapper;
    }

    public JobRegistryMapper getJobRegistryMapper() {
        return jobRegistryMapper;
    }

    public JobGroupMapper getJobGroupMapper() {
        return jobGroupMapper;
    }

    public JobLogReportMapper getJobLogReportMapper() {
        return jobLogReportMapper;
    }

    public JavaMailSender getMailSender() {
        return mailSender;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public JobDatasourceMapper getJobDatasourceMapper() {
        return jobDatasourceMapper;
    }

    public String getDataSourceAESKey() {
        return dataSourceAESKey;
    }

    public void setDataSourceAESKey(String dataSourceAESKey) {
        this.dataSourceAESKey = dataSourceAESKey;
    }
}
