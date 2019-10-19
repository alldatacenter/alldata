package com.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 应用启动入口
 * Created by wulinhao on 2019/9/26.
 */
@ImportResource(locations="classpath:spring/dubbo-manage-customer.xml")
@SpringBootApplication
public class MallManageService {
    public static void main(String[] args) {
        SpringApplication.run(MallManageService.class, args);
    }
}
