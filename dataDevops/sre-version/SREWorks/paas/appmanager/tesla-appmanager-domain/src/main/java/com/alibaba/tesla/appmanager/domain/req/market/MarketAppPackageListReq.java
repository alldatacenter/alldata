package com.alibaba.tesla.appmanager.domain.req.market;

import com.alibaba.tesla.appmanager.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 市场应用列表 Request
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MarketAppPackageListReq extends BaseRequest {

    /**
     * 远端市场地址
     */
    private String remoteUrls;



}
