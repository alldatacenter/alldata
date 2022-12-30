package com.alibaba.tesla.tkgone.server.common;

public class FileLineInfo {

    public static String getFileName(Throwable throwable) {
        StackTraceElement ste = throwable.getStackTrace()[1];
        return ste.getFileName();
    }

    public static int getLineNumber(Throwable throwable) {
        StackTraceElement ste = throwable.getStackTrace()[1];
        return ste.getLineNumber();
    }

    public static String getClassName(Throwable throwable) {
        StackTraceElement ste = throwable.getStackTrace()[1];
        return ste.getClassName();
    }

    public static String getMethodName(Throwable throwable) {
        StackTraceElement ste = throwable.getStackTrace()[1];
        return ste.getMethodName();
    }

    public static String getTimeInfo(Throwable throwable) {
        long million = System.currentTimeMillis() % 1000;
        return String.format("[%s.%s] %s:%s", Tools.currentDataString(), million, getFileName(throwable),
            getLineNumber(throwable));
    }

}
