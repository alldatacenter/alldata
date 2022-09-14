package com.alibaba.tesla.action.controller;

import com.alibaba.tesla.action.common.*;
import com.alibaba.tesla.action.constant.HttpAttrs;
import com.alibaba.tesla.action.constant.HttpHeaderNames;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.*;

/**
 * Tesla API后台Controller基类
 *
 * @author dongdong.ldd@alibaba-inc.com
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class BaseController {

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    protected TeslaResultFactory teslaResultFactory;

    /**
     * 根据 Http header 中特定的用户名称头获取用户名
     *
     * @return "" if empty
     */
    public String getUserName() {
        String userName = httpServletRequest.getHeader(HttpHeaderNames.X_AUTH_USER);
        userName = userName == null ? "" : userName;
        return userName;
    }

    /**
     * 根据 Http header 中特定的用户名称头获取 USER ID
     *
     * @return "" if empty
     */
    public String getUserId() {
        String userId = httpServletRequest.getHeader(HttpHeaderNames.X_AUTH_USERID);
        userId = userId == null ? "" : userId;
        return userId;
    }

    /**
     * 根据 Http header 中特定的用户名称头获取 APP ID
     *
     * @return "" if empty
     */
    public String getAppId() {
        String appid = httpServletRequest.getHeader(HttpHeaderNames.X_AUTH_APP);
        appid = appid == null ? "" : appid;
        return appid;
    }

    /**
     * 获取应用唯一标识
     *
     * @return app id
     */
    public String getBizAppId() {
        return Optional.ofNullable(httpServletRequest.getHeader(HttpHeaderNames.X_BIZ_APP)).orElse("");
    }

    /**
     * 根据 Http header 中特定的头获取用户工号
     *
     * @return "" if empty
     */
    public String getUserEmployeeId() {
        String employeeId = httpServletRequest.getHeader(HttpHeaderNames.X_EMPL_ID);
        employeeId = employeeId == null ? "" : employeeId;
        return employeeId;
    }

    /**
     * 根据 Http header 中特定的头获取用户邮箱
     *
     * @return "" if empty
     */
    public String getUserEmailAddress() {
        String email = httpServletRequest.getHeader(HttpHeaderNames.X_EMAIL_ADDR);
        email = email == null ? "" : email;
        return email;
    }

    /**
     * 根据 Http header 获取唯一标识请求的 trace id
     *
     * @return "" if empty
     */
    public String getTraceId() {
        String traceId = httpServletRequest.getHeader(HttpHeaderNames.X_TRACE_ID);
        if (traceId == null) {
            traceId = "";
        }
        return traceId;
    }

    /**
     * 尝试获取请求端IP地址，以便后续debug和限速操作
     *
     * @return "" if empty
     */
    public String getClientIP() {
        String ipStr;
        Object clientIP = httpServletRequest.getAttribute(HttpAttrs.CLIENT_IP);
        if (clientIP == null) {
            ipStr = "";
        } else {
            ipStr = (String) clientIP;
        }
        return ipStr;
    }

    /**
     * 构建异常返回数据
     */
    public TeslaBaseResult buildExceptionResult(TeslaBaseException e) {
        return TeslaResultFactory.buildExceptionResult(e);
    }

    /**
     * 构建异常返回数据
     */
    public TeslaBaseResult buildExceptionResult(Exception e) {
        return TeslaResultFactory.buildExceptionResult(e);
    }

    /**
     * 构建正常的返回数据
     */
    public TeslaBaseResult buildSucceedResult(Object data) {
        return TeslaResultFactory.buildSucceedResult(data);
    }

    /**
     * 根据验证结果制作返回
     *
     * @param result 验证错误结果
     * @return 返回的错误内容
     */
    public TeslaBaseResult buildValidationResult(BindingResult result) {
        return TeslaResultFactory.buildValidationErrorResult(result);
    }

    /**
     * 用户自定义返回值数据
     */
    public TeslaBaseResult buildResult(int code, String message, Object data) {
        return TeslaResultFactory.buildResult(code, message, data);
    }

    /**
     * 如果参数校验有异常抛出Tesla前端参数异常，交由全局异常处理逻辑构建参数校验失败返回值
     */
    public void checkValidationBindingResult(BindingResult result) {
        if (!result.hasErrors()) {
            return;
        }
        String errMesg;
        List<String> errorMessages = new ArrayList<>();
        List<FieldError> fieldErrors = result.getFieldErrors();
        for (FieldError error : fieldErrors) {
            errorMessages.add(String.format("%s: %s", error.getField(), error.getDefaultMessage()));
        }
        errMesg = String.join(", ", errorMessages);
        throw new RuntimeException(errMesg);
    }

    /**
     * 统一处理参数异常错误 (MethodArgumentNotValidException)
     */
    @ExceptionHandler(value = {MethodArgumentNotValidException.class,})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public TeslaBaseResult methodArgumentNotValidHandler(MethodArgumentNotValidException ex) {
        return TeslaResultFactory.buildValidationErrorResult(ex.getBindingResult());
    }

    /**
     * 统一处理参数异常错误 (ConstraintViolationException)
     */
    @ExceptionHandler(value = {ConstraintViolationException.class,})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public TeslaBaseResult constraintViolationHandler(ConstraintViolationException ex) {
        Map<String, String> errorMessages = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String name = ((PathImpl) violation.getPropertyPath()).getLeafNode().getName();
            errorMessages.put(name, violation.getMessage());
        }
        return TeslaResultFactory.buildValidationErrorResult(errorMessages);
    }
}
