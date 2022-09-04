package com.alibaba.tdata.start;

import com.alicp.jetcache.autoconfigure.JetCacheAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Hello world!
 *
 */
@SpringBootApplication(exclude = {JetCacheAutoConfiguration.class})
@EnableScheduling
@ComponentScan(basePackages = {"com.alibaba.tdata"})
@MapperScan({"com.alibaba.tdata.aisp.server.repository.mapper"})
public class ApplicationPrivate {
    public static void main( String[] args )
    {
        SpringApplication.run(ApplicationPrivate.class, args);
    }
}
