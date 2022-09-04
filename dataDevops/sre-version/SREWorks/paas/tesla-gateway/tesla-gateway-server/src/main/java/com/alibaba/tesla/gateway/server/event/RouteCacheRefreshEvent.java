package com.alibaba.tesla.gateway.server.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
public class RouteCacheRefreshEvent extends ApplicationEvent {
    public RouteCacheRefreshEvent(Object source) {
        super(source);
    }
}
