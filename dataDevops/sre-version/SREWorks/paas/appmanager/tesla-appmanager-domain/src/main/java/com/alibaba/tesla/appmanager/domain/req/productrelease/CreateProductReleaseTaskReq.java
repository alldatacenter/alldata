package com.alibaba.tesla.appmanager.domain.req.productrelease;

import com.alibaba.tesla.appmanager.common.enums.ProductReleaseTaskStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductReleaseTaskReq {

    /**
     * Log 记录
     */
    private StringBuilder logContent;

    /**
     * 产品 ID
     */
    private String productId;

    /**
     * 版本发布 ID
     */
    private String releaseId;

    /**
     * 调度类型
     */
    private String schedulerType;

    /**
     * 调度值
     */
    private String schedulerValue;

    /**
     * 状态
     */
    private ProductReleaseTaskStatusEnum status;
}
