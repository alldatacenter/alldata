package com.alibaba.tesla.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.alibaba.tesla"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
