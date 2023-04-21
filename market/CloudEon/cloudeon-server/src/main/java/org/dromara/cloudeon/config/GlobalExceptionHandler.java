package org.dromara.cloudeon.config;

import org.dromara.cloudeon.dto.ResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ResponseBody
    @ExceptionHandler(Exception.class)
    public ResultDTO<Void> globalException(HttpServletResponse response, Exception ex) {
        log.info("GlobalExceptionHandler...");
        log.info("错误代码：" + response.getStatus());
        ex.printStackTrace();
        ResultDTO<Void> resultDTO = ResultDTO.failed(ex.getCause().getMessage());
        return resultDTO;
    }

    @ResponseBody
    @ExceptionHandler( cn.dev33.satoken.exception.NotLoginException.class)
    public ResultDTO<Void> checkTokenException(HttpServletResponse response, Exception ex) {
        log.info("checkTokenException...");
        log.info("错误代码：" + response.getStatus());
        ex.printStackTrace();
        ResultDTO<Void> resultDTO = ResultDTO.failed(ex.getMessage());
        return resultDTO;
    }
}