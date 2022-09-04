package com.alibaba.tesla.authproxy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统配置项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigDO implements Serializable {
    private Long id;
    private Date gmtCreate;
    private Date gmtModified;
    private String name;
    private String value;
}