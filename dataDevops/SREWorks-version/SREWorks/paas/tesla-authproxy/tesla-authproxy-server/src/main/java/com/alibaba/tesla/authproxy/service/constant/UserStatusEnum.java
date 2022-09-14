package com.alibaba.tesla.authproxy.service.constant;

/**
 * 用户状态
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum UserStatusEnum {

    /**
     * 用户已被删除
     */
    DELETED,

    /**
     * 正常且激活状态
     */
    ALIVE,

    /**
     * 创建未完成状态 - 尚未创建密码
     */
    NON_COMPLETE_PASSWORD,

    /**
     * 创建未完成状态 - 尚未创建 Access Key
     */
    NON_COMPLETE_ACCESSKEY,

    /**
     * 创建未完成状态 - 尚未回调方法
     */
    NON_COMPLETE_CALLBACK;

    /**
     * 根据状态值生成对应的 Enum 常量
     *
     * @param status 状态值
     * @return 目标类型常量
     */
    public static UserStatusEnum build(Integer status) {
        switch (status) {
            case -1:
                return DELETED;
            case 0:
                return ALIVE;
            case 1:
                return NON_COMPLETE_PASSWORD;
            case 2:
                return NON_COMPLETE_ACCESSKEY;
            case 3:
                return NON_COMPLETE_CALLBACK;
            default:
                throw new IllegalArgumentException("Invalid user status " + status);
        }
    }

    /**
     * 转换为数字格式，用于数据库存储
     */
    public Integer toInt() {
        switch (this) {
            case DELETED:
                return -1;
            case ALIVE:
                return 0;
            case NON_COMPLETE_PASSWORD:
                return 1;
            case NON_COMPLETE_ACCESSKEY:
                return 2;
            case NON_COMPLETE_CALLBACK:
                return 3;
            default:
                throw new IllegalArgumentException("Invalid user status " + this.toString());
        }
    }

}
