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
public class MarketAppListReq extends BaseRequest {

    /**
     * 应用选项过滤 Key
     */
    private String optionKey;

    /**
     * 应用选项过滤 Value
     */
    private String optionValue;
}
