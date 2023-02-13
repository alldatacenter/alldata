package com.platform.ck;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author AllDataDC
 * @date 2023/01/05
 */
@SpringBootApplication
@MapperScan("com.platform.ck.mapper")
@Slf4j
public class CKBase {
    public static void main(String[] args) {
        log.info("clickhouse 应用程序启动成功");
        SpringApplication.run(CKBase.class,args);
    }
}

