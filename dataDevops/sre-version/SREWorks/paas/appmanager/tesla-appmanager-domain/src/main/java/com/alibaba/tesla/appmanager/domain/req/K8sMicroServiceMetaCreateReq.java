package com.alibaba.tesla.appmanager.domain.req;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.domain.dto.ContainerObjectDTO;
import com.alibaba.tesla.appmanager.domain.dto.EnvMetaDTO;
import lombok.Data;

import java.util.List;

/**
 * K8S 微服务元数据变更请求
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@Data
public class K8sMicroServiceMetaCreateReq {

    /**
     * 应用标示
     */
    private String appId;

    /**
     * 微服务标示
     */
    private String microServiceId;

    /**
     * 微服务名称
     */
    private String name;

    /**
     * 描述信息
     */
    private String description;

    /**
     * 容器对象
     */
    private List<ContainerObjectDTO> containerObjectList;

    /**
     * 环境变量定义
     */
    private List<EnvMetaDTO> envList;

    /**
     * 组件类型
     */
    private ComponentTypeEnum componentType;

    private JSONObject service;
}
