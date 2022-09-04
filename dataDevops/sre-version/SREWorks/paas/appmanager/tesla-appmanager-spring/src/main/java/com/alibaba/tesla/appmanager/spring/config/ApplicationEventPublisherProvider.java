package com.alibaba.tesla.appmanager.spring.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.*;
import org.springframework.context.*;

/**
 * Event Publisher 提供者
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class ApplicationEventPublisherProvider implements ApplicationEventPublisherAware {

    private ApplicationEventPublisher applicationEventPublisher;

    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public ApplicationEventPublisher getApplicationEventPublisher() {
        return applicationEventPublisher;
    }
}