package com.alibaba.tesla.appmanager.dynamicscript.repository.domain;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 动态脚本表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DynamicScriptDO {
    private Long id;

    /**
     * 类型
     */
    private String kind;

    /**
     * 标识名称
     */
    private String name;

    /**
     * 当前版本
     */
    private Integer currentRevision;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 最后修改时间
     */
    private Date gmtModified;

    /**
     * 代码
     */
    private String code;
}