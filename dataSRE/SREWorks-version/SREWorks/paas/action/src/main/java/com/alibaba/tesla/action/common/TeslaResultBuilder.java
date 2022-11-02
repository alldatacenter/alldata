package com.alibaba.tesla.action.common;

import com.alibaba.tesla.action.constant.TeslaResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 返回值构造器
 *
 * @author tandong.td@alibaba-inc.com
 *
 * Created by tandong on 2017/6/24.
 */
public class TeslaResultBuilder implements Serializable  {

    static String MSG_PARAM_EMPTY = "parameter can't be empty";

    static String MSG_PARAM_ERROR = "parameter error:";

    static String MSG_ERROR = "服务异常";

    static String MSG_NO_PERMISSION = "无权限";

    static String MSG_SUCCESS = "SUCCESS";

    /**
     * 返回无权限结果
     * @param data
     * @return
     */
    public static TeslaResult noPermissionResult(Object data){
        String message = MSG_NO_PERMISSION;
        return new TeslaResult(TeslaResult.NO_PERMISSION, data, MSG_NO_PERMISSION);
    }

    /**
     * 构建参数错误返回值
     * @param detailMessage 返回的详细错误信息
     * @return
     */
    public static TeslaResult paramErrorResult(String detailMessage){
        String message = MSG_PARAM_ERROR;
        if(null != detailMessage && detailMessage.length() > 0){
            message = MSG_PARAM_ERROR + detailMessage;
        }
        return new TeslaResult(TeslaResult.BAD_REQUEST, null, message);
    }

    /**
     * 构建参数校验错误返回结果
     * 使用Spring的参数校验器
     * @param result
     * @return
     */
    public static TeslaResult buildValidationResult(BindingResult result) {
        Map<String, String> errorMap = new HashMap<>(10);
        List<FieldError> fieldErrors = result.getFieldErrors();
        for (FieldError error : fieldErrors) {
            errorMap.put(error.getField(), error.getDefaultMessage());
        }

        TeslaResult ret = new TeslaResult();
        ret.setCode(TeslaResult.BAD_REQUEST);
        ret.setData(errorMap);

        List<String> errorMessages = new ArrayList<>();
        for (Entry<String, String> entry : errorMap.entrySet()) {
            errorMessages.add(entry.getKey() + ": " + entry.getValue());
        }
        ret.setMessage("参数错误： " + String.join(", ", errorMessages));
        return ret;
    }

    /**
     * 构建参数为空的返回值
     * @return
     */
    public static TeslaResult paramEmptyResult(){
        return new TeslaResult(TeslaResult.BAD_REQUEST, null, MSG_PARAM_EMPTY);
    }

    /**
     * 构建错误返回值
     * @param data 返回数据
     * @param errorMsg 返回错误消息
     * @return
     */
    public static TeslaResult errorResult(Object data,String errorMsg){
        return new TeslaResult(TeslaResult.FAILURE, data, errorMsg);
    }

    public static TeslaResult errorResult(Exception e){
        String message = e.getMessage();
        if(null != e.getCause()){
            message = e.getCause().getMessage();
        }
        return new TeslaResult(TeslaResult.FAILURE, null, message);
    }

    /**
     * 构建失败返回值
     * @param data 返回数据
     * @return
     */
    public static TeslaResult failureResult(Object data){
        return new TeslaResult(TeslaResult.FAILURE, data, MSG_ERROR);
    }

    /**
     * 构建失败返回值
     * @param data 返回数据
     * @return
     */
    public static TeslaResult failureResult(int code, Object data){
        return new TeslaResult(code, data, MSG_ERROR);
    }

    /**
     * 自定义错误码和错误消息
     * @param code
     * @param data
     * @param message
     * @return
     */
    public static TeslaResult failureResult(int code, Object data, String message){
        return new TeslaResult(code, data, message);
    }

    /**
     * 构建失败返回值，默认
     * @return
     */
    public static TeslaResult failureResult(){
        return new TeslaResult(TeslaResult.FAILURE, null, MSG_ERROR);
    }

    /**
     * 构建成功返回值，默认
     * @return
     */
    public static TeslaResult successResult(){
        return new TeslaResult(TeslaResult.SUCCESS, null, MSG_SUCCESS);
    }

    /**
     * 构建成功返回值
     * @param data 返回数据
     * @return
     */
    public static TeslaResult successResult(Object data){
        return new TeslaResult(TeslaResult.SUCCESS, data, MSG_SUCCESS);
    }

    /**
     * 指定成功返回的code、data、message参数
     * @param code
     * @param data
     * @param message
     * @return
     */
    public static TeslaResult successResult(int code, Object data, String message){
        if(null == message || message.length() == 0){
            message = MSG_SUCCESS;
        }
        return new TeslaResult(code, data, message);
    }

    /**
     * 构建成功返回值
     * @param data 返回数据
     * @param message 自定义成功消息
     * @return
     */
    public static TeslaResult successResult(Object data, String message){
        if(null != message && message.length() > 0){
            return new TeslaResult(TeslaResult.SUCCESS, data, MSG_SUCCESS);
        }else{
            return new TeslaResult(TeslaResult.SUCCESS, data, message);
        }
    }
}
