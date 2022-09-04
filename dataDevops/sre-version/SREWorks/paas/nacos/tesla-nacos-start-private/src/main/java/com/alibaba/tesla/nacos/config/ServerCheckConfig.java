package com.alibaba.tesla.nacos.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Configuration
public class ServerCheckConfig implements BeanPostProcessor {

    @Resource
    private RestTemplate restTemplate;

    @SuppressWarnings("all")
    @PostConstruct
    private void init(){
        new Thread(() -> {
            log.info("checkServer start ....");
            LocalDateTime startTime = LocalDateTime.now().plusMinutes(5);
            boolean isOk = false;
            while (LocalDateTime.now().isBefore(startTime)) {
                try {
                    isOk = checkServerIsOk();
                    TimeUnit.SECONDS.sleep(20L);
                    if (isOk) {
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (!isOk) {
                //端口未准备好，则退出
                log.info("start exsit .....");
                System.exit(1);
            }
            log.info("ServerCheck finished");
        }).start();
    }

    private boolean checkServerIsOk(){
        boolean status = false;
        try {
            ResponseEntity<String> responseEntity = restTemplate.getForEntity("http://127.0.0.1:8848/nacos/status.taobao", null, String.class);
            if (HttpStatus.OK.value() != responseEntity.getStatusCode().value()) {
                log.warn("check failed..., code={}", responseEntity.getStatusCode());
            }else {
                log.info("check success ....");
                status = true;
            }
        } catch (Exception e) {
            log.error("checkServer failed, errMsg={}", e.getMessage());
        }finally {
            log.info("checkServer, status={}", status);
        }
        return status;
    }
}
