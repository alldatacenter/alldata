package com.alibaba.tesla.appmanager.domain.req.productrelease;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListProductReleaseTaskAppPackageTaskReq {

    /**
     * 任务 ID
     */
    private String taskId;
}
