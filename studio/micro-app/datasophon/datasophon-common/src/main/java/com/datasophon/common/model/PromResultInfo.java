package com.datasophon.common.model;

import lombok.Data;

/**
 * @Title: prometheus指标值
 */
@Data
public class PromResultInfo {
 
 
    /**
     * prometheus指标属性
     */
    private PromMetricInfo metric;
 
    /**
     * prometheus指标值
     */
    private String[] value;

}