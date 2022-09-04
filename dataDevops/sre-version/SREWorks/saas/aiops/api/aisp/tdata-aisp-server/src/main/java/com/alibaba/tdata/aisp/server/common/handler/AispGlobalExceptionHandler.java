package com.alibaba.tdata.aisp.server.common.handler;

import javax.servlet.http.HttpServletResponse;

import com.alibaba.tdata.aisp.server.common.constant.AispResult;
import com.alibaba.tdata.aisp.server.common.exception.DetectorException;
import com.alibaba.tdata.aisp.server.common.exception.PlatformInternalException;

import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.alibaba.tdata.aisp.server.common.factory.AispResponseFactory.buildResult;

/**
 * @ClassName: GlobalExceptionHandler
 * @Author: dyj
 * @DATE: 2021-04-01
 * @Description:
 **/
@Slf4j
@RestControllerAdvice
public class AispGlobalExceptionHandler {
    @Autowired
    private HttpServletResponse response;

    @ExceptionHandler({IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public AispResult handBadException(IllegalArgumentException e) {
        log.warn("#global IllegalArgumentException handler!:", e);
        return buildResult(null, 400, e.getMessage());
    }

    @ExceptionHandler({BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public AispResult handBadException(BindException e) {
        log.warn("#global BindException handler!:", e);
        return buildResult(null, 400, e.getMessage());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public AispResult handBadException(MissingServletRequestParameterException e) {
        log.warn("#global MissingServletRequestParameterException handler!:", e);
        return buildResult(null, 400, e.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public AispResult handBadException(HttpMessageNotReadableException e) {
        log.warn("#global HttpMessageNotReadableException handler!:", e);
        return buildResult(null, 400, e.getMessage());
    }

    @ExceptionHandler({InvalidDefinitionException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public AispResult handBadException(InvalidDefinitionException e) {
        log.warn("#global InvalidDefinitionException handler!:", e);
        return buildResult(null, 400, e.getMessage());
    }

    @ExceptionHandler({HttpMediaTypeNotSupportedException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public AispResult handBadException(HttpMediaTypeNotSupportedException e) {
        log.warn("#global HttpMediaTypeNotSupportedException handler!:", e);
        return buildResult(null, 400, e.getMessage());
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public AispResult handBadException(HttpRequestMethodNotSupportedException e) {
        log.warn("#global exception handler!:", e);
        return buildResult(null, 400, e.getMessage());
    }

    @ExceptionHandler({PlatformInternalException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public AispResult handBadException(PlatformInternalException e) {
        log.warn("#global PlatformInternalException handler!message: {} Cause: {}", e.getMessage(), e);
        return buildResult(e.getTaskUUID(), 500, e.getMessage());
    }

    @ExceptionHandler({DetectorException.class})
    @ResponseBody
    public AispResult handBadException(DetectorException e) {
        log.warn("#global DetectorException handler!:", e);
        response.setStatus(e.getHttpStatus());
        return buildResult(e.getTaskUUID(), e.getCode(), e.getMessage());
    }

    @ExceptionHandler({Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public AispResult handException(Exception e) {
        log.warn("#global exception handler!:", e);
        return buildResult(null, 500, e.getMessage());
    }
}
