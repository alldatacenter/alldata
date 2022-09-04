package com.alibaba.tesla.tkgone.start;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Pandora Boot应用的入口类
 * <p>
 * 详情见http://gitlab.alibaba-inc.com/middleware-container/pandora-boot/wikis/spring-boot-diamond
 *
 * @author chengxu
 */
@SpringBootApplication(scanBasePackages = {"com.alibaba.tesla"})
@MapperScan(basePackages = {"com.alibaba.tesla.tkgone.server.domain"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
