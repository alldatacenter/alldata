package com.platform.mall.exception;

/**
 * @author wulinhao
 */
public class XmallUploadException extends RuntimeException {

    private String msg;

    public XmallUploadException(String msg){
        super(msg);
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
