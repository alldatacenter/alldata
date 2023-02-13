
package com.platform.utils;

/**
 * 支付状态
 * @author AllDataDC
 * @date 2023-01-27
 */
public enum  AliPayStatusEnum {

    /** 交易成功 */
    FINISHED("TRADE_FINISHED"),

    /** 支付成功 */
    SUCCESS("TRADE_SUCCESS"),

    /** 交易创建 */
    BUYER_PAY("WAIT_BUYER_PAY"),

    /** 交易关闭 */
    CLOSED("TRADE_CLOSED");

    private final String value;

    AliPayStatusEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
