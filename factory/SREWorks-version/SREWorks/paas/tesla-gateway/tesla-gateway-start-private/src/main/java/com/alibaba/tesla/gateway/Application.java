package com.alibaba.tesla.gateway;

import com.alibaba.tesla.gateway.server.nacos.TeslaNacosRibbonClientConfiguration;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan(
    basePackages = {"com.alibaba.tesla.common", "com.alibaba.tesla.gateway"},
    excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = TeslaNacosRibbonClientConfiguration.class)
})
@EnableDiscoveryClient
@RibbonClients( defaultConfiguration = TeslaNacosRibbonClientConfiguration.class)
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        try {
            SpringApplication.run(Application.class, args);
        } catch (Exception e) {
            System.out.println(ExceptionUtils.getStackTrace(e));
            System.exit(1);
        }
    }
}
