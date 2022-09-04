package com.alibaba.tesla.gateway.server.filter.webfilter;

import com.alibaba.tesla.gateway.server.config.properties.TeslaGatewayProperties;
import com.alibaba.tesla.gateway.server.constants.GatewayConst;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.*;

import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.HttpMethod.TRACE;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Component
public class CrosWebFilter implements WebFilter , Ordered {
    private static final List<String> ALL = Collections.singletonList("*");
    private static final List<String> ALL_ALLOW_HEADERS = Arrays.asList(("Origin,Expect,Allow,Accept,Content-Type,"
        + "Content-Length,Accept-Encoding,Accept-Language,Cookie,X-Requested-With,"
        + "X-Biz-App,X-Env,X-Gateway-Forward-Env,Eagleeye-Traceid,X-Biz-tenant,X-Run-As-Role,Authorization")
        .split(","));
    private static final List<String> ALL_EXPOSE_HEADERS = Arrays.asList("Content-Length,Content-Type,Content-Encoding".split(","));
    private static final List<HttpMethod> HTTP_METHOD_ALL = Arrays.asList(GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE);

    @Autowired
    private TeslaGatewayProperties gatewayProperties;

    private Set<String> allowCrosDomainSet = new HashSet<>(8);

    @PostConstruct
    private void init() {
        String crosDomain = this.gatewayProperties.getAllowCrosDomain();
        if (StringUtils.isNotBlank(crosDomain)) {
            allowCrosDomainSet.addAll(Arrays.asList(crosDomain.split(",")));
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (gatewayProperties.isEnabledCros()){
            if(CorsUtils.isCorsRequest(exchange.getRequest()) && Objects.equals(GatewayConst.ACCESS_ONCE, exchange.getRequest().getHeaders().getFirst(
                GatewayConst.TESLA_GATEWAY_ACCESS))){
                exchange = new CrosServerWebExchangeDecorator(exchange);

                if(Objects.equals(exchange.getRequest().getMethod(), HttpMethod.OPTIONS) &&
                    allowCrosDomainSet.contains(exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN))){
                    exchange.getResponse().getHeaders().setAccessControlAllowCredentials(true);
                    exchange.getResponse().getHeaders().setAccessControlAllowHeaders(ALL_ALLOW_HEADERS);
                    exchange.getResponse().getHeaders().setAccessControlExposeHeaders(ALL_EXPOSE_HEADERS);
                    exchange.getResponse().getHeaders().setAccessControlAllowMethods(HTTP_METHOD_ALL);
                    if(StringUtils.isNotBlank(exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN))){
                        exchange.getResponse().getHeaders().setAccessControlAllowOrigin(exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN));
                    }
                    exchange.getResponse().setStatusCode(HttpStatus.OK);
                    return Mono.empty();
                }
            }
        }
        return chain.filter(exchange);
    }

    class CrosServerWebExchangeDecorator extends ServerWebExchangeDecorator {

        private ServerHttpResponseDecorator responseDecorator;

        protected CrosServerWebExchangeDecorator(ServerWebExchange exchange) {
            super(exchange);
            this.responseDecorator = new CrosServerHttpResponseDecorator(exchange);
        }

        @Override
        public ServerHttpResponse getResponse() {
            return this.responseDecorator;
        }
    }

    class CrosServerHttpResponseDecorator extends ServerHttpResponseDecorator{

        private ServerWebExchange exchange;

        public CrosServerHttpResponseDecorator(ServerWebExchange exchange) {
            super(exchange.getResponse());
            this.exchange = exchange;
        }

        @Override
        public HttpHeaders getHeaders() {

            HttpHeaders httpHeaders = new HttpHeaders(super.getHeaders());

            httpHeaders.setAccessControlAllowCredentials(true);
            httpHeaders.setAccessControlAllowHeaders(ALL_ALLOW_HEADERS);
            httpHeaders.setAccessControlExposeHeaders(ALL_EXPOSE_HEADERS);
            httpHeaders.setAccessControlAllowMethods(HTTP_METHOD_ALL);
            if(StringUtils.isNotBlank(exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN))){
                httpHeaders.setAccessControlAllowOrigin(exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN));
            }

            return httpHeaders;
        }
    }

    @Override
    public int getOrder() {
        return 3;
    }
}
