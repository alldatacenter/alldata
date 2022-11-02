package com.alibaba.tesla.appmanager.domain.req.rtappinstance;

import com.alibaba.tesla.appmanager.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 实时组件实例历史 Query 请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RtComponentInstanceHistoryQueryReq extends BaseRequest {

    /**
     * 组件实例 ID
     */
    private String componentInstanceId;

    /**
     * 状态
     */
    private String status;
}
