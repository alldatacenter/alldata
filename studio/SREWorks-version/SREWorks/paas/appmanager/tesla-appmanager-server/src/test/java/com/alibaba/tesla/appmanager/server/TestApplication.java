package com.alibaba.tesla.appmanager.server;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 测试 Application
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories("com.alibaba.tesla.dag.model.repository")
@EntityScan("com.alibaba.tesla.dag.model.domain")
@ComponentScan(basePackages = {"com.alibaba.tesla"})
@MapperScan(basePackages = {
        "com.alibaba.tesla.appmanager.server.repository.mapper",
        "com.alibaba.tesla.appmanager.dynamicscript.repository.mapper",
        "com.alibaba.tesla.appmanager.trait.repository.mapper",
        "com.alibaba.tesla.appmanager.definition.repository.mapper",
        "com.alibaba.tesla.appmanager.meta.helm.repository.mapper",
        "com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.mapper",
        "com.alibaba.tesla.appmanager.deployconfig.repository.mapper",
        "com.alibaba.tesla.appmanager.workflow.repository.mapper",
        "com.alibaba.tesla.dag.repository.mapper",
        "com.alibaba.tesla.appmanager.plugin.repository.mapper",
})
@EnableTransactionManagement
@EnableAsync
@Slf4j
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
