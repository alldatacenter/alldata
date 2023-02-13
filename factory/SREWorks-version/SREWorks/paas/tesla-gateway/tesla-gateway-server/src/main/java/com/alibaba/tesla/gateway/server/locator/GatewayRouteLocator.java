package com.alibaba.tesla.gateway.server.locator;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.util.TeslaGsonUtil;
import com.alibaba.tesla.gateway.server.event.RouteCacheRefreshEvent;
import com.alibaba.tesla.gateway.server.event.RouteRefreshEvent;
import com.alibaba.tesla.gateway.server.cache.GatewayCache;
import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Component
@Slf4j
public class GatewayRouteLocator implements ApplicationListener<RouteRefreshEvent> {

    @Autowired
    private DynamicBaseRouteLocator dynamicBaseRouteLocator;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private GatewayCache gatewayCache;

    /**
     * 更新路由规则
     *
     * @param routeInfos 路由配置
     */
    private Mono<Void> updateRoute(List<RouteInfoDO> routeInfos) {
        List<RouteInfoDO> routes = routeInfos.stream().filter(routeInfo -> routeInfo.isEnable()).collect(
            Collectors.toList());
        if(CollectionUtils.isEmpty(routes)){
            return Mono.empty();
        }
        this.gatewayCache.updateRouteInfoCache(routes);
        List<String> routeIds = routes.stream().map(RouteInfoDO::getRouteId).collect(Collectors.toList());
        return Mono.just(routes).flatMapMany(Flux::fromIterable)
            .doOnNext(b -> log.info("insert routeInfo to gateway||routeInfo={}", JSONObject.toJSONString(b)))
            .map(routeInfo -> this.dynamicBaseRouteLocator.insert(routeInfo).block())
            .onErrorContinue((e, o) -> {
                log.error("error, routeInfo={}", o.toString(), e);
            })
            .then(this.dynamicBaseRouteLocator.getAll().filter(route -> !routeIds.contains(route.getId()))
                .doOnEach(route -> {
                    if(route !=null && route.get() != null){
                        log.info("delete route||routeId={}||route={}", route.get().getId(), JSONObject.toJSONString(route));
                        this.dynamicBaseRouteLocator.delete(route.get().getId()).block();
                    }
                })
                .then())
            .doFinally(type -> this.publishRefreshEvent());
    }

    private Mono<Void> publishRefreshEvent(){
        log.info("publish refresh event");
        this.eventPublisher.publishEvent(new RouteCacheRefreshEvent("tesla-gateway-route-cache-update"));
        return Mono.empty();
    }

    @Override
    public void onApplicationEvent(RouteRefreshEvent routeRefreshEvent) {
        List<RouteInfoDO> routeInfos = routeRefreshEvent.getRouteConfig();
        updateRoute(routeInfos)
            .doOnSuccess(v ->
                log.info("Updated route config, size={}", routeInfos.size())
                )
            .onErrorContinue((t, b) -> {
                log.error("action=route.route.locator||message=update route failed||routeConfig={}||exception={}",
                    TeslaGsonUtil.toJson(routeInfos), ExceptionUtils.getStackTrace(t));
            })
            .subscribe();
    }
}
