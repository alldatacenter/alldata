package com.alibaba.sreworks.job;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@SpringBootApplication(scanBasePackages = {"com.alibaba.sreworks", "com.alibaba.tesla"})
@EnableScheduling
@EnableConfigurationProperties
@EnableJpaRepositories({"com.alibaba.tesla.dag.model.repository", "com.alibaba.sreworks.job.master.domain.repository"})
@EntityScan({"com.alibaba.tesla.dag.model.domain", "com.alibaba.sreworks.job.master.domain"})
@ComponentScan(
    value = {
        "com.alibaba.sreworks",
        "com.alibaba.tesla"
    },
    excludeFilters = {
        @Filter(type = FilterType.REGEX, pattern = "com\\.alibaba\\.tesla\\.dag\\.selfcheck.*")
    }
)
@EnableAsync
@MapperScan(basePackages = {
    "com.alibaba.tesla.dag.repository.mapper"
})
public class MasterApplication {

    public static void main(String[] args) {
        SpringApplication.run(MasterApplication.class, args);
    }

}
