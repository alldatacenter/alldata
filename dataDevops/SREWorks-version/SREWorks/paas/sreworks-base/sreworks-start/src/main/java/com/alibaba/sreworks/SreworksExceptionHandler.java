package com.alibaba.sreworks;

import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ControllerAdvice
public class SreworksExceptionHandler {

    public SreworksExceptionHandler() {
    }

    @ExceptionHandler({ApiException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public TeslaBaseResult handApiException(ApiException e) {
        log.error("ApiException: \n" + e.getResponseBody(), e);
        return TeslaResultFactory.buildResult(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            e.getLocalizedMessage(),
            e.getResponseBody()
        );
    }

}
