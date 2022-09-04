package com.alibaba.sreworks.warehouse.common.exception;

/**
 * ES索引异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/05/08 16:41
 */
public class ESLifecyclePolicyException extends Exception {
    public ESLifecyclePolicyException(String message) {
        super(message);
    }

    public ESLifecyclePolicyException(Throwable cause) {
        super(cause);
    }
}
