package com.alibaba.tesla.gateway.server.gateway;

import com.alibaba.tesla.common.base.enums.TeslaEnv;
import com.alibaba.tesla.gateway.common.enums.ServerTypeEnum;
import com.alibaba.tesla.gateway.server.cache.GatewayCache;
import com.alibaba.tesla.gateway.server.constants.GatewayConst;
import com.alibaba.tesla.gateway.server.exceptions.TeslaXEnvForwardException;
import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoDO;
import com.alibaba.tesla.web.properties.TeslaEnvProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.handler.FilteringWebHandler;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

import static com.alibaba.tesla.gateway.server.constants.GatewayConst.*;
import static com.alibaba.tesla.gateway.server.constants.GatewayConst.GATEWAY_DAILY_ROUTE_ID;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
public class TeslaFilteringWebHandler extends FilteringWebHandler {

    private GatewayCache gatewayCache;

    private TeslaEnvProperties envProperties;

    public TeslaFilteringWebHandler(List<GlobalFilter> globalFilters, GatewayCache gatewayCache,
                                    TeslaEnvProperties envProperties) {
        super(globalFilters);
        this.gatewayCache = gatewayCache;
        this.envProperties = envProperties;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange) {
        //更改路由
        //从缓存中取出，然后重置更改路由，指向日常和预发网关
        String env = exchange.getRequest().getHeaders().getFirst(X_ENV_NAME);
        if(StringUtils.isNotBlank(env)){
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            if(route != null){
                String routeId = route.getId();
                RouteInfoDO routeInfoDO = gatewayCache.getRouteInfoByRouteId(routeId);
                Assert.notNull(routeInfoDO, "routeInfo not exist, routeId=" + routeId);
                if(!Objects.equals(routeInfoDO.getServerType(), ServerTypeEnum.PAAS.name())){
                    if(isAllowForward(env)){
                        if(StringUtils.equalsIgnoreCase(TESLA_X_ENV_PRE, env)){
                            //修改路由, 转发预发网关
                            logger.info("forward pre env gateway, requestId=" + exchange.getRequest().getId());
                            Route gatewayPreRoute = gatewayCache.getRouteByRouteId(GATEWAY_PRE_ROUTE_ID);
                            Assert.notNull(gatewayPreRoute, "gateway pre env route not exist");
                            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, gatewayPreRoute);
                        }else if(StringUtils.equalsIgnoreCase(GatewayConst.TESLA_X_ENV_DAILY, env)){
                            //修改路由, 转发到日常网关
                            logger.info("forward daily env gateway, requestId=" +  exchange.getRequest().getId());
                            Route gatewayPreRoute = gatewayCache.getRouteByRouteId(GATEWAY_DAILY_ROUTE_ID);
                            Assert.notNull(gatewayPreRoute, "gateway daily env route not exist, routeId=" + GATEWAY_DAILY_ROUTE_ID);
                            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, gatewayPreRoute);
                        }
                    }
                }
            }
        }
        return super.handle(exchange);
    }

    private boolean isAllowForward(String env){
        if (StringUtils.isBlank(env)){
            return false;
        }
        boolean allow = false;
        TeslaEnv teslaEnv = TeslaEnv.valueOf(env.toUpperCase());
        TeslaEnv currentEnv = TeslaEnv.valueOf(envProperties.getEnv().name().toUpperCase());
        switch (currentEnv){
            case DAILY:
                if(!Objects.equals(teslaEnv, TeslaEnv.DAILY)){
                    throw new TeslaXEnvForwardException("not allow forward to " + teslaEnv);
                }
                break;
            case PRE:
                if(Objects.equals(teslaEnv, TeslaEnv.PROD)){
                    throw new TeslaXEnvForwardException("not allow forward to " + teslaEnv);
                }else if(!Objects.equals(teslaEnv, TeslaEnv.PRE)){
                    allow = true;
                }
                break;
            case PROD:
                if(!Objects.equals(teslaEnv, TeslaEnv.PROD)){
                    allow = true;
                }
                break;
            default:
                // nothing to do

        }
        return allow;

    }
}
