package com.alibaba.tesla.gateway.server.filter.limit;

import com.alibaba.tesla.gateway.server.constants.GatewayConst;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;



/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Component(value = UserIdKeyResolver.NAME)
public class UserIdKeyResolver extends GatewayBaseKeyResolver{

    public static final String NAME = "userIdKeyResolver";

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        String empId = exchange.getRequest().getHeaders().getFirst(GatewayConst.HEADER_NAME_EMP_ID);
        return Mono.just(buildResolverKey(getRouteId(exchange), empId));
    }

    @Override

    protected String getResolverTypeName() {
        return NAME;
    }
}
