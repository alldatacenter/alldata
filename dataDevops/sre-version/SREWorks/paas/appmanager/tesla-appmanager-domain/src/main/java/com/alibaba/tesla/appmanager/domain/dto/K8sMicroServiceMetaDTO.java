package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * @author qianmo.zm@alibaba-inc.com
 * @date 2020/09/28.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class K8sMicroServiceMetaDTO {
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
     * 应用标示
     */
    private String appId;

    /**
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * 微服务标示
     */
    private String microServiceId;

    /**
     * 微服务名称
     */
    private String name;

    /**
     * 微服务描述
     */
    private String description;

    /**
     * 组件类型
     */
    private ComponentTypeEnum componentType;

    /**
     * 架构
     */
    private String arch;

    /**
     * 类型 (Deployment/StatefulSet/CloneSet/AdvancedStatefulSet)
     */
    private String kind;

    /**
     * 环境变量定义
     */
    private List<EnvMetaDTO> envList;

    /**
     * 构建对象
     */
    private List<ContainerObjectDTO> containerObjectList;

    /**
     * 初始化容器对象
     */
    private List<InitContainerDTO> initContainerList;

    /**
     * 环境变量Key定义
     */
    private List<String> envKeyList;

    /**
     * 仓库配置
     */
    private RepoDTO repoObject;

    /**
     * 镜像推送配置
     */
    private ImagePushDTO imagePushObject;

    /**
     * 部署对象
     */
    private LaunchDTO launchObject;

    /**
     * 归属产品 ID
     */
    private String productId;

    /**
     * 归属发布版本 ID
     */
    private String releaseId;
}
