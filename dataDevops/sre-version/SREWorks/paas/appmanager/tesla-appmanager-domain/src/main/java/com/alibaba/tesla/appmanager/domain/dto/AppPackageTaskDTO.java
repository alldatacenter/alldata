package com.alibaba.tesla.appmanager.domain.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Component Package 任务详细信息
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPackageTaskDTO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 修改时间
     */
    private Date gmtModified;

    /**
     * 应用唯一标识
     */
    private String appId;

    /**
     * 映射 app package 表主键 ID
     */
    private Long appPackageId;

    /**
     * 创建人
     */
    private String packageCreator;

    /**
     * 任务状态
     */
    private String taskStatus;

    /**
     * 版本号
     */
    private String packageVersion;

    /**
     * 包配置选项信息
     */
    private String packageOptions;

    /**
     * 简易版本号
     */
    private String simplePackageVersion;

    /**
     * 已上架
     */
    private Boolean isOnSale;

    /**
     * 环境 ID
     */
    private String envId;

    /**
     * 默认应用部署 YAML
     */
    private String swapp;

    /**
     * 标签列表
     */
    private List<String> tags = new ArrayList<>();
}
