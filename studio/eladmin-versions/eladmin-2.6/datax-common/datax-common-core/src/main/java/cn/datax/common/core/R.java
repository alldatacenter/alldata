package cn.datax.common.core;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class R implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private boolean success;
    private int code;
    private String msg;
    private Object data;
    private long timestamp;
	
	private R() {}
    
    public static R error() {
        return error(null);
    }

    public static R error(String message) {
        return error(null, message);
    }

    public static R error(Integer code, String message) {
        if(code == null) {
            code = 500;
        }
        if(message == null) {
            message = "服务器内部错误";
        }
        return build(code, false, message);
    }
    
    public static R ok() {
        return ok(null);
    }

    public static R ok(String message) {
        return ok(null, message);
    }

    public static R ok(Integer code, String message) {
        if(code == null) {
            code = 200;
        }
        if(message == null) {
            message = "操作成功";
        }
        return build(code, true, message);
    }
    
    public static R build(int code, boolean success, String message) {
        return new R()
                .setCode(code)
                .setSuccess(success)
                .setMessage(message)
                .setTimestamp(System.currentTimeMillis());
    }
    
    public R setCode(int code) {
        this.code = code;
        return this;
    }

    public R setSuccess(boolean success) {
        this.success = success;
        return this;
    }
    public R setMessage(String msg) {
        this.msg = msg;
        return this;
    }

    public R setData(Object data) {
        this.data = data;
        return this;
    }

    public R setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }
    
}
