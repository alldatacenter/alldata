package com.platform.admin.exception;

import com.baomidou.mybatisplus.extension.api.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Created by jwk on 2023/01/05.
 * 全局异常处理
 * @author Jing WenKai
 * @date 2023/01/05 11:57
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public R handleException(Exception e){
        log.error("系统异常{0}",e);
        return R.failed(e.getMessage());
    }
}
