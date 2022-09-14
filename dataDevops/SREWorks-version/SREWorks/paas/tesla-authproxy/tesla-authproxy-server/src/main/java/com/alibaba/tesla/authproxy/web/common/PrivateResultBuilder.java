package com.alibaba.tesla.authproxy.web.common;

import com.alibaba.tesla.common.utils.TeslaResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 专有云场景返回数据构建器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivateResultBuilder {

    private static final HashMap<String, String> EMPTY_INFO = new HashMap<>();

    /**
     * 用于返回数据的最外层 code
     */
    private static final Integer CODE_NOT_FOUND = 404;
    private static final Integer CODE_FORBIDDEN = 403;

    /**
     * 用于返回数据的内部扩展层 status
     */
    private static final Integer STATUS_OK = 200;
    private static final Integer STATUS_BAD_REQUEST = 400;
    private static final Integer STATUS_VALIDATION_ERROR = 422;

    /**
     * 构建返回的成功数据 (使用 PrivateExtData)
     */
    public static TeslaResult buildExtSuccessResult() {
        TeslaResult ret = new TeslaResult();
        ret.setCode(TeslaResult.SUCCESS);
        ret.setMessage("");
        PrivateExtData extData = new PrivateExtData();
        extData.setStatus(STATUS_OK);
        extData.setInfo(EMPTY_INFO);
        ret.setData(extData);
        return ret;
    }

    /**
     * 构建返回的成功数据 (使用 PrivateExtData)
     *
     * @param data 携带数据
     */
    public static TeslaResult buildExtSuccessResult(Object data) {
        TeslaResult ret = new TeslaResult();
        ret.setCode(TeslaResult.SUCCESS);
        ret.setMessage("");
        PrivateExtData extData = new PrivateExtData();
        extData.setStatus(STATUS_OK);
        extData.setInfo(data);
        ret.setData(extData);
        return ret;
    }

    /**
     * 构建返回的成功数据 (使用 PrivateExtData)
     *
     * @param data 携带数据
     */
    public static TeslaResult buildSuccessResult(Object data) {
        TeslaResult ret = new TeslaResult();
        ret.setCode(TeslaResult.SUCCESS);
        ret.setMessage("");
        ret.setData(data);
        return ret;
    }

    /**
     * 构建参数错误返回数据 (使用 PrivateExtData)
     *
     * @param data 参数错误对应关系，key=错误字段名，value=错误原因
     */
    public static TeslaResult buildExtValidationErrorResult(Map<String, String> data) {
        TeslaResult ret = new TeslaResult();
        ret.setCode(TeslaResult.SUCCESS);
        ret.setMessage("");
        PrivateExtData extData = new PrivateExtData();
        extData.setStatus(STATUS_VALIDATION_ERROR);
        extData.setMessage("validation error");
        extData.setInfo(data);
        ret.setData(extData);
        return ret;
    }

    /**
     * 构建返回的失败数据 (使用 PrivateExtData)
     *
     * @param message 错误信息
     */
    public static TeslaResult buildExtBadRequestResult(String message) {
        TeslaResult ret = new TeslaResult();
        ret.setCode(TeslaResult.SUCCESS);
        ret.setMessage("");
        PrivateExtData extData = new PrivateExtData();
        extData.setStatus(STATUS_BAD_REQUEST);
        extData.setMessage(message);
        extData.setInfo(EMPTY_INFO);
        ret.setData(extData);
        return ret;
    }

    /**
     * 构建返回的失败数据 (使用 PrivateExtData)
     *
     * @param message 错误信息
     * @param data    携带数据
     */
    public static TeslaResult buildExtBadRequestResult(String message, Object data) {
        TeslaResult ret = new TeslaResult();
        ret.setCode(TeslaResult.SUCCESS);
        ret.setMessage("");
        PrivateExtData extData = new PrivateExtData();
        extData.setStatus(STATUS_BAD_REQUEST);
        extData.setMessage(message);
        extData.setInfo(data);
        ret.setData(extData);
        return ret;
    }

    /**
     * 构建返回的异常数据
     *
     * @param message 异常信息
     */
    public static TeslaResult buildExceptionResult(String message) {
        TeslaResult ret = new TeslaResult();
        ret.setCode(TeslaResult.FAILURE);
        ret.setMessage(message);
        ret.setData(EMPTY_INFO);
        return ret;
    }

    /**
     * 构建返回的权限不足数据
     *
     * @param message 异常信息
     */
    public static TeslaResult buildForbiddenResult(String message) {
        TeslaResult ret = new TeslaResult();
        ret.setCode(CODE_FORBIDDEN);
        ret.setMessage(message);
        ret.setData(EMPTY_INFO);
        return ret;
    }

    /**
     * 构建返回的权限不足数据
     *
     * @param message      异常信息
     * @param resourcePath 资源路径
     */
    public static TeslaResult buildForbiddenResult(String message, String resourcePath) {
        TeslaResult ret = new TeslaResult();
        ret.setCode(CODE_FORBIDDEN);
        ret.setMessage(message);
        Map<String, String> data = new HashMap<>();
        data.put("resource_path", resourcePath);
        ret.setData(data);
        return ret;
    }

    /**
     * 构建返回的权限不足数据
     *
     * @param message 异常信息
     */
    public static TeslaResult buildNotLoginResult(String message, String loginUrl) {
        TeslaResult ret = new TeslaResult();
        ret.setCode(TeslaResult.NOAUTH);
        ret.setMessage(message);
        Map<String, String> data = new HashMap<>();
        data.put("loginUrl", loginUrl);
        ret.setData(data);
        return ret;
    }

    /**
     * 构建返回的 404
     */
    public static TeslaResult buildNotFoundResult() {
        TeslaResult ret = new TeslaResult();
        ret.setCode(CODE_NOT_FOUND);
        ret.setMessage("not found");
        ret.setData(EMPTY_INFO);
        return ret;
    }

}

/**
 * 专有云场景下的返回值 data 字段的结构（为了兼容前端框架）
 */
class PrivateExtData implements Serializable {

    public static final long serialVersionUID = 1L;

    /**
     * 当没有 info 数据可以 set 时使用此对象
     */
    private static final Object EMPTY_DATA = new ArrayList<>();

    /**
     * 状态码
     */
    private Integer status;

    /**
     * 显示信息
     */
    private String message = "";

    /**
     * 返回数据信息
     */
    private Object info = EMPTY_DATA;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getInfo() {
        return info;
    }

    public void setInfo(Object info) {
        this.info = info;
    }

}