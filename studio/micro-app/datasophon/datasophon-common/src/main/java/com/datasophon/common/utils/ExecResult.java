package com.datasophon.common.utils;

import lombok.Data;

import java.io.Serializable;

public class ExecResult implements Serializable {

    private boolean execResult = false;

    private String execOut;

    private String execErrOut;

    public String getExecErrOut() {
        return execErrOut;
    }

    public void setExecErrOut(String execErrOut) {
        this.execErrOut = execErrOut;
    }

    public boolean getExecResult() {
        return execResult;
    }

    public void setExecResult(boolean execResult) {
        this.execResult = execResult;
    }

    public String getExecOut() {
        return execOut;
    }

    public void setExecOut(String execOut) {
        this.execOut = execOut;
    }
}
