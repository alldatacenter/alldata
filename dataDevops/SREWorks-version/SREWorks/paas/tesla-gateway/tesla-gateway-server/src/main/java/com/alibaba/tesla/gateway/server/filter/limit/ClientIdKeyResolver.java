package com.alibaba.tesla.gateway.server.filter.limit;

import com.alibaba.tesla.gateway.server.constants.GatewayConst;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Component(value = ClientIdKeyResolver.NAME)
public class ClientIdKeyResolver extends GatewayBaseKeyResolver{
    public static final String NAME = "clientIdKeyResolver";

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        String app = exchange.getRequest().getHeaders().getFirst(GatewayConst.HEADER_NAME_APP);
        return Mono.just(buildResolverKey(getRouteId(exchange), app));
    }

    @Override
    protected String getResolverTypeName() {
        return NAME;
    }
}
