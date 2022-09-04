package com.alibaba.tesla.gateway.server.filter.webfilter;

import com.alibaba.tesla.gateway.server.constants.WebExchangeConst;
import com.alibaba.tesla.gateway.server.exceptions.TeslaGatewayForwardEnvException;
import com.alibaba.tesla.web.properties.TeslaEnvProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * 限制请求转发
 * 日常不允许转发到预发和生产
 * 预发不允许转发到生产
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Component
public class GatewayForwardEnvWebFilter implements WebFilter, Ordered {
    private static final String ENV = "X-GATEWAY-FORWARD-ENV";

    private static final String PROD = "prod";
    private static final String PRE = "pre";
    private static final String DAILY = "daily";
    private static final String DAILY2 = "daily2";

    @Autowired
    private TeslaEnvProperties envProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String env = exchange.getRequest().getHeaders().getFirst(ENV);
        if(StringUtils.isNotBlank(env)){
            switch (envProperties.getEnv()){
                case DAILY:
                    if(Objects.equals(PRE, env) || Objects.equals(PROD, env)){
                        throw new TeslaGatewayForwardEnvException(String.format("daily environment can not forward to pre|prod environment, %s=%s", ENV, env));
                    }
                    break;
                case PRE:
                    if(Objects.equals(PROD, env)){
                        throw new TeslaGatewayForwardEnvException(String.format("pre environment can not forward to prod environment, %s=%s", ENV, env));
                    }
                default:
                    //nothing to do
            }
            if(!Objects.equals(envProperties.getEnv().name().toLowerCase(), env)){
                exchange.getAttributes().put(WebExchangeConst.TESLA_IS_FORWARD_ENV, true);
            }
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 5;
    }
}
