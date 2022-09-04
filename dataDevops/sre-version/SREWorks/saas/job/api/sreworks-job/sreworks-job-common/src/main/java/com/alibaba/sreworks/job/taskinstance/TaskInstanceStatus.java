package com.alibaba.sreworks.job.taskinstance;

public enum TaskInstanceStatus {

    INIT,

    RUNNING,

    WAIT_RETRY,

    EXCEPTION,

    SUCCESS,

    TIMEOUT,

    CANCELLED,

    STOPPED

}
