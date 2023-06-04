package com.datasophon.common.model;

/**
 * @Description: 命令返回结果
 * @author: gaodayu
 * @date: 2022-03-25 21:03
 */
public class ExecCmdResult {

    // 命令执行是否成功
    private boolean success;

    // 输出结果
    private String result;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
