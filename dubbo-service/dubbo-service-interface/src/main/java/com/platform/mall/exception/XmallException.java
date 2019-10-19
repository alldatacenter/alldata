package com.platform.mall.exception;

/**
 * @author wulinhao
 * @date 2019/9/24
 */
public class XmallException extends RuntimeException {

    private String msg;

    public XmallException(String msg){
        super(msg);
        this.msg=msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
