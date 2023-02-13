package com.alibaba.sreworks;

import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

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

    static private boolean initialized = false;

    @InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        if (!initialized) {
            System.out.println("[Info] your spring is safe now.");
            initialized = true;
        }
        dataBinder.setDisallowedFields("class.*", "Class.*", "*.class.*", "*.Class.*");
    }

}
