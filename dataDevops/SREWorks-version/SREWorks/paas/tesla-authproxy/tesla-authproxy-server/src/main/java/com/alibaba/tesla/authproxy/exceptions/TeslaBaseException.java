package com.alibaba.tesla.authproxy.exceptions;

import com.alibaba.tesla.authproxy.constants.ErrorCode;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class TeslaBaseException extends Exception {
    private static final long serialVersionUID = 1L;

    protected int errCode = ErrorCode.SERVER_ERROR; // errCode 错误码，对应于API返回值的code字段
    protected String errMessage = "";   // errMessage 错误信息，对应于API返回值的message字段
    protected Object errData = null;    // 出错详情数据，对应于API返回值的data字段

    public TeslaBaseException() {
        super();
    }

    public TeslaBaseException(String message) {
        super(message);
        this.errMessage = message;
    }

    public TeslaBaseException(Throwable cause) {
        super(cause);
    }

    public TeslaBaseException(String message, Throwable cause) {
        super(message, cause);
        this.errMessage = message;
    }

    /**
     * 通过异常明确指定异常对应的返回值格式
     */
    public TeslaBaseException(int errCode, String message, Object data) {
        super(message);
        this.errCode = errCode;
        this.errMessage = message;
        this.errData = data;
    }

    /**
     * 部分异常只需要指明errCode和错误信息，不需要data详情
     */
    public TeslaBaseException(int errCode, String message) {
        super(message);
        this.errCode = errCode;
        this.errMessage = message;
    }

    /**
     * 通过异常明确指定异常对应的返回值数据，同时兼容标准异常的cause信息
     */
    public TeslaBaseException(int errCode, String message, Object data, Throwable cause) {
        super(message, cause);
        this.errCode = errCode;
        this.errMessage = message;
        this.errData = data;
    }

    public int getErrCode() {
        return errCode;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public Object getErrData() {
        return errData;
    }
}
