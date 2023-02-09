package com.qcloud.cos.model.ciModel.auditing;

/**
 * 回调内容的结构，有效值：Simple（回调内容包含基本信息）、Detail（回调内容包含详细信息）。默认为 Simple。
 */
public enum CallbackVersion {

    Simple, Detail

}
