package com.alibaba.tesla.tkgone.server.common;

public class LocalSys {
    public static int getCpuNum() {
        return Runtime.getRuntime().availableProcessors();
    }
}
