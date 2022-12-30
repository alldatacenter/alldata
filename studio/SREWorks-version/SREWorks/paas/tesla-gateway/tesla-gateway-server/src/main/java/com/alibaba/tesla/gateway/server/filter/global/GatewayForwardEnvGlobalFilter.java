package com.alibaba.tesla.gateway.server.filter.global;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Service
public class GatewayForwardEnvGlobalFilter implements GlobalFilter, Ordered {

    private static final String ENV = "X-GATEWAY-FORWARD-ENV";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String env = exchange.getRequest().getHeaders().getFirst(ENV);
        if(StringUtils.isNotEmpty(env)){
            exchange.getRequest().mutate().header(ENV, new String[] {""});
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return GlobalFilterOrderManager.ENV_FORWARD_FILTER;
    }
}
