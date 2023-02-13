package com.alibaba.tesla.gateway.server.handler;

import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;
import com.alibaba.tesla.gateway.common.exception.GatewayException;
import com.alibaba.tesla.gateway.common.exception.OperatorRouteException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;


/**
 * 网关本身异常处理
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String DEFAULT_ERROR_MSG_TEMPLATE = "Gateway server error, requestId=%s, errMsg=%s";


    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public TeslaBaseResult handBindException(WebExchangeBindException e){
        return TeslaResultFactory.buildResult(1000, null, e.getLocalizedMessage());
    }


    @ExceptionHandler({GatewayException.class, Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public TeslaBaseResult defaultHandException(ServerHttpRequest request, Exception e){
        log.error("default exception, requestId={} ", request.getId(), e);
        return TeslaResultFactory.buildResult(HttpStatus.INTERNAL_SERVER_ERROR.value(),
            String.format(DEFAULT_ERROR_MSG_TEMPLATE, request.getId(), e.getLocalizedMessage()) ,
            ExceptionUtils.getStackTrace(e));
    }

    @ExceptionHandler({OperatorRouteException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public TeslaBaseResult operatorRouteException(OperatorRouteException e){
        log.warn("", e);
        return TeslaResultFactory.buildResult(HttpStatus.BAD_REQUEST.value(), e.getLocalizedMessage());
    }
}
