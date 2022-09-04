package com.alibaba.tesla.gateway.server.listener;

import com.alibaba.tesla.gateway.server.cache.GatewayCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Component
public class GatewayRouteCacheRefreshListener implements ApplicationListener<RefreshRoutesEvent> {

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private GatewayCache gatewayCache;


    @Async("eventRefreshExecutor")
    @Override
    public void onApplicationEvent(RefreshRoutesEvent event) {
        log.info("receive RefreshRouteEvent, event ={}", event.toString());
        try {
            TimeUnit.MILLISECONDS.sleep(100);
            List<Route> newRoute = routeLocator.getRoutes().toStream().collect(Collectors.toList());
            gatewayCache.updateRouteCache(newRoute);
        }catch (Exception e){
            log.error("exception, refresh route event failed");
        }
    }
}
