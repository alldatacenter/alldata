package com.alibaba.tesla.authproxy.web.common;

import com.alibaba.tesla.common.utils.TeslaResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 专有云 Controller 基类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class PrivateBaseController {

    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public TeslaResult validationHandler(MethodArgumentNotValidException ex) {
        return buildValidationResult(ex.getBindingResult());
    }

    /**
     * 根据验证结果制作返回
     *
     * @param result 验证错误结果
     * @return 返回的错误内容
     */
    protected TeslaResult buildValidationResult(BindingResult result) {
        Map<String, String> errorMap = new HashMap<>(10);
        List<FieldError> fieldErrors = result.getFieldErrors();
        for (FieldError error : fieldErrors) {
            errorMap.put(error.getField(), error.getDefaultMessage());
        }
        return PrivateResultBuilder.buildExtValidationErrorResult(errorMap);
    }

}
