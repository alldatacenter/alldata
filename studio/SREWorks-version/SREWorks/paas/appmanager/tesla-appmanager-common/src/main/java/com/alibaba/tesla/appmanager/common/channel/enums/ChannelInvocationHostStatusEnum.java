package com.alibaba.tesla.appmanager.common.channel.enums;

/**
 * Channel Invocation Host 状态枚举
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum ChannelInvocationHostStatusEnum {

    /**
     * 系统正在校验或发送命令
     */
    PENDING(0),

    /**
     * 命令正在运行
     */
    RUNNING(1),

    /**
     * 命令执行完成，且退出码为 0
     */
    SUCCESS(2),

    /**
     * 命令执行完成，且退出码非0
     */
    FAILED(3),

    /**
     * 命令执行超时，并且停止成功
     */
    TIMEOUT(4),

    /**
     * 正在停止任务
     */
    STOPPING(5),

    /**
     * 等待停止命令
     */
    WAIT_STOP(6),

    /**
     * 命令运行时被终止
     */
    TERMINATED(7),

    /**
     * 已经取消，命令未曾启动
     */
    CANCELLED(8),

    /**
     * 命令执行或者停止时发生异常无法继续
     */
    ERROR(9),

    TOUCH(10);

    private Integer value;

    private ChannelInvocationHostStatusEnum(int value) {
        this.value = value;
    }

    public static ChannelInvocationHostStatusEnum valueOf(int value) {
        switch (value) {
            case 0:
                return PENDING;
            case 1:
                return RUNNING;
            case 2:
                return SUCCESS;
            case 3:
                return FAILED;
            case 4:
                return TIMEOUT;
            case 5:
                return STOPPING;
            case 6:
                return WAIT_STOP;
            case 7:
                return TERMINATED;
            case 8:
                return CANCELLED;
            case 9:
                return ERROR;
            case 10:
                return TOUCH;
            default:
                throw new IllegalArgumentException("Invalid TaskHostStatusEnum value " + value);
        }
    }

    /**
     * 判断当前状态是否是终态
     *
     * @return true or false
     */
    public boolean isFinishedStatus() {
        return !isRunningStatus();
    }

    /**
     * 判断当前状态是否是运行态
     *
     * @return true or false
     */
    public boolean isRunningStatus() {
        return TOUCH.equals(this) || PENDING.equals(this) || RUNNING.equals(this);
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
