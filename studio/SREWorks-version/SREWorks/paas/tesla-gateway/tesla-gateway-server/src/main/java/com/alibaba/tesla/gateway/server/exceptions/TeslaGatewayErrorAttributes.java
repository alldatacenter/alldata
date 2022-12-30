package com.alibaba.tesla.gateway.server.exceptions;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.gateway.common.exception.TeslaIllegalArgumentException;
import com.alibaba.tesla.gateway.domain.TeslaGatewayResult;
import com.alibaba.tesla.gateway.server.monitor.TeslaGatewayMetric;
import com.alibaba.tesla.gateway.server.util.TeslaServerRequestUtil;
import com.alibaba.tesla.gateway.server.util.UserAgentUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 加入request id 方便排查问题
 * 异常统一渲染逻辑
 * ref {@link org.springframework.boot.web.reactive.error.DefaultErrorAttributes}
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Component
public class TeslaGatewayErrorAttributes implements ErrorAttributes {

    private static final String ERROR_ATTRIBUTE = DefaultErrorAttributes.class.getName() + ".ERROR";

    private final boolean includeException;

    /**
     * Create a new {@link DefaultErrorAttributes} instance that does not include the
     * "exception" attribute.
     */
    public TeslaGatewayErrorAttributes() {
        this(false);
    }

    @Autowired
    private TeslaGatewayMetric gatewayErrorMetric;

    @Autowired
    private TeslaServerRequestUtil teslaServerRequestUtil;

    /**
     * Create a new {@link DefaultErrorAttributes} instance.
     * @param includeException whether to include the "exception" attribute
     */
    public TeslaGatewayErrorAttributes(boolean includeException) {
        this.includeException = includeException;
    }

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, boolean includeStackTrace) {
        Map<String, Object> errorAttributes = new LinkedHashMap<>();
        errorAttributes.put("timestamp", new Date());
        errorAttributes.put("path", request.path());
        Throwable error = getError(request);
        HttpStatus errorStatus = determineHttpStatus(error);
        errorAttributes.put("status", errorStatus.value());
        errorAttributes.put("requestId", request.exchange().getRequest().getId());
        errorAttributes.put("error", errorStatus.getReasonPhrase());
        errorAttributes.put("message", determineMessage(error));
        errorAttributes.put("from", TeslaGatewayResult.GATEWAY_FLAG);
        errorAttributes.put("x-env", teslaServerRequestUtil.getXEnv(request));
        handleException(request, errorAttributes, determineException(error), includeStackTrace);
        return errorAttributes;
    }

    private HttpStatus determineHttpStatus(Throwable error) {
        if (error instanceof ResponseStatusException) {
            return ((ResponseStatusException) error).getStatus();
        }

        //对异常的统一处理逻辑，添加在此处
        if(error instanceof TeslaIllegalArgumentException){
            return HttpStatus.BAD_REQUEST;
        }

        if(error instanceof TeslaGatewayForwardEnvException){
            return HttpStatus.BAD_REQUEST;
        }

        if(error instanceof TeslaXEnvForwardException){
            return HttpStatus.BAD_REQUEST;
        }

        ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(error.getClass(),
            ResponseStatus.class);
        if (responseStatus != null) {
            return responseStatus.code();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String determineMessage(Throwable error) {
        if (error instanceof WebExchangeBindException) {
            return error.getMessage();
        }
        if (error instanceof ResponseStatusException) {
            return ((ResponseStatusException) error).getReason();
        }
        ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(error.getClass(),
            ResponseStatus.class);
        if (responseStatus != null) {
            return responseStatus.reason();
        }
        return error.getMessage();
    }

    private Throwable determineException(Throwable error) {
        if (error instanceof ResponseStatusException) {
            return (error.getCause() != null) ? error.getCause() : error;
        }
        return error;
    }

    private void addStackTrace(Map<String, Object> errorAttributes, Throwable error) {
        StringWriter stackTrace = new StringWriter();
        error.printStackTrace(new PrintWriter(stackTrace));
        stackTrace.flush();
        errorAttributes.put("trace", stackTrace.toString());
    }

    private void handleException(ServerRequest request, Map<String, Object> errorAttributes, Throwable error, boolean includeStackTrace) {
        if (this.includeException) {
            errorAttributes.put("exception", error.getClass().getName());
        }
        if (includeStackTrace) {
            addStackTrace(errorAttributes, error);
        }
        if (error instanceof BindingResult) {
            BindingResult result = (BindingResult) error;
            if (result.hasErrors()) {
                errorAttributes.put("errors", result.getAllErrors());
            }
        }

        //todo
        //异常统一处理
        String path = (String)errorAttributes.get("path");
        String requestId = (String) errorAttributes.get("requestId");
        String xEnv = (String)errorAttributes.get("x-env");
        if (error instanceof ResponseStatusException){
            HttpStatus status = ((ResponseStatusException)error).getStatus();
            gatewayErrorMetric.httpErrorRecord(status, path, xEnv);
            if(Objects.equals(status, HttpStatus.INTERNAL_SERVER_ERROR)){
                log.error("Tesla gateway error||requestId={}||path={}||status={}||errorInfo={},", requestId, path, status, JSONObject.toJSONString(errorAttributes), error);
            }else {
                log.warn("error, requestId={}||requestUrl={}||headers={}||status={}||message={}", requestId, request.uri().toString(), request.headers().toString(), status, error.getLocalizedMessage());
            }
        } if(error instanceof TeslaGatewayForwardEnvException || error instanceof TeslaXEnvForwardException){
            gatewayErrorMetric.httpErrorRecord(HttpStatus.BAD_REQUEST, path, xEnv);
            log.info("base request, requestId={}, message={}, path={}", requestId, error.getLocalizedMessage(), path);
        }
        else {
            gatewayErrorMetric.httpErrorRecord(HttpStatus.INTERNAL_SERVER_ERROR, path, xEnv);
        }
    }

    @Override
    public Throwable getError(ServerRequest request) {
        return (Throwable) request.attribute(ERROR_ATTRIBUTE)
            .orElseThrow(() -> new IllegalStateException("Missing exception attribute in ServerWebExchange"));
    }

    @Override
    public void storeErrorInformation(Throwable error, ServerWebExchange exchange) {
        exchange.getAttributes().putIfAbsent(ERROR_ATTRIBUTE, error);
    }
}
