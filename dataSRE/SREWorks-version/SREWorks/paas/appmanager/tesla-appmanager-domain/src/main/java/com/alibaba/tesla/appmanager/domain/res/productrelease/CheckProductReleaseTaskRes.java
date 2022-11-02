package com.alibaba.tesla.appmanager.domain.res.productrelease;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckProductReleaseTaskRes {

    /**
     * 是否存在正在运行中的调度任务
     */
    private boolean exists;

    /**
     * 如果存在冲突调度任务，冲突 Task UUID 清单
     */
    private List<String> conflict = new ArrayList<>();
}
