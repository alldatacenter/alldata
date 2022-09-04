package com.alibaba.tesla.gateway.server.filter.limit;

import com.alibaba.tesla.gateway.server.constants.GatewayConst;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.web.server.ServerWebExchange;

import java.util.Optional;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */

public abstract class GatewayBaseKeyResolver implements KeyResolver {

    protected String getRouteId(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        return route == null ? GatewayConst.UNKNOWN : route.getId();
    }

    /**
     * get resolver type name
     * @return resolver type name
     */
    protected abstract String getResolverTypeName();

    /**
     * build resolver key
     * @param args args
     * @return string
     */
    protected String buildResolverKey(String ... args) {
        StringBuilder sb = new StringBuilder();
        sb.append(getResolverTypeName());
        for (String arg : args) {
            sb.append("||");
            sb.append(Optional.ofNullable(arg).orElse(GatewayConst.UNKNOWN));
        }
        return sb.toString();
    }
}
