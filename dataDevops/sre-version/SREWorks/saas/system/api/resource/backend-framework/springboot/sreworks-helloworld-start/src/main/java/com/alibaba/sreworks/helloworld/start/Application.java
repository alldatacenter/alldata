package com.alibaba.sreworks.helloworld.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.alibaba.sreworks.helloworld"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
