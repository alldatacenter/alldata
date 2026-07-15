package com.platform.admin.util;


import java.io.PrintWriter;
import java.io.StringWriter;

public class FlinkXException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private ErrorCode errorCode;

    public FlinkXException(ErrorCode errorCode, String errorMessage) {
        super(errorCode.toString() + " - " + errorMessage);
        this.errorCode = errorCode;
    }

    private FlinkXException(ErrorCode errorCode, String errorMessage, Throwable cause) {
        super(errorCode.toString() + " - " + getMessage(errorMessage) + " - " + getMessage(cause), cause);

        this.errorCode = errorCode;
    }

    public static FlinkXException asFlinkXException(ErrorCode errorCode, String message) {
        return new FlinkXException(errorCode, message);
    }

    public static FlinkXException asFlinkXException(ErrorCode errorCode, String message, Throwable cause) {
        if (cause instanceof FlinkXException) {
            return (FlinkXException) cause;
        }
        return new FlinkXException(errorCode, message, cause);
    }

    public static FlinkXException asFlinkXException(ErrorCode errorCode, Throwable cause) {
        if (cause instanceof FlinkXException) {
            return (FlinkXException) cause;
        }
        return new FlinkXException(errorCode, getMessage(cause), cause);
    }

    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

    private static String getMessage(Object obj) {
        if (obj == null) {
            return "";
        }

        if (obj instanceof Throwable) {
            StringWriter str = new StringWriter();
            PrintWriter pw = new PrintWriter(str);
            ((Throwable) obj).printStackTrace(pw);
            return str.toString();
            // return ((Throwable) obj).getMessage();
        } else {
            return obj.toString();
        }
    }
}
