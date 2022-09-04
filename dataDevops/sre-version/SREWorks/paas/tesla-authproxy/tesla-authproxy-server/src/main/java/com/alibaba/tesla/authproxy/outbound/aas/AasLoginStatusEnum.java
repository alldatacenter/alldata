package com.alibaba.tesla.authproxy.outbound.aas;

/**
 * AAS 登录结果类型
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum AasLoginStatusEnum {

    // 成功
    SUCCESS,

    // 用户不存在
    USER_NOT_EXIST,

    // 密码错误
    PASSWORD_ERROR,

    // 用户被临时锁定
    USER_TEMP_LOCKED,

    // 用户被长期锁定
    USER_LOCKED,

    // AAS 系统故障
    AAS_FAULT,

    // 未知错误
    UNKNOWN;

    /**
     * 将 AasLoginStatusEnum 由 AAS 的 login tip 转为自己的 Enum 常量
     *
     * @param responseStr AAS 的登录提示字符串，如果成功，则应该传入 "SUCCESS"
     * @return Enum 常量
     */
    public static AasLoginStatusEnum build(String responseStr) {
        if (responseStr.equals("SUCCESS")) {
            return SUCCESS;
        } else if (responseStr.contains("用户名不正确")) {
            return USER_NOT_EXIST;
        } else if (responseStr.contains("密码输入错误，已累计")) {
            return PASSWORD_ERROR;
        } else if (responseStr.contains("密码输入错误次数过多")) {
            return USER_TEMP_LOCKED;
        } else {
            return UNKNOWN;
        }
    }

}