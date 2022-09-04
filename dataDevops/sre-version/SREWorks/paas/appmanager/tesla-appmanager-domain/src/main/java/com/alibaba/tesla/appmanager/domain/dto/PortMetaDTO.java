package com.alibaba.tesla.appmanager.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author qianmo.zm@alibaba-inc.com
 * @date 2020/10/29.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortMetaDTO {

    /**
     * 变量名
     */
    private String name;

    /**
     * 变量值
     */
    private String value;

    /**
     * 注释
     */
    private String comment;
}
