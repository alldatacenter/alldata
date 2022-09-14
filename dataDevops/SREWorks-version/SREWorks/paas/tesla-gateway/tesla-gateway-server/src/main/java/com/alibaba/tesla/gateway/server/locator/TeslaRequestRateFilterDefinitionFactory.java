package com.alibaba.tesla.gateway.server.locator;

import com.alibaba.tesla.gateway.common.enums.RateLimitTypeEnum;
import com.alibaba.tesla.gateway.domain.req.RouteRateLimit;
import com.alibaba.tesla.gateway.server.filter.limit.ClientIdKeyResolver;
import com.alibaba.tesla.gateway.server.filter.limit.RouteIdKeyResolver;
import com.alibaba.tesla.gateway.server.filter.limit.UserIdKeyResolver;
import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoDO;
import org.springframework.cloud.gateway.filter.FilterDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class TeslaRequestRateFilterDefinitionFactory {

    private static String REQUEST_FILTER_TMP = "RequestRateLimiter=%s, %s, %s";

    private static String RESOLVER_FORMAT_PREFIX = "#{@%s}";

    private TeslaRequestRateFilterDefinitionFactory() {
    }

    public static FilterDefinition buildRateLimitFilter(RouteInfoDO routeInfoDO){
        if(null == routeInfoDO.getRateLimit() ||
            Objects.equals(routeInfoDO.getRateLimit().getType(), RateLimitTypeEnum.NO_LIMIT.name())){
            return null;
        }
        RouteRateLimit rateLimit = routeInfoDO.getRateLimit();
        RateLimitTypeEnum type = RateLimitTypeEnum.valueOf(rateLimit.getType());
        if (Objects.equals(type, RateLimitTypeEnum.NO_LIMIT)) {
            return null;
        }
        int burstCapacity = rateLimit.getLimit() > 0 ?
            rateLimit.getLimit() : RouteRateLimit.DEFAULT_REQUEST_COUNT;
        int replenishRate = burstCapacity;
        String keyResolver;
        switch (type){
            case ROUTE_ID:
                keyResolver = String.format(RESOLVER_FORMAT_PREFIX, RouteIdKeyResolver.NAME);
                break;
            case USER_ID:
                keyResolver = String.format(RESOLVER_FORMAT_PREFIX, UserIdKeyResolver.NAME);
                break;
            case CLIENT_ID:
                keyResolver =  String.format(RESOLVER_FORMAT_PREFIX, ClientIdKeyResolver.NAME);
                break;
            default:
                throw new IllegalArgumentException("not support this type, type=" + rateLimit.getType());
        }

        FilterDefinition definition = new FilterDefinition();

        Map<String, String> args = new HashMap<>(8);
        args.put("key-resolver", keyResolver);
        args.put("redis-rate-limiter.replenishRate", String.valueOf(burstCapacity));
        args.put("redis-rate-limiter.burstCapacity", String.valueOf(replenishRate));
        args.put("redis-rate-limiter.requestedTokens", String.valueOf("1"));

        definition.setArgs(args);
        definition.setName("RequestRateLimiter");

        return definition;
    }
}
