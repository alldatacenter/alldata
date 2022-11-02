package com.alibaba.tesla.gateway.server.filter.global;

import com.alibaba.tesla.gateway.domain.req.BlackListConf;
import com.alibaba.tesla.gateway.server.constants.GatewayConst;
import com.alibaba.tesla.gateway.server.exceptions.TeslaBlackListRejectException;
import com.alibaba.tesla.gateway.server.monitor.TeslaGatewayMetric;
import com.alibaba.tesla.gateway.server.util.RouteMetaDataKeyConstants;
import com.alibaba.tesla.gateway.server.util.TeslaServerRequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;

/**
 * 黑名单拦截器
 * 要先过鉴权，然后再拦截黑名单
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Component
public class BlackListGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private TeslaGatewayMetric teslaGatewayMetric;

    @Autowired
    private TeslaServerRequestUtil teslaServerRequestUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = (Route)exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route != null && !CollectionUtils.isEmpty(route.getMetadata()) && route.getMetadata().get(
            RouteMetaDataKeyConstants.BLACK_LIST) != null) {
            BlackListConf blackListConf = (BlackListConf)route.getMetadata().get(RouteMetaDataKeyConstants.BLACK_LIST);

            //check ip
            if (!CollectionUtils.isEmpty(blackListConf.getIpBlackList())) {
                String realHostIp = teslaServerRequestUtil.getRealHostIp(exchange.getRequest());
                if(StringUtils.isNotBlank(realHostIp)){
                    Optional<String> optional = blackListConf.getIpBlackList().stream().filter(
                        ip -> Objects.equals(realHostIp, ip)).findAny();
                    if (optional.isPresent()) {
                        this.teslaGatewayMetric.recordBlackListReject(route.getId(), "ip");
                        throw new TeslaBlackListRejectException("ip in blacklist, please contact administrator. ip=" + realHostIp);
                    }
                }
            }
            //check user
            if (!CollectionUtils.isEmpty(blackListConf.getUserBlackList())) {
                String userId = exchange.getRequest().getHeaders().getFirst(GatewayConst.HEADER_NAME_EMP_ID);
                if (StringUtils.isNotBlank(userId)) {
                    Optional<String> optional = blackListConf.getUserBlackList().stream().filter(
                        id -> Objects.equals(userId, id)).findAny();
                    if (optional.isPresent()) {
                        this.teslaGatewayMetric.recordBlackListReject(route.getId(), "userId");
                        throw new TeslaBlackListRejectException("userId in blacklist, please contact administrator. userId=" + userId);
                    }
                }
            }

            //check app
            if (!CollectionUtils.isEmpty(blackListConf.getAppBlackList())) {
                String authApp = exchange.getRequest().getHeaders().getFirst(GatewayConst.HEADER_NAME_APP);
                if (StringUtils.isNotBlank(authApp)) {
                    Optional<String> optional = blackListConf.getAppBlackList().stream().filter(
                        app -> Objects.equals(app, authApp)).findAny();
                    if (optional.isPresent()) {
                        this.teslaGatewayMetric.recordBlackListReject(route.getId(), "app");
                        throw new TeslaBlackListRejectException("authApp in blacklist, please contact administrator. authApp=" + authApp);
                    }
                }
            }

        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return GlobalFilterOrderManager.BALK_LIST_FILTER;
    }
}
