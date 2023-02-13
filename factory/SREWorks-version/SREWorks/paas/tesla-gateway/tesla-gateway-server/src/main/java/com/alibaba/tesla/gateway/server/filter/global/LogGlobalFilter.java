package com.alibaba.tesla.gateway.server.filter.global;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.gateway.common.utils.DateTimeUtil;
import com.alibaba.tesla.gateway.server.constants.GatewayConst;
import com.alibaba.tesla.gateway.server.constants.WebExchangeConst;
import com.alibaba.tesla.gateway.server.monitor.TeslaGatewayMetric;
import com.alibaba.tesla.gateway.server.util.TeslaServerRequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * log日志记录，优化时慎重考虑，blink的清洗作业
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Component
public class LogGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private TeslaGatewayMetric gatewayMetric;

    @Autowired
    private TeslaServerRequestUtil teslaServerRequestUtil;

    private AtomicLong count = new AtomicLong(0L);


    @PostConstruct
    private void init(){
        log.info("Init  GlobalFilter [LogGlobalFilter]");
        gatewayMetric.connectNum(count);
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        count.incrementAndGet();
        exchange.getAttributes().put(WebExchangeConst.EXEC_START_TIME, System.currentTimeMillis());
        return chain.filter(exchange)
            .doFinally(type -> {
                Long startTime = exchange.getAttribute(WebExchangeConst.EXEC_START_TIME);
                LinkedHashSet<URI> originRequestSet = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
                if(CollectionUtils.isEmpty(originRequestSet)){
                    originRequestSet = new LinkedHashSet<>();
                    originRequestSet.add(getUnknown());

                }
                URI originUri = originRequestSet.iterator().next();

                URI gatewayRequestUrl = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
                Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
                String routeId = route != null ? route.getId() : null;
                if(null != startTime){
                    long executeTime = System.currentTimeMillis() - startTime;
                    long authCostTime = this.getAuthCheckCostTime(exchange);
                    log.info("actionName=executeLog||id={}||traceId={}||type={}||x-env={}||routeId={}||method={}||originUri={}||rawQuery={}||requestHost={}||originHost={}||targetUri={}||targetHost={}||headers={}||httpCode={}||startTime={}||authCostTime={}||cost={}",
                        exchange.getRequest().getId(),
                        exchange.getRequest().getHeaders().getFirst(GatewayConst.TRACE_ID),
                        type,
                        exchange.getRequest().getHeaders().getFirst(GatewayConst.X_ENV_NAME),
                        routeId,
                        exchange.getRequest().getMethod(),
                        originUri.getPath(),
                        originUri.getRawQuery(),
                        originUri.getHost(),
                        teslaServerRequestUtil.getRealHostIp(exchange.getRequest()),
                        gatewayRequestUrl != null ? gatewayRequestUrl.getPath() : null,
                        gatewayRequestUrl != null ? gatewayRequestUrl.getAuthority() : null,
                        JSONObject.toJSONString(exchange.getRequest().getHeaders()),
                        exchange.getResponse().getStatusCode() != null ? String.valueOf(exchange.getResponse().getStatusCode().value()) : "unknown",
                        DateTimeUtil.formart(startTime),
                        authCostTime,
                        executeTime);
                }
                exchange.getAttributes().remove(WebExchangeConst.EXEC_START_TIME);
                count.decrementAndGet();
            });
    }

    /**
     * 获取鉴权花费的时间
     * @param exchange {@link ServerWebExchange}
     * @return auth check cost time
     */
    private long getAuthCheckCostTime(ServerWebExchange exchange) {
        Object object = exchange.getAttribute(WebExchangeConst.TESLA_AUTH_CHECK_TIME);
        if(object == null){
            return -1L;
        }
        return (Long) object;
    }

    @Override
    public int getOrder() {
        return GlobalFilterOrderManager.LOG_GLOBAL_FILTER;
    }


    private static URI getUnknown(){
        try {
            return new URI("http://unknown/unkonwn");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
