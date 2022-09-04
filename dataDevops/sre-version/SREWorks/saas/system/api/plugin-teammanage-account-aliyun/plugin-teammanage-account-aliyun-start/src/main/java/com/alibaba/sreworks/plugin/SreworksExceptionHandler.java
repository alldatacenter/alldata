package com.alibaba.sreworks.plugin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

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
