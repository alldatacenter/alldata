package com.alibaba.tesla.appmanager.server.config;

import com.alibaba.tesla.appmanager.spring.event.ApplicationContextLoadedEvent;
import com.alibaba.tesla.appmanager.spring.util.SpringBeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApplicationContextConfig implements ApplicationContextAware {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringBeanUtil.setApplicationContext(applicationContext);
        log.info("application context has set into spring bean util");
        publisher.publishEvent(new ApplicationContextLoadedEvent(this));
    }
}
