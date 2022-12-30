package com.alibaba.tesla.action.common;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.tesla.action.constant.ErrorCode.*;

/**
 * 用来生成Tesla result格式返回值的factory, 单例注入到Spring IoC容器中
 *
 * @author dongdong.ldd@alibaba-inc.com
 */
@Component
public class TeslaResultFactory {

    public static final String TESLA_CODE = "TESLA_CODE";

    public static TeslaBaseResult buildSucceedResult() {
        setTeslaCode(OK);
        return new TeslaBaseResult(EMPTY_OBJ);
    }

    public static TeslaBaseResult buildSucceedResult(Object data) {
        TeslaBaseResult ret = buildSucceedResult();
        ret.setData(data);
        return ret;
    }

    public static TeslaBaseResult buildSucceedResult(Object data, String message) {
        TeslaBaseResult ret = buildSucceedResult();
        ret.setMessage(message);
        ret.setData(data);
        return ret;
    }

    public static TeslaBaseResult buildExceptionResult(TeslaBaseException e) {
        String errMessage = e.getErrMessage();
        setTeslaCode(e.getErrCode());
        return new TeslaBaseResult(e.getErrCode(), errMessage);
    }

    /**
     * 构建参数错误返回数据
     *
     * @param data 参数错误对应关系，key=错误字段名，value=错误原因
     */
    public static TeslaBaseResult buildValidationErrorResult(Map<String, String> data) {
        setTeslaCode(VALIDATION_ERROR);

        TeslaBaseResult ret = new TeslaBaseResult();
        ret.setCode(USER_ARG_ERROR);
        ret.setMessage(validationMessage(data));
        ret.setData(transformValidationItemMap(data));
        return ret;
    }

    /**
     * 构建参数错误返回数据
     *
     * @param field        字段名称
     * @param errorMessage 错误信息
     */
    public static TeslaBaseResult buildValidationErrorResult(String field, String errorMessage) {
        setTeslaCode(VALIDATION_ERROR);
        Map<String, String> data = new HashMap<>(1);
        data.put(field, errorMessage);

        TeslaBaseResult ret = new TeslaBaseResult();
        ret.setCode(USER_ARG_ERROR);
        ret.setMessage(validationMessage(data));
        ret.setData(data);
        return ret;
    }

    /**
     * 构造参数错误返回数据
     *
     * @param result 验证结果
     */
    public static TeslaBaseResult buildValidationErrorResult(BindingResult result) {
        setTeslaCode(VALIDATION_ERROR);
        Map<String, String> data = new HashMap<>(10);
        List<FieldError> fieldErrors = result.getFieldErrors();
        for (FieldError error : fieldErrors) {
            data.put(error.getField(), error.getDefaultMessage());
        }

        TeslaBaseResult ret = new TeslaBaseResult();
        ret.setCode(CLIENT_ERROR);
        ret.setMessage(validationMessage(data));
        ret.setData(transformValidationItemMap(data));
        return ret;
    }

    /**
     * 构建客户端异常信息
     *
     * @param errorMessage 错误信息
     */
    public static TeslaBaseResult buildClientErrorResult(String errorMessage) {
        setTeslaCode(CLIENT_ERROR);
        TeslaBaseResult ret = new TeslaBaseResult();
        ret.setCode(CLIENT_ERROR);
        ret.setMessage(errorMessage);
        ret.setData(EMPTY_OBJ);
        return ret;
    }

    /**
     * 构建错误返回结构
     *
     * @param data
     * @param msg
     * @return
     */
    public static TeslaBaseResult buildErrorResult(Object data, String msg) {
        setTeslaCode(SERVER_ERROR);
        TeslaBaseResult ret = new TeslaBaseResult();
        ret.setCode(SERVER_ERROR);
        ret.setMessage(msg);
        ret.setData(data);
        return ret;
    }

    @Deprecated
    public static TeslaBaseResult buildValidationResult(BindingResult result) {
        return buildValidationErrorResult(result);
    }

    /**
     * 构建找不到的错误返回数据
     */
    public static TeslaBaseResult buildNotFoundResult() {
        setTeslaCode(NOT_FOUND);
        TeslaBaseResult ret = new TeslaBaseResult();
        ret.setCode(NOT_FOUND);
        ret.setMessage("not found");
        ret.setData(EMPTY_DATA);
        return ret;
    }

    /**
     * 构建无权限的错误返回
     */
    public static TeslaBaseResult buildForbiddenResult(String message) {
        setTeslaCode(FORBIDDEN);
        TeslaBaseResult ret = new TeslaBaseResult();
        ret.setCode(FORBIDDEN);
        ret.setMessage(message);
        ret.setData(EMPTY_DATA);
        return ret;
    }

    /**
     * 构建无权限的错误返回
     */
    public static TeslaBaseResult buildForbiddenResult() {
        return buildForbiddenResult("forbidden");
    }

    public static TeslaBaseResult buildExceptionResult(Exception e) {
        setTeslaCode(SERVER_ERROR);
        TeslaBaseResult result = new TeslaBaseResult();
        result.setCode(SERVER_ERROR);
        result.setMessage(String.format("Internal Server Error：%s", e.getMessage()));
        result.setData(EMPTY_OBJ);
        return result;
    }

    public static TeslaBaseResult buildResult(int code, String message, Object data) {
        setTeslaCode(code);
        return new TeslaBaseResult(code, message, data);
    }

    public static TeslaBaseResult buildResult(int code, String message) {
        setTeslaCode(code);
        return new TeslaBaseResult(code, message, EMPTY_OBJ);
    }

    /**
     * 用于监控指标，在 request 中设置 TESLA_CODE attribute
     *
     * @param code 实际代码
     */
    private static void setTeslaCode(Integer code) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return;
        }
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        if (request == null) {
            return;
        }
        request.setAttribute(TESLA_CODE, code);
    }

    /**
     * 组合验证字段失败的 map 为一句话
     *
     * @param data 验证失败字典
     * @return
     */
    private static String validationMessage(Map<String, String> data) {
        List<String> errorMessages = new ArrayList<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            errorMessages.add(entry.getKey() + ": " + entry.getValue());
        }
        return "Validation Error: " + String.join(", ", errorMessages);
    }

    public static List<ValidationItemMessage> transformValidationItemMap(Map<String, String> data) {
        List<ValidationItemMessage> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            result.add(ValidationItemMessage.builder()
                .field(entry.getKey())
                .message(entry.getValue())
                .build());
        }
        return result;
    }

    @Data
    @Builder
    public static class ValidationItemMessage {
        private String field;
        private String message;
    }
}
