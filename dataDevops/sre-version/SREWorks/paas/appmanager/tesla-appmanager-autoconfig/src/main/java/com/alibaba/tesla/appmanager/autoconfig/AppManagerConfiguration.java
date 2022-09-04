package com.alibaba.tesla.appmanager.autoconfig;

import lombok.Data;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
@EnableConfigurationProperties({
        AppNacosProperties.class,
        PackageProperties.class,
        RedisProperties.class,
        ClusterProperties.class,
        SystemProperties.class,
        AuthProperties.class,
        ImageBuilderProperties.class,
        ThreadPoolProperties.class,
})
@Component
@ComponentScan(value = {"com.alibaba.tesla.appmanager.autoconfig"})
public class AppManagerConfiguration {
}

