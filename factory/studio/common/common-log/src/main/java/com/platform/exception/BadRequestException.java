
package com.platform.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * @author AllDataDC
 * @date 2023-01-27
 * 统一异常处理
 */
@Getter
public class BadRequestException extends RuntimeException{

    private Integer status = BAD_REQUEST.value();

    public BadRequestException(String msg){
        super(msg);
    }

    public BadRequestException(HttpStatus status,String msg){
        super(msg);
        this.status = status.value();
    }
}
