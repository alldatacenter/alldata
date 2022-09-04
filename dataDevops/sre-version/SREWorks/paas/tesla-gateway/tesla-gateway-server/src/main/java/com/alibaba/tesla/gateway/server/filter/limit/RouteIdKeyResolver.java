package com.alibaba.tesla.gateway.server.filter.limit;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Primary
@Component(value = RouteIdKeyResolver.NAME)
public class RouteIdKeyResolver extends GatewayBaseKeyResolver{

    public static final String NAME = "routeIdKeyResolver";

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return Mono.just(buildResolverKey(getRouteId(exchange)));
    }

    @Override

    protected String getResolverTypeName() {
        return NAME;
    }
}
