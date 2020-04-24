package com.platform.website.module;

import org.apache.commons.lang3.StringUtils;

/**
 * 返回结果
 * 
 * @author wulinhao
 * 
 */
public class Message {

    private final static int OK = 200;
    private final static int ERROR = 500;
    private final static int NO_CONTENT = 204;
    private final static int BAD_REQUEST = 400;

    public static class MessageEntry {
        private int code;
        private String msg;

        public MessageEntry(int code, String msg) {
            super();
            this.code = code;
            this.msg = msg;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }
    }

    public static class SuccessMsg extends MessageEntry {

        public SuccessMsg(int code, Object object) {
            super(code, "ok");
            this.data = object;
        }

        private Object data;

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }

    public static class ErrorMsg extends MessageEntry {
        public ErrorMsg(int code, String message) {
            super(code, StringUtils.isEmpty(message) ? "error" : message);
        }
    }

    public static class NoContentMsg extends MessageEntry {
        public NoContentMsg(int code, String message) {
            super(code, StringUtils.isEmpty(message) ? "no content" : message);
        }
    }

    public static class BadRequestMsg extends MessageEntry {
        public BadRequestMsg(int code, String message) {
            super(code, StringUtils.isEmpty(message) ? "bad request" : message);
        }
    }

    /**
     * 正常返回并且有值
     * 
     * @param result
     * @return
     */
    public static MessageEntry ok(Object result) {
        return new SuccessMsg(OK, result);
    }

    /**
     * 服务器发送异常
     * 
     * @param message
     * @return
     */
    public static MessageEntry error(String message) {
        return new ErrorMsg(ERROR, message);
    }

    /**
     * 服务器正常，但是没有要查询的值
     * 
     * @param message
     * @return
     */
    public static MessageEntry noContent(String message) {
        return new NoContentMsg(NO_CONTENT, message);
    }

    /**
     * 错误的请求
     * 
     * @param message
     * @return
     */
    public static MessageEntry badRequest(String message) {
        return new BadRequestMsg(BAD_REQUEST, message);
    }
}