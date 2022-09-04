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
public class EnvMetaDTO {
    /**
     * 变量名
     */
    private String name;

    /**
     * 描述
     */
    private String comment;

    /**
     * 默认值
     */
    private String defaultValue;
}
