package com.alibaba.tesla.appmanager.domain.res.deployconfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployConfigApplyTemplateRes<T> {

    /**
     * 应用成功列表
     */
    private List<T> items;
}
