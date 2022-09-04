package com.alibaba.tesla.appmanager.domain.res.productrelease;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建产品发布版本任务结果 Res
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductReleaseTaskRes {

    /**
     * 任务 ID
     */
    private String taskId;
}
