package com.alibaba.sreworks.health.common.incident;

public enum SelfHealingStatus {
    WAITING,
    RUNNING, // 不进入自愈通道
    FAILURE,
    SUCCESS,  // 不进入自愈通道
    CANCEL  // 不进入自愈通道
}
