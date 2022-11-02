package com.alibaba.tesla.appmanager.domain.req;

import com.alibaba.tesla.appmanager.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 应用元数据查询请求
 *
 * @author qianmo.zm@alibaba-inc.com
 * @date 2020/09/28.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AppMetaQueryReq extends BaseRequest {

    private String appId;

    private String appIdLike;

    private String optionKey;

    private String optionValue;
}
