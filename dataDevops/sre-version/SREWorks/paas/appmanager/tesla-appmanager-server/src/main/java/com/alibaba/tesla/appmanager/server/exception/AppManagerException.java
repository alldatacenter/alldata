package com.alibaba.tesla.appmanager.server.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author qianmo.zm@alibaba-inc.com
 * @date 2017/11/07.
 */
@AllArgsConstructor
public class AppManagerException extends Exception {
    @Getter
    private String code;

    @Getter
    private String message;
}
