package com.alibaba.tesla.authproxy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

@Configuration
@Slf4j
public class ApplicationStartup implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private AuthProperties authProperties;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Auth Policy:" + authProperties.getAuthPolicy());
        log.info("Login Policy:" + authProperties.getLoginPolicy());
        log.info("Default Role:" + authProperties.isDefaultRole());
    }
}
