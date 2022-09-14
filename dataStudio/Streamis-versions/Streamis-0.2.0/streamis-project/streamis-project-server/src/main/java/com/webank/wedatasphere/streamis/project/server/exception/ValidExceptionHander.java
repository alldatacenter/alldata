package com.webank.wedatasphere.streamis.project.server.exception;

import org.apache.commons.lang.StringUtils;
import org.apache.linkis.server.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.Set;

@RestControllerAdvice
public class ValidExceptionHander {

    private static final Logger LOG = LoggerFactory.getLogger(ValidExceptionHander.class);

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Message handle(MethodArgumentNotValidException e){
        LOG.error("Request parameter validation exception", e);
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder stringBuilder = new StringBuilder();
        bindingResult.getFieldErrors().forEach((item) -> stringBuilder.append(item.getDefaultMessage()).append(";"));
        return Message.error("failed to validate request parameter, detail:"+stringBuilder.toString());
    }

    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    public Message handle(MethodArgumentTypeMismatchException e){
        LOG.error("Request parameter validation exception", e);
        return Message.error("failed to validate request parameter, detail:"+e.getMessage());
    }

    @ExceptionHandler(value = ConstraintViolationException.class)
    public Message handle(ConstraintViolationException e){
        LOG.error("Request parameter validation exception", e);
        ArrayList<Object> list = new ArrayList<>();
        Set<ConstraintViolation<?>> constraintViolations = e.getConstraintViolations();
        for (ConstraintViolation<?> violation : constraintViolations) {
            list.add(violation.getMessage());
        }
        return Message.error("failed to validate request parameter, detail:"+ StringUtils.join(list, ","));
    }



}
