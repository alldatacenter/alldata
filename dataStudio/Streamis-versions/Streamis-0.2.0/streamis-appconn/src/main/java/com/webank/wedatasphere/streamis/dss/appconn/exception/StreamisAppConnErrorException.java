package com.webank.wedatasphere.streamis.dss.appconn.exception;

import com.webank.wedatasphere.dss.standard.common.exception.operation.ExternalOperationFailedException;

public class StreamisAppConnErrorException extends ExternalOperationFailedException {

    public StreamisAppConnErrorException(int errorCode, String message) {
        super(errorCode, message);
    }

    public StreamisAppConnErrorException(int errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
