package com.alibaba.tesla.appmanager.common.channel.enums;

/**
 * Channel Invocation 状态枚举
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum ChannelInvocationStatusEnum {

    /**
     * 系统正在发送命令中。存在至少一台主机的脚本执行状态为 Pending，则整体状态为 Pending
     */
    PENDING(0),

    /**
     * 命令执行中。存在至少一台主机的脚本执行状态为 Running，则整体状态为 Running
     */
    RUNNING(1),

    /**
     * 各个主机上的命令执行状态均为 Stopped (Canceled/Terminated) 或 Success，且至少一个实例的脚本执行状态是 Success，则整体状态为 Success
     */
    SUCCESS(2),

    /**
     * 各个主机上的命令执行状态均为 Stopped (Canceled/Terminated) 或 Failed (包含 Failed/Timeout/Error 状态)，则整体状态为 Failed
     */
    FAILED(3),

    /**
     * 正在停止任务。存在至少一台主机上的命令执行状态为 Stopping，则整体状态为 Stopping
     */
    STOPPING(4),

    /**
     * 任务已停止。所有主机的脚本执行状态是 Stopped (Canceled/Terminated)，则整体状态为 Stopped
     */
    STOPPED(5),

    /**
     * 存在至少一台主机执行状态为 Success 且至少一台主机执行状态为 Failed (包含 Failed/Timeout/Error 状态)，则整体状态为 PartialFailed
     */
    PARTIAL_FAILED(6),

    /**
     * task落盘完成，还没有一个host分发。
     */
    TOUCH(7);

    private Integer value;

    private ChannelInvocationStatusEnum(int value) {
        this.value = value;
    }

    public static ChannelInvocationStatusEnum valueOf(int value) {
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
                return STOPPING;
            case 5:
                return STOPPED;
            case 6:
                return PARTIAL_FAILED;
            case 7:
                return TOUCH;
            default:
                throw new IllegalArgumentException("Invalid TaskStatusEnum value " + value);
        }
    }

    public static boolean hasExist(String status) {
        boolean flag = false;
        for (ChannelInvocationStatusEnum item : ChannelInvocationStatusEnum.values()) {
            if (item.name().equals(status)) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    /**
     * 判断当前状态是否是终态
     *
     * @return true or false
     */
    public boolean isFinishedStatus() {
        return SUCCESS.equals(this) || FAILED.equals(this) || STOPPED.equals(this) || PARTIAL_FAILED.equals(this);
    }

    /**
     * 判断当前状态是否是运行态
     *
     * @return true or false
     */
    public boolean isRunningStatus() {
        return !isFinishedStatus();
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
