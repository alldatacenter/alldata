package com.aliyun.oss.model;

import java.io.IOException;

public class SelectObjectException extends IOException {
    public static final String INVALID_INPUT_STREAM = "InvalidInputStream";
    public static final String INVALID_CRC = "InvalidCRC";
    public static final String INVALID_SELECT_VERSION = "InvalidSelectVersion";
    public static final String INVALID_SELECT_FRAME = "InvalidSelectFrame";

    private String errorCode;
    private String requestId;
    public SelectObjectException(String errorCode, String message, String requestId) {
        super(message);
        this.errorCode = errorCode;
        this.requestId = requestId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getRequestId() {
        return requestId;
    }

    @Override
    public String toString() {
        return "[Message]: " + getMessage() + "\n[ErrorCode]: " + getErrorCode() + "\n[RequestId]: " + getRequestId();
    }
}
