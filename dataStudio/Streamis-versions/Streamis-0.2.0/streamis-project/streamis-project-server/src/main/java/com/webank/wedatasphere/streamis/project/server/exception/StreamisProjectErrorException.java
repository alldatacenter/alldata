package com.webank.wedatasphere.streamis.project.server.exception;

import org.apache.linkis.common.exception.ErrorException;

/**
 * Description:
 */
public class StreamisProjectErrorException extends ErrorException {


    public StreamisProjectErrorException(int errCode, String desc) {
        super(errCode, desc);
    }

    public StreamisProjectErrorException(int errorCode, String desc, Throwable throwable){
        super(errorCode, desc);
        this.initCause(throwable);
    }

}
