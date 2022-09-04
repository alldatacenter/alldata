package com.alibaba.tesla.nacos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * spring Boot应用的入口类
 * <p>
 *
 * @author qiuqiang.qq@alibaba-inc.com
 */
@SpringBootApplication(scanBasePackages = {"com.alibaba.nacos", "com.alibaba.tesla"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
