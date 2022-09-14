package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 部署工单 - ComponentPackage 表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeployComponentDO implements Serializable {
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
     * 所属部署单 ID
     */
    private Long deployId;

    /**
     * 部署类型(COMPONENT/TRAIT)
     */
    private String deployType;

    /**
     * 当前部署项对应的组件(Trait)标识ID
     */
    private String identifier;

    /**
     * 应用唯一标识
     */
    private String appId;

    /**
     * 集群 ID
     */
    private String clusterId;

    /**
     * 部署目标 Namespace ID
     */
    private String namespaceId;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * 部署开始时间
     */
    private Date gmtStart;

    /**
     * 部署结束时间
     */
    private Date gmtEnd;

    /**
     * 状态
     */
    private String deployStatus;

    /**
     * 错误信息
     */
    private String deployErrorMessage;

    /**
     * 部署工单发起人
     */
    private String deployCreator;

    /**
     * 部署流程 ID
     */
    private String deployProcessId;

    /**
     * VERSION
     */
    private Integer version;

    private static final long serialVersionUID = 1L;

    /**
     * 返回申请过程消耗的时间
     *
     * @return 字符串
     */
    public String costTime() {
        if (gmtCreate == null || gmtModified == null) {
            return "unknown";
        }
        return String.format("%d", gmtModified.getTime() - gmtCreate.getTime());
    }
}