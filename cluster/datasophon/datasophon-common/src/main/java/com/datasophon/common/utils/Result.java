
package com.datasophon.common.utils;

import com.datasophon.common.Constants;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;


@Data
public class Result extends HashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;

    private Object data;

    public Result() {
    }

    public static Result error() {
        return error(500, "未知异常，请联系管理员");
    }

    public static Result error(String msg) {
        return error(500, msg);
    }

    public static Result error(int code, String msg) {
        Result result = new Result();
        result.put("code", code);
        result.put("msg", msg);
        return result;
    }

    public static Result success(Map<String, Object> map) {
        Result result = new Result();
        result.putAll(map);
        return result;
    }

    public Integer getCode() {
        return (Integer) this.get(Constants.CODE);
    }

    public Object getData() {
        return this.get(Constants.DATA);
    }

    public static Result success(Object data) {
        Result result = new Result();
        result.put(Constants.CODE,200);
        result.put(Constants.MSG,"success");
        result.put("data",data);
        return result;
    }
    public static Result success() {
        Result result = new Result();
        result.put(Constants.CODE,200);
        result.put(Constants.MSG,"success");
        return result;
    }

    @Override
    public Result put(String key, Object value) {
        super.put(key, value);
        return this;
    }
}
