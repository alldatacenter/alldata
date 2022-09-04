package com.alibaba.tesla.gateway.server.filter.webfilter;

import com.alibaba.tesla.gateway.server.constants.GatewayConst;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Component
public class EagleEyeWebFilter implements WebFilter, Ordered {
    private  static String IP;

    static {
        try {
            IP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("UnknownHostException, ", e);
        }
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(GatewayConst.TRACE_ID);
        if(StringUtils.isBlank(traceId)){
            //traceId = EagleEye.generateTraceId(IP);
            traceId = UUID.randomUUID().toString();
            exchange.getRequest().mutate()
                .header(GatewayConst.TRACE_ID, new String[] {traceId});
        }
        EagleEyeServerWebExchangeDecorator exchangeDecorator = new EagleEyeServerWebExchangeDecorator(
            exchange);
        return chain.filter(exchangeDecorator);
    }

    class EagleEyeServerWebExchangeDecorator extends ServerWebExchangeDecorator{

        private ServerHttpResponseDecorator responseDecorator;

        protected EagleEyeServerWebExchangeDecorator(ServerWebExchange delegate) {
            super(delegate);
            this.responseDecorator = new EagleEyeServerHttpResponseDecorator(delegate);
        }

        @Override
        public ServerHttpResponse getResponse() {
            return this.responseDecorator;
        }
    }

    class EagleEyeServerHttpResponseDecorator extends ServerHttpResponseDecorator{

        private ServerWebExchange exchange;

        public EagleEyeServerHttpResponseDecorator(ServerWebExchange exchange) {
            super(exchange.getResponse());
            this.exchange = exchange;
        }

        @Override
        public HttpHeaders getHeaders() {

            HttpHeaders httpHeaders = new HttpHeaders(super.getHeaders());
            String responseTraceId = exchange.getRequest().getHeaders().getFirst(GatewayConst.TRACE_ID);
            if(StringUtils.isNotBlank(responseTraceId)){
                httpHeaders.set(GatewayConst.TRACE_ID, responseTraceId);
            }
            return httpHeaders;
        }
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
