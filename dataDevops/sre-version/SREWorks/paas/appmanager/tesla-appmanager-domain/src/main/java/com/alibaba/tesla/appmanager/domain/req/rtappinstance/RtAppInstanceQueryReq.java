package com.alibaba.tesla.appmanager.domain.req.rtappinstance;

import com.alibaba.tesla.appmanager.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 实时应用实例 Query 请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RtAppInstanceQueryReq extends BaseRequest {

    /**
     * 应用实例 ID
     */
    private String appInstanceId;

    /**
     * 应用 ID (可逗号分隔)
     */
    private String appId;

    /**
     * Cluster ID
     */
    private String clusterId;

    /**
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Stage ID (可逗号分隔)
     */
    private String stageId;

    /**
     * 版本
     */
    private String version;

    /**
     * 状态
     */
    private String status;

    /**
     * 是否可访问
     */
    private Boolean visit;

    /**
     * 是否可升级
     */
    private Boolean upgrade;

    /**
     * 应用选项过滤 Key
     */
    private String optionKey;

    /**
     * 应用选项过滤 Value
     */
    private String optionValue;

    /**
     * 是否反转查询
     */
    private Boolean reverse = true;
}
