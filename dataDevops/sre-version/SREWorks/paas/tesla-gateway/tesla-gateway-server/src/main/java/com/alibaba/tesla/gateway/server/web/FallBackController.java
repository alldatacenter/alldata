package com.alibaba.tesla.gateway.server.web;

import com.alibaba.tesla.gateway.domain.TeslaGatewayResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import java.util.Date;
import java.util.Optional;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@RequestMapping("/fallback")
@RestController
public class FallBackController {

    /**
     * 使用多个模式，避免post模式等catch不住的问题
     * @param exchange {@link ServerWebExchange}
     * @return result
     */
    @RequestMapping
    public TeslaGatewayResult fallBack(ServerWebExchange exchange){
        Throwable exception = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        String message = HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase();
        if(exception != null){
            exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "service is not available, please try again later. " +
                Optional.ofNullable(exception.getLocalizedMessage()).orElse(StringUtils.EMPTY);
                exchange.getRequest().getId();
        }
        log.warn("actionName=fallback||requestId={}||status={}||message={}",
            exchange.getRequest().getId(),
            status,
            message,
            exception);
        exchange.getResponse().setStatusCode(status);
        return TeslaGatewayResult.builder()
            .message(message)
            .path(exchange.getRequest().getPath().value())
            .status(status.value())
            .timestamp(new Date())
            .requestId(exchange.getRequest().getId())
            .from(TeslaGatewayResult.GATEWAY_FLAG)
            .build();
    }
}
