package com.alibaba.tesla.appmanager.domain.req;

import com.alibaba.tesla.appmanager.common.BaseRequest;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * @author qianmo.zm@alibaba-inc.com
 * @date 2020/09/28.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class K8sMicroServiceMetaQueryReq extends BaseRequest {

    /**
     * 应用唯一标识
     */
    @Deprecated
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
     * 微应用类型
     */
    private List<ComponentTypeEnum> componentTypeList;

    /**
     * 微服务标示
     */
    private String microServiceId;

    /**
     * 架构
     */
    private String arch;
}
