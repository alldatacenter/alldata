package com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.exception;

import org.apache.linkis.common.exception.ExceptionLevel;
import org.apache.linkis.common.exception.LinkisRuntimeException;

public class StreamisJobLaunchException extends FlinkJobLaunchErrorException{

    public StreamisJobLaunchException(int errorCode, String errorMsg, Throwable t) {
        super(errorCode, errorMsg, t);
    }

    public static class Runtime extends LinkisRuntimeException {

        public Runtime(int errCode, String desc,Throwable t) {
            super(errCode, desc);
            super.initCause(t);
        }

        @Override
        public ExceptionLevel getLevel() {
            return ExceptionLevel.ERROR;
        }
    }
}
