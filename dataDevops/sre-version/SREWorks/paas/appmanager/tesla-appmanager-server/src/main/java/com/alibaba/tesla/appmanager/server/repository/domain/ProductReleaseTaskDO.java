package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 产品版本任务实例表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductReleaseTaskDO {
    /**
     * ID
     */
    private Long id;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 最后修改时间
     */
    private Date gmtModified;

    /**
     * 产品 ID
     */
    private String productId;

    /**
     * 发布版本 ID
     */
    private String releaseId;

    /**
     * 任务 ID
     */
    private String taskId;

    /**
     * 调度类型 (可选 CRON / MANUAL)
     */
    private String schedulerType;

    /**
     * 调度配置 (仅 CRON 时内容为 CRON 表达式, MANUAL 时为空)
     */
    private String schedulerValue;

    private String status;

    private String errorMessage;
}