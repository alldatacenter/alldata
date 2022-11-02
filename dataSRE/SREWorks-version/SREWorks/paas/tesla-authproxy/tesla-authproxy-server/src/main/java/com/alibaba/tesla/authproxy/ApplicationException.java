package com.alibaba.tesla.authproxy;

/**
 * <p>Description: 自定义异常信息 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
public class ApplicationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误编码,可以根据错误编码，在message*.properties中获取错误信息
     */
    private int errCode;

    /**
     * 错误信息{}中的参数
     */
    private Object[] params;

    /**
     * 一般情况，只需传错误编码的构造方法
     *
     * @param errorCode
     */
    public ApplicationException(int errorCode) {
        this(errorCode, null, null, null);
    }

    /**
     * 需要自定义message的构造方法 使用此方法时，errCode一般为空
     * <p>
     * 如果errCode和message都有值， 用户看到的错误信息是errCode的信息
     *
     * @param errCode
     * @param message
     */

    public ApplicationException(int errCode, String message) {
        this(errCode, message, null, null);
    }

    /**
     * 当错误编码对应的错误信息需要参数时，使用此方法传入参数
     *
     * @param errCode
     * @param params
     */
    public ApplicationException(int errCode, Object[] params) {
        this(errCode, null, params, null);
    }

    /**
     * 需要自定义message的构造方法,且message中有用{}表示的参数 使用此方法时，errCode一般为空
     *
     * @param errCode
     * @param message
     * @param params
     */
    public ApplicationException(int errCode, String message, Object[] params) {
        this(errCode, message, params, null);
    }

    /**
     * 带全部参数的方法， 此方法errCode，message两者只需一个要值，params根据是否有参数 决定，有几个参数，则需要传入几个对象
     *
     * @param errCode
     * @param message
     * @param params
     * @param e
     */
    public ApplicationException(int errCode, String message, Object[] params, Throwable e) {
        super(message, e);
        this.params = params;
        this.errCode = errCode;
    }

    public int getErrCode() {
        return errCode;
    }

    public void setErrCode(int errCode) {
        this.errCode = errCode;
    }

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }
}
