package com.alibaba.tesla.tkgone.server.common.http;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.http.HttpStatus;

/**
 * @author feiquan
 */
@AllArgsConstructor
@Data
public class RestResponse<T> {
    private int code;
    private String message;
    private T data;

    public RestResponse() {
        this(HttpStatus.SC_OK, null, null);
    }

    public RestResponse(T data) {
        this(HttpStatus.SC_OK, null, data);
    }
}

