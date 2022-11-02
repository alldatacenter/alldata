package com.alibaba.tesla.server.common.handler;

import com.alibaba.tesla.server.common.constant.AispResult;
import com.alibaba.tesla.server.common.util.ExceptionUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.alibaba.tesla.server.common.factory.AispResponseFactory.buildResult;

/**
 * @ClassName: ProcessGlobalExceptionHandler
 * @Author: dyj
 * @DATE: 2022-03-01
 * @Description:
 **/
@Slf4j
@RestControllerAdvice
public class ProcessGlobalExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public AispResult handBadException(IllegalArgumentException e) {
        log.warn("#global IllegalArgumentException handler!:", e);
        return buildResult(null, "IllegalArgument", e.getMessage());
    }

    @ExceptionHandler({BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public AispResult handBadException(BindException e) {
        log.warn("#global BindException handler!:", e);
        return buildResult(null, "IllegalArgument", e.getMessage());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public AispResult handBadException(MissingServletRequestParameterException e) {
        log.warn("#global MissingServletRequestParameterException handler!:", e);
        return buildResult(null, "IllegalArgument", e.getMessage());
    }

    @ExceptionHandler({Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public AispResult handException(Exception e) {
        log.error("#global exception handler!:"+e.getMessage(), e);
        return buildResult(null, "InternalError", ExceptionUtil.getStackTrace(e));
    }
}
