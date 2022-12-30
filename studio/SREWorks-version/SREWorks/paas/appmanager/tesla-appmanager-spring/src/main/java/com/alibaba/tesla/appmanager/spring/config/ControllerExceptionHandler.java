package com.alibaba.tesla.appmanager.spring.config;

import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.annotation.Nullable;
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

    @ExceptionHandler({AppException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected TeslaBaseResult appException(AppException e, HttpServletRequest request) {
        log.warn("action=appExceptionHandler|method={}|url={}|message={}",
            request.getMethod(), getFullUrl(request), ExceptionUtils.getStackTrace(e));
        return TeslaResultFactory.buildResult(e.getErrorCode().getCode(), e.getErrorMessage());
    }

    @ExceptionHandler({Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    protected TeslaBaseResult exceptionHandler(Exception exception, HttpServletRequest request) {
        if (!"app offline".equals(exception.getMessage())) {
            log.warn("action=exceptionHandler|method={}|url={}|errorMessage={}|traceback={}",
                request.getMethod(), getFullUrl(request), exception.getMessage(),
                ExceptionUtils.getStackTrace(exception));
        }
        return TeslaResultFactory.buildExceptionResult(exception);
    }

    protected ResponseEntity<Object> handleExceptionInternal(
        Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
        log.warn("action=internalExceptionHandler|contextPath={}|params={}|httpStatus={}|errorMessage={}|traceback={}",
            request.getContextPath(), request.getParameterMap(), status, ex.getMessage(),
            ExceptionUtils.getStackTrace(ex));
        return new ResponseEntity<>(TeslaResultFactory.buildExceptionResult(ex), headers,
            HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static String getFullUrl(HttpServletRequest request) {
        if (null == request) {
            return "";
        } else {
            return StringUtils.isEmpty(request.getQueryString())
                    ? request.getRequestURL().toString()
                    : request.getRequestURL().append('?').append(request.getQueryString()).toString();
        }
    }
}
