package com.alibaba.sreworks.health.common;

import com.alibaba.sreworks.health.common.exception.*;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;
import com.alibaba.tesla.common.base.constant.TeslaStatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import static com.alibaba.sreworks.health.common.HealthCodeEnum.*;

@Slf4j
@ControllerAdvice
public class HealthExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public TeslaBaseResult httpMessageNotReadableException(HttpMessageNotReadableException e) {
        return TeslaResultFactory.buildResult(TeslaStatusCode.USER_ARG_ERROR, e.getMessage(), "");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public TeslaBaseResult illegalArgumentException(IllegalArgumentException e) {
        log.error("Health Illegal Argument Exception Occurred:", e);
        return TeslaResultFactory.buildResult(TeslaStatusCode.USER_ARG_ERROR, e.getMessage(), "");
    }

    @ExceptionHandler(ParamException.class)
    @ResponseBody
    public TeslaBaseResult paramException(ParamException e) {
        log.error("Health Param Exception Occurred:", e);
        return TeslaResultFactory.buildResult(ParamErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(CommonDefinitionExistException.class)
    @ResponseBody
    public TeslaBaseResult definitionExistException(CommonDefinitionExistException e) {
        log.error("Health Common Definition Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(DefinitionExistErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(CommonDefinitionNotExistException.class)
    @ResponseBody
    public TeslaBaseResult definitionNotExistException(CommonDefinitionNotExistException e) {
        log.error("Health Common Definition Not Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(DefinitionNotExistErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(CommonDefinitionDeleteException.class)
    @ResponseBody
    public TeslaBaseResult definitionDeleteException(CommonDefinitionDeleteException e) {
        log.error("Health Common Definition Delete Exception Occurred:", e);
        return TeslaResultFactory.buildResult(DefinitionDeleteErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(ShieldRuleExistException.class)
    @ResponseBody
    public TeslaBaseResult shieldRuleExistException(ShieldRuleExistException e) {
        log.error("Health Shield Rule Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(ShieldRuleExitErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(ShieldRuleNotExistException.class)
    @ResponseBody
    public TeslaBaseResult shieldRuleNotExistException(ShieldRuleNotExistException e) {
        log.error("Health Shield Rule Not Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(ShieldRuleNotExitErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(ShieldRuleDeleteException.class)
    @ResponseBody
    public TeslaBaseResult shieldRuleDeleteException(ShieldRuleDeleteException e) {
        log.error("Health Shield Rule Delete Exception Occurred:", e);
        return TeslaResultFactory.buildResult(ShieldRuleDeleteErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(ConvergenceRuleExistException.class)
    @ResponseBody
    public TeslaBaseResult convergenceRuleExistException(ConvergenceRuleExistException e) {
        log.error("Health Convergence Rule Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(ConvergenceRuleExitErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(ConvergenceRuleNotExistException.class)
    @ResponseBody
    public TeslaBaseResult convergenceRuleNotExistException(ConvergenceRuleNotExistException e) {
        log.error("Health Convergence Rule Not Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(ConvergenceRuleNotExitErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(ConvergenceRuleDeleteException.class)
    @ResponseBody
    public TeslaBaseResult convergenceRuleDeleteException(ConvergenceRuleDeleteException e) {
        log.error("Health Convergence Rule Delete Exception Occurred:", e);
        return TeslaResultFactory.buildResult(ConvergenceRuleDeleteErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(IncidentTypeExistException.class)
    @ResponseBody
    public TeslaBaseResult incidentTypeExistException(IncidentTypeExistException e) {
        log.error("Health Incident Type Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(IncidentTypeExitErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(IncidentTypeNotExistException.class)
    @ResponseBody
    public TeslaBaseResult incidentTypeNotExistException(IncidentTypeNotExistException e) {
        log.error("Health Incident Type Not Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(IncidentTypeNotExitErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(IncidentTypeDeleteException.class)
    @ResponseBody
    public TeslaBaseResult incidentTypeDeleteException(IncidentTypeDeleteException e) {
        log.error("Health Incident Type Delete Exception Occurred:", e);
        return TeslaResultFactory.buildResult(IncidentTypeDeleteErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(IncidentInstanceExistException.class)
    @ResponseBody
    public TeslaBaseResult incidentInstanceExistException(IncidentInstanceExistException e) {
        log.error("Health Incident Instance Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(IncidentInstanceExitErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(IncidentInstanceNotExistException.class)
    @ResponseBody
    public TeslaBaseResult incidentInstanceNotExistException(IncidentInstanceNotExistException e) {
        log.error("Health Incident Instance Not Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(IncidentInstanceNotExitErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(FailureInstanceExistException.class)
    @ResponseBody
    public TeslaBaseResult failureInstanceExistException(FailureInstanceExistException e) {
        log.error("Health Failure Instance Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(FailureInstanceExitErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(FailureInstanceNotExistException.class)
    @ResponseBody
    public TeslaBaseResult failureInstanceNotExistException(FailureInstanceNotExistException e) {
        log.error("Health Failure Instance Not Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(FailureInstanceNotExitErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(AlertInstanceExistException.class)
    @ResponseBody
    public TeslaBaseResult alertInstanceExistException(AlertInstanceExistException e) {
        log.error("Health Alert Instance Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(AlertInstanceExitErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(AlertInstanceNotExistException.class)
    @ResponseBody
    public TeslaBaseResult alertInstanceNotExistException(AlertInstanceNotExistException e) {
        log.error("Health Alert Instance Not Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(AlertInstanceNotExitErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(RiskTypeExistException.class)
    @ResponseBody
    public TeslaBaseResult riskTypeExistException(RiskTypeExistException e) {
        log.error("Health Risk Type Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(RiskTypeExitErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(RiskTypeNotExistException.class)
    @ResponseBody
    public TeslaBaseResult riskTypeNotExistException(RiskTypeNotExistException e) {
        log.error("Health Risk Type Not Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(RiskTypeNotExitErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(RiskTypeDeleteException.class)
    @ResponseBody
    public TeslaBaseResult incidentTypeDeleteException(RiskTypeDeleteException e) {
        log.error("Health Risk Type Delete Exception Occurred:", e);
        return TeslaResultFactory.buildResult(RiskTypeDeleteErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(RiskInstanceExistException.class)
    @ResponseBody
    public TeslaBaseResult riskInstanceExistException(RiskInstanceExistException e) {
        log.error("Health Risk Instance Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(RiskInstanceExitErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(RiskInstanceNotExistException.class)
    @ResponseBody
    public TeslaBaseResult riskInstanceNotExistException(RiskInstanceNotExistException e) {
        log.error("Health Risk Instance Not Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(RiskInstanceNotExitErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(EventInstanceExistException.class)
    @ResponseBody
    public TeslaBaseResult eventInstanceExistException(EventInstanceExistException e) {
        log.error("Health Event Instance Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(EventInstanceExitErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(EventInstanceNotExistException.class)
    @ResponseBody
    public TeslaBaseResult eventInstanceNotExistException(EventInstanceNotExistException e) {
        log.error("Health Event Instance Not Exist Exception Occurred:", e);
        return TeslaResultFactory.buildResult(EventInstanceNotExitErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(RequestException.class)
    @ResponseBody
    public TeslaBaseResult requestException(RequestException e) {
        log.error("Health Request Exception Occurred:", e);
        return TeslaResultFactory.buildResult(RequestErr.code, e.getMessage(), "");
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public TeslaBaseResult exception(Exception e) {
        log.error("Health Server Exception Occurred:", e);
        return TeslaResultFactory.buildResult(TeslaStatusCode.SERVER_ERROR, e.getMessage(), "");
    }
}
