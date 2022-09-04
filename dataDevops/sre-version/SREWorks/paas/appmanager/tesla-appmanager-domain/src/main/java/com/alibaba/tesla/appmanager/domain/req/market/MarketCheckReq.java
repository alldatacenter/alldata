package com.alibaba.tesla.appmanager.domain.req.market;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 市场的endpoint测试
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketCheckReq {

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
     * 远端Bucket名称
     */
    private String remoteBucket;

    /**
     * 远端仓库路径
     */
    private String remotePackagePath;
}
