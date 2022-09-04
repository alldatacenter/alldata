package com.alibaba.tesla.appmanager.domain.req.apppackage;

import com.alibaba.tesla.appmanager.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 应用包查询请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AppPackageTaskQueryReq extends BaseRequest {

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 应用打包的任务ID
     */
    private Long appPackageTaskId;
}
