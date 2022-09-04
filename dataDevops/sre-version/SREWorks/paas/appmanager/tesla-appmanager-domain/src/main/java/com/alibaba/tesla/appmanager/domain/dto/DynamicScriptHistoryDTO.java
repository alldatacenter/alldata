package com.alibaba.tesla.appmanager.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 动态脚本_历史
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DynamicScriptHistoryDTO {

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
     * 代码版本
     */
    private Integer revision;

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