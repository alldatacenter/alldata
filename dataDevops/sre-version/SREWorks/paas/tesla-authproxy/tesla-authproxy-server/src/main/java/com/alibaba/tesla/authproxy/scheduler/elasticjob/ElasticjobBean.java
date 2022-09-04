package com.alibaba.tesla.authproxy.scheduler.elasticjob;

import com.alibaba.tesla.authproxy.AuthProperties;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Configuration
public class ElasticjobBean {

    @Autowired
    private AuthProperties authProperties;

    @Bean
    @Conditional(ElasticjobCondition.class)
    public CoordinatorRegistryCenter registryCenter() {
        String zookeeperHost = authProperties.getZookeeperHost();
        String zookeeperNamespace = authProperties.getZookeeperNamespace();
        ZookeeperConfiguration configuration = new ZookeeperConfiguration(zookeeperHost, zookeeperNamespace);
        CoordinatorRegistryCenter regCenter = new ZookeeperRegistryCenter(configuration);
        regCenter.init();
        return regCenter;
    }

}
