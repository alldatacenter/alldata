package com.alibaba.sreworks.pmdb.common;

import com.alibaba.sreworks.pmdb.common.exception.*;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;
import com.alibaba.tesla.common.base.constant.TeslaStatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import static com.alibaba.sreworks.pmdb.common.PmdbCodeEnum.*;

@Slf4j
@ControllerAdvice
public class PmdbExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public TeslaBaseResult httpMessageNotReadableException(HttpMessageNotReadableException e) {
        return TeslaResultFactory.buildResult(TeslaStatusCode.USER_ARG_ERROR, e.getMessage(), "");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public TeslaBaseResult illegalArgumentException(IllegalArgumentException e) {
        log.error("Illegal Argument Exception Occurred:", e);
        return TeslaResultFactory.buildResult(TeslaStatusCode.USER_ARG_ERROR, e.getMessage(), "");
    }

    @ExceptionHandler(ParamException.class)
    @ResponseBody
    public TeslaBaseResult paramException(ParamException e) {
        log.error("Param Exception Occurred:", e);
        return TeslaResultFactory.buildResult(ParamErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(RequestException.class)
    @ResponseBody
    public TeslaBaseResult requestException(RequestException e) {
        log.error("Request Exception Occurred:", e);
        return TeslaResultFactory.buildResult(RequestErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(DataSourceNotExistException.class)
    @ResponseBody
    public TeslaBaseResult datasourceNotExistException(DataSourceNotExistException e) {
        log.error("Datasource Not Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(DatasourceNotExistErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(MetricExistException.class)
    @ResponseBody
    public TeslaBaseResult metricExistException(MetricExistException e) {
        log.error("Metric Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(MetricExistErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(MetricNotExistException.class)
    @ResponseBody
    public TeslaBaseResult metricNotExistException(MetricNotExistException e) {
        log.error("Metric Not Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(MetricNotExistErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(MetricInstanceExistException.class)
    @ResponseBody
    public TeslaBaseResult metricInstanceExistException(MetricInstanceExistException e) {
        log.error("Metric Instance Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(MetricInstanceExistErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(MetricInstanceNotExistException.class)
    @ResponseBody
    public TeslaBaseResult metricInstanceNotExistException(MetricInstanceNotExistException e) {
        log.error("Metric Instance Not Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(MetricInstanceNotExistErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(MetricAnomalyDetectionConfigExistException.class)
    @ResponseBody
    public TeslaBaseResult metricAnomalyDetectionConfigExistException(MetricAnomalyDetectionConfigExistException e) {
        log.error("Metric Anomaly Detection Config Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(MetricAnomalyDetectionConfigExistErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public TeslaBaseResult exception(Exception e) {
        log.error("Server Exception Occurred:", e);
        return TeslaResultFactory.buildResult(TeslaStatusCode.SERVER_ERROR, e.getMessage(), "");
    }
}
