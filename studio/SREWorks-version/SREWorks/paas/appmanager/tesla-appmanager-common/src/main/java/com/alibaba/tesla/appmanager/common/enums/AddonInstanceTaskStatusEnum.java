package com.alibaba.tesla.appmanager.common.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 附加组件实例任务状态枚举
 *
 * @author qiuqiang.qq@alibaba-inc.com
 */
public enum AddonInstanceTaskStatusEnum {

    /**
     * 等待
     */
    PENDING,

    /**
     * 运行
     */
    RUNNING,

    /**
     * 成功
     */
    SUCCESS,

    /**
     * 失败
     */
    FAILURE,

    /**
     * 异常
     */
    EXCEPTION,

    /**
     * 等待人工处理
     */
    WAIT_FOR_OP;

    /**
     * 返回处于运行态的状态字符串列表
     *
     * @return list of string
     */
    public static List<String> runningStatusList() {
        return Arrays.asList(PENDING.toString(), RUNNING.toString());
    }

    /**
     * 返回当前状态是否为结束态
     *
     * @return true or false
     */
    public boolean isEnd() {
        switch (this) {
            case SUCCESS:
            case FAILURE:
            case EXCEPTION:
            case WAIT_FOR_OP:
                return true;
            default:
                return false;
        }
    }
}
