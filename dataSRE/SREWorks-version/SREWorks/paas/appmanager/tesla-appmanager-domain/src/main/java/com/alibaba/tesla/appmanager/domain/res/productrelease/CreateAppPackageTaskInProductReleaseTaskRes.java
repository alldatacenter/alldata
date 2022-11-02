package com.alibaba.tesla.appmanager.domain.res.productrelease;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建产品发布版本任务 (AppPackageTask) 结果 Res
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAppPackageTaskInProductReleaseTaskRes {

    /**
     * 应用包 ID
     */
    private Long appPackageTaskId;
}
