package com.datasophon.common.model;

import lombok.Data;

/**
 * @Title: prometheus指标属性（这里只反序列化了需要的属性，用户可根据需要做增减）
 */
@Data
public class PromMetricInfo {
 
    /**
     * prometheus指标名称
     */
    private String __name__;
 
    /**
     * prometheus实例名称
     */
    private String instance;
 
    /**
     * prometheus任务名称
     */
    private String job;
 
}