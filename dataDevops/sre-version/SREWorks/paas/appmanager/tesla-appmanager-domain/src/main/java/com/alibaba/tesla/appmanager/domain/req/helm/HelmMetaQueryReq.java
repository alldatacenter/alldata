package com.alibaba.tesla.appmanager.domain.req.helm;

import com.alibaba.tesla.appmanager.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * HELM组件查询请求
 *
 * @author fangzong.lyj@alibaba-inc.com
 * @date 2021/12/26 09:36
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HelmMetaQueryReq extends BaseRequest {
    /**
     * ID
     */
    private Long id;

    /**
     * 应用 ID
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
     * Helm 名称
     */
    private String name;
}
