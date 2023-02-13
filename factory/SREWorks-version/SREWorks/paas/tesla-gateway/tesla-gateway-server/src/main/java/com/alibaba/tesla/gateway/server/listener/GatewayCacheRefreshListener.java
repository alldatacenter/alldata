package com.alibaba.tesla.gateway.server.listener;

import com.alibaba.tesla.gateway.server.event.RouteCacheRefreshEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.CachingRouteLocator;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Component
public class GatewayCacheRefreshListener implements ApplicationListener<RouteCacheRefreshEvent> {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public void onApplicationEvent(RouteCacheRefreshEvent routeCacheRefreshEvent) {
        log.info("receive refresh route cache event, source={}", routeCacheRefreshEvent.getSource());
        //发送事件有两重含义，清理掉缓存中的路由和清理掉routeDefine，重新加载
        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
    }
}
