package com.alibaba.tesla.gateway.server.filter.webfilter;

import com.alibaba.tesla.gateway.server.constants.GatewayConst;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * 加入记录请求经过网关的次数
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Component
public class AccessWebFilter implements WebFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        exchange.getRequest().mutate().header(GatewayConst.TESLA_GATEWAY_ACCESS, new String[] {buildAccessNum(exchange)});
        return chain.filter(exchange);
    }


    @Override
    public int getOrder() {
        return 1;
    }

    private String buildAccessNum(ServerWebExchange exchange){
        int num = 1;
        if(StringUtils.isNotBlank(exchange.getRequest().getHeaders().getFirst(GatewayConst.TESLA_GATEWAY_ACCESS))){
            num = Integer.parseInt(
                Objects.requireNonNull(exchange.getRequest().getHeaders().getFirst(GatewayConst.TESLA_GATEWAY_ACCESS))) + 1;
        }
        return String.valueOf(num);
    }

}
