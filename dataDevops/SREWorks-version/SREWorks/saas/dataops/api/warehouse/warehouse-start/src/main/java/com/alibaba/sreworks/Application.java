package com.alibaba.sreworks;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

/**
 * Pandora Boot应用的入口类
 * <p>
 * 详情见http://gitlab.alibaba-inc.com/middleware-container/pandora-boot/wikis/spring-boot-diamond
 *
 * @author chengxu
 */
@Slf4j
@SpringBootApplication(
    scanBasePackages = {"com.alibaba.sreworks"}
)
@MapperScan(basePackages = {"com.alibaba.sreworks.warehouse.domain"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    public void init() {
        log.info("######## Application Init Success ########");
    }
}
