package com.alibaba.tesla.gateway.common.enums;

/**
 * header auth失败类型
 *
 * @author cdx
 * @date 2019/11/18 14:10
 */
public enum AuthErrorTypeEnum {

    /**
     * 请求不包含完整的四个header
     */
    EMPTY_HEADER,
    /**
     * header认证失败
     */
    HEADER_ERROR
}
