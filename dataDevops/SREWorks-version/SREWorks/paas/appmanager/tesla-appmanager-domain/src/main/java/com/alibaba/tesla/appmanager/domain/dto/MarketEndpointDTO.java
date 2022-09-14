package com.alibaba.tesla.appmanager.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Market App Item
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketEndpointDTO implements Serializable {


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
     * 远端仓库路径
     */
    private String remotePackagePath;

    /**
     * 远端Bucket名称
     */
    private String remoteBucket;




}
