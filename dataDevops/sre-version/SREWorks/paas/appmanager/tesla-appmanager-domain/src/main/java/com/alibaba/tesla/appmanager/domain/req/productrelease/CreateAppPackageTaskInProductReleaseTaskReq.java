package com.alibaba.tesla.appmanager.domain.req.productrelease;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAppPackageTaskInProductReleaseTaskReq {

    /**
     * Log 记录
     */
    private StringBuilder logContent;

    /**
     * 产品 ID
     */
    private String productId;

    /**
     * 版本发布 ID
     */
    private String releaseId;

    /**
     * 任务 ID
     */
    private String taskId;

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 构建 Tag
     */
    private String tag;

    /**
     * Git Clone 的本地目录
     */
    private Path dir;

    /**
     * 分支地址
     */
    private String branch;

    /**
     * build yaml 对应的分支内容
     */
    private String buildPath;

    /**
     * 调度类型
     */
    private String schedulerType;

    /**
     * 调度值
     */
    private String schedulerValue;
}
