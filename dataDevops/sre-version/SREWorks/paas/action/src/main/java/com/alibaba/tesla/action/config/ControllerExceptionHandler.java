package com.alibaba.tesla.action.config;

import com.alibaba.tesla.action.utils.ServletRequestUtils;
import com.alibaba.tesla.action.common.TeslaBaseResult;
import com.alibaba.tesla.action.common.TeslaResultFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局 Controller 异常处理器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@ControllerAdvice(
    annotations = {RestController.class, Controller.class}
)
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    protected TeslaBaseResult exceptionHandler(Exception exception, HttpServletRequest request) {
        if (!"app offline".equals(exception.getMessage())) {
            log.warn("action=exceptionHandler||method={}||url={}||errorMessage={}||traceback={}",
                request.getMethod(), ServletRequestUtils.getFullUrl(request), exception.getMessage(),
                ExceptionUtils.getStackTrace(exception));
        }
        return TeslaResultFactory.buildExceptionResult(exception);
    }

    protected ResponseEntity<Object> handleExceptionInternal(
        Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
        log.warn("action=internalExceptionHandler||contextPath={}||params={}||httpStatus={}||errorMessage={}||traceback={}",
            request.getContextPath(), request.getParameterMap(), status, ex.getMessage(),
            ExceptionUtils.getStackTrace(ex));
        return new ResponseEntity<>(TeslaResultFactory.buildExceptionResult(ex), headers,
            HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
