package com.alibaba.sreworks.dataset.common;

import com.alibaba.sreworks.dataset.common.exception.*;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;
import com.alibaba.tesla.common.base.constant.TeslaStatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import static com.alibaba.sreworks.dataset.common.DatasetCodeEnum.*;

@Slf4j
@ControllerAdvice
public class DatasetExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public TeslaBaseResult httpMessageNotReadableException(HttpMessageNotReadableException e) {
        return TeslaResultFactory.buildResult(TeslaStatusCode.USER_ARG_ERROR, e.getMessage(), "");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public TeslaBaseResult illegalArgumentException(IllegalArgumentException e) {
        log.error("Dataset Illegal Argument Exception Occurred:", e);
        return TeslaResultFactory.buildResult(TeslaStatusCode.USER_ARG_ERROR, e.getMessage(), "");
    }

    @ExceptionHandler(ParamException.class)
    @ResponseBody
    public TeslaBaseResult paramException(ParamException e) {
        log.error("Dataset Param Exception Occurred:", e);
        return TeslaResultFactory.buildResult(ParamErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(RequestException.class)
    @ResponseBody
    public TeslaBaseResult requestException(RequestException e) {
        log.error("Dataset Request Exception Occurred:", e);
        return TeslaResultFactory.buildResult(RequestErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(SubjectNotExistException.class)
    @ResponseBody
    public TeslaBaseResult subjectNotExistException(SubjectNotExistException e) {
        log.error("Dataset Subject Not Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(SubjectNotExistErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(DomainExistException.class)
    @ResponseBody
    public TeslaBaseResult domainExistException(DomainExistException e) {
        log.error("Dataset Domain Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(DomainExistErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(DomainNotExistException.class)
    @ResponseBody
    public TeslaBaseResult domainNotExistException(DomainNotExistException e) {
        log.error("Dataset Domain Not Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(DomainNotExistErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(ModelExistException.class)
    @ResponseBody
    public TeslaBaseResult modelExistException(ModelExistException e) {
        log.error("Dataset Model Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(ModelExistErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(ModelNotExistException.class)
    @ResponseBody
    public TeslaBaseResult modelNotExistException(ModelNotExistException e) {
        log.error("Dataset Model Not Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(ModelNotExistErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(InterfaceExistException.class)
    @ResponseBody
    public TeslaBaseResult interfaceExistException(InterfaceExistException e) {
        log.error("Dataset Interface Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(InterfaceExistErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(InterfaceNotExistException.class)
    @ResponseBody
    public TeslaBaseResult interfaceNotExistException(InterfaceNotExistException e) {
        log.error("Dataset Interface Not Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(InterfaceNotExistErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(InterfaceConfigException.class)
    @ResponseBody
    public TeslaBaseResult interfaceConfigException(InterfaceConfigException e) {
        log.error("Dataset Interface Config Exception Occurred:", e);
        return TeslaResultFactory.buildResult(InterfaceConfigErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public TeslaBaseResult exception(Exception e) {
        log.error("Dataset Server Exception Occurred:", e);
        return TeslaResultFactory.buildResult(TeslaStatusCode.SERVER_ERROR, e.getMessage(), "");
    }
}
