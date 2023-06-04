package com.datasophon.common.model;

import lombok.Data;

@Data
public class PromResponceInfo {
    private String status;
    /**
     * prometheus指标属性和值
     */
    private PromDataInfo data;

}
