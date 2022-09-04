package com.alibaba.tesla.appmanager.workflow.repository.domain;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务主机注册表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskHostRegistryDO {
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
     * 任务实例主机名
     */
    private String hostname;

    /**
     * 任务实例当前信息
     */
    private String instanceInfo;
}