package com.datasophon.common.lifecycle;



/**
 * This enum is used to represent the server status, include master/worker.
 */
public enum ServerStatus {

    RUNNING(0, "The current server is running"),
    WAITING(1, "The current server is waiting, this means it cannot work"),
    STOPPED(2, "The current server is stopped"),
    ;

    private final int code;
    private final String desc;

    ServerStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
