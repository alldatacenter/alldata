package com.alibaba.tesla.action.constant;

import java.io.Serializable;

/**
 * @author tandong.td@alibaba-inc.com
 *
 * Created by tandong on 2017/8/14.
 */
public class TeslaResult implements Serializable {


    public static final int SUCCESS = 200;

    public static final int FAILURE = 500;

    /**
     * 登录认证失败
     */
    public static final int NOAUTH = 401;

    /**
     * 参数错误
     */
    public static final int BAD_REQUEST = 400;

    /**
     * 无权限
     */
    public static final int NO_PERMISSION = 403;

    /**
     * 业务异常
     */
    public static final int BIZ_ERROR = 10000;

    /**
     * 接收标识 true -成功 false-失败
     */
    private int code;

    /**
     * 返回的数据JSON格式
     */
    private Object data;

    /**
     * 消息内容
     */
    private String message;

    public TeslaResult() {}

    public TeslaResult(int code, Object data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
