package com.alibaba.tesla.authproxy.scheduler.elasticjob;

import com.alibaba.tesla.authproxy.service.job.elasticjob.ResetExpiredAccountJob;
import com.alibaba.tesla.authproxy.service.job.elasticjob.SyncOamRoleJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * ElasticJob - 任务调度器配置及 Bean 初始化
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Configuration
@Conditional(ElasticjobCondition.class)
public class ElasticjobConfig {

    @Autowired
    private CoordinatorRegistryCenter registryCenter;

    @Autowired
    private ResetExpiredAccountJob resetExpiredAccountJob;

    @Autowired
    private SyncOamRoleJob syncOamRoleJob;

    @PostConstruct
    public void resetExpiredAccountJob() {
        JobCoreConfiguration simpleCoreConfig = JobCoreConfiguration
                .newBuilder("resetExpiredAccountJob", "0 0/30 * * * ?", 1).build();
        SimpleJobConfiguration simpleJobConfig = new SimpleJobConfiguration(simpleCoreConfig,
                ResetExpiredAccountJob.class.getCanonicalName());
        LiteJobConfiguration configuration = LiteJobConfiguration.newBuilder(simpleJobConfig).overwrite(true).build();
        new SpringJobScheduler(this.resetExpiredAccountJob, this.registryCenter, configuration).init();
    }

    @PostConstruct
    public void syncToOamRoleJob() {
        JobCoreConfiguration simpleCoreConfig = JobCoreConfiguration
            .newBuilder("syncToOamRoleJob", "0 0/10 * * * ?", 1).build();
        SimpleJobConfiguration simpleJobConfig = new SimpleJobConfiguration(simpleCoreConfig,
            ResetExpiredAccountJob.class.getCanonicalName());
        LiteJobConfiguration configuration = LiteJobConfiguration.newBuilder(simpleJobConfig).overwrite(true).build();
        new SpringJobScheduler(this.syncOamRoleJob, this.registryCenter, configuration).init();
    }
}
