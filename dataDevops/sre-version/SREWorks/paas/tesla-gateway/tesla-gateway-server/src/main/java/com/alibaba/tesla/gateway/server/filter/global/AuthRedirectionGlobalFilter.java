package com.alibaba.tesla.gateway.server.filter.global;

import com.alibaba.tesla.gateway.domain.req.AuthRedirection;
import com.alibaba.tesla.gateway.server.config.properties.TeslaGatewayProperties;
import com.alibaba.tesla.gateway.server.util.RouteMetaDataKeyConstants;
import com.alibaba.tesla.gateway.server.util.UserAgentUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Component
public class AuthRedirectionGlobalFilter implements GlobalFilter, Ordered {

    private static final List<String> GLOBAL_REDIRECT_URIS = Collections.singletonList("/v2/api-docs");

    @Resource
    private TeslaGatewayProperties teslaGatewayProperties;


    @Override
    public int getOrder() {
        return GlobalFilterOrderManager.AUTH_REDIRECT_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getResponse().beforeCommit(() -> {
            Route route = (Route)exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            if (Objects.equals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode()) && route != null &&  UserAgentUtil.isBrowserReq(exchange.getRequest())) {
                if ( isGlobalRedirectUri(exchange.getRequest().getPath().toString()) || (
                    !CollectionUtils.isEmpty(route.getMetadata())
                        && route.getMetadata().get(RouteMetaDataKeyConstants.AUTH_DIRECTION) != null
                        && ((AuthRedirection) route.getMetadata().get(RouteMetaDataKeyConstants.AUTH_DIRECTION)).isEnabled()
                )) {
                    AuthRedirection authRedirection = (AuthRedirection)route.getMetadata().get(
                        RouteMetaDataKeyConstants.AUTH_DIRECTION);
                    exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                    String redirectUrl = exchange.getRequest().getURI().toString();
                    if (StringUtils.isNotBlank(authRedirection.getDefaultLocation())) {
                        if (route.getMetadata().get(RouteMetaDataKeyConstants.STRIP_PREFIX) != null) {
                            int num = Integer.parseInt((String) route.getMetadata().get(RouteMetaDataKeyConstants.STRIP_PREFIX));
                            redirectUrl = authRedirection.getDefaultLocation();
                            if (!authRedirection.getDefaultLocation().endsWith("/")) {
                                redirectUrl += "/";
                            }
                            redirectUrl += Arrays.stream(exchange.getRequest().getURI().getRawPath().split("/")).skip(num + 1).collect(
                                Collectors.joining("/"));
                        } else {
                            redirectUrl = authRedirection.getDefaultLocation() + exchange.getRequest().getURI().getRawPath();
                        }
                        if (StringUtils.isNotBlank(exchange.getRequest().getURI().getRawQuery())) {
                            redirectUrl += ("?" + exchange.getRequest().getURI().getRawQuery());
                        }
                    }
                    try {
                        exchange.getResponse().getHeaders().set("location", teslaGatewayProperties.getAuthRedirectUrl() + "?backurl="
                            + URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8.toString()));
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                    exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                }
            }
            return Mono.empty();
        });
        return chain.filter(exchange);
    }

    private boolean isGlobalRedirectUri(String  path) {
        return false;
        //return GLOBAL_REDIRECT_URIS.stream().anyMatch(path::endsWith);
    }
}
