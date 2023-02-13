package com.alibaba.tesla.gateway.server.locator;

import com.alibaba.tesla.gateway.domain.req.RouteInfo;
import com.alibaba.tesla.gateway.domain.res.RouteOperatorRes;
import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoDO;
import org.springframework.cloud.gateway.route.RouteDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public interface DynamicBaseRouteLocator {

    /**
     * 更新路由，add|update
     * @param route
     * @return boolean
     */
    Mono<Boolean> insert(RouteInfoDO route);

    /**
     * 移除路由
     * @param routeId route id
     * @return operator res
     */
    Mono<RouteOperatorRes> delete(String routeId);

    /**
     * 查询
     * @param routeId route id
     * @return route definition
     */
    Mono<RouteDefinition> get(String routeId);

    /**
     * 获取所有的路由信息
     * @return route definition
     */
    Flux<RouteDefinition> getAll();

}
