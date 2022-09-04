package com.alibaba.sreworks.job;

import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@ControllerAdvice
public class SreworksExceptionHandler {

    public SreworksExceptionHandler() {
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
