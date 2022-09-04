package com.alibaba.tesla.gateway.server.locator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Collections.synchronizedMap;

/**
 * 复写getRouteDefines,防止并行修改routeDefine而跑错
 * @see org.springframework.cloud.gateway.route.InMemoryRouteDefinitionRepository
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Component
public class TeslaInMemoryRouteDefinitionRepository implements RouteDefinitionRepository {

    private final Map<String, RouteDefinition> routes = synchronizedMap(
        new LinkedHashMap<String, RouteDefinition>());


    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return route.flatMap(r -> {
            routes.put(r.getId(), r);
            return Mono.empty();
        });
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        return routeId.flatMap(id -> {
            if (routes.containsKey(id)) {
                routes.remove(id);
                return Mono.empty();
            }
            return Mono.defer(() -> Mono.error(
                new NotFoundException("RouteDefinition not found: " + routeId)));
        });
    }

    @SuppressWarnings("all")
    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return Flux.fromIterable(new CopyOnWriteArrayList(routes.values()));
    }


}
