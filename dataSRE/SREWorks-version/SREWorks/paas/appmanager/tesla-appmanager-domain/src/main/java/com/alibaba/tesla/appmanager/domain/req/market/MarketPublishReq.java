package com.alibaba.tesla.appmanager.domain.req.market;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 应用发布至公共市场请求
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketPublishReq {

    /**
     * 远端公共市场地址
     */
    private String endpoint;

    /**
     * 远端公共市场写入accessKey
     */
    private String accessKey;

    /**
     * 远端公共市场写入secretKey
     */
    private String secretKey;

    /**
     * 远端公共市场类型
     */
    private String endpointType;

    /**
     * 本地应用包
     */
    private Long appPackageId;

    /**
     * 远端应用版本，不修改则为package中的当前版本
     */
    private String remoteSimplePackageVersion;

    /**
     * 远端应用ID，不修改则为package中的当前APP_ID
     */
    private String remoteAppId;

    /**
     * 远端仓库路径
     */
    private String remotePackagePath;

    /**
     * 远端Bucket名称
     */
    private String remoteBucket;

}
