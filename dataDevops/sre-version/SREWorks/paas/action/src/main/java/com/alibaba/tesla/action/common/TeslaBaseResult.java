package com.alibaba.tesla.action.common;

import com.alibaba.tesla.action.constant.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Tesla API后台统一的HTTP body JSON结构
 *
 * @author Adonis
 */
@JsonInclude(Include.ALWAYS)
public class TeslaBaseResult implements Serializable {

    public static final long serialVersionUID = 1L;

    public static String SUCCEED_MESG = "OK";

    /**
     * 返回码
     */
    private int code;

    /**
     * 文本消息描述
     */
    private String message;

    /**
     * 返回的详细数据
     */
    private Object data;

    public TeslaBaseResult() {
        this.code = ErrorCode.OK;
        this.message = SUCCEED_MESG;
        this.data = null;
    }

    /**
     * 成功返回
     */
    public TeslaBaseResult(Object data) {
        this.code = ErrorCode.OK;
        this.message = SUCCEED_MESG;
        this.data = data;
    }

    /**
     * 自定义失败返回数据
     */
    public TeslaBaseResult(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public TeslaBaseResult(int code, String message) {
        this.code = code;
        this.message = message;
        this.data = new HashMap<>();
    }

    public TeslaBaseResult(String message, Object data) {
        this.message = message;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

}
