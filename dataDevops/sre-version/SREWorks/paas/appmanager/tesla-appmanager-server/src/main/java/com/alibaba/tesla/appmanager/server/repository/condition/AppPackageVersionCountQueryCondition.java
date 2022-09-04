package com.alibaba.tesla.appmanager.server.repository.condition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用版本计数查询条件
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPackageVersionCountQueryCondition {

    /**
     * 应用 ID 列表
     */
    private List<String> appIds = new ArrayList<>();

    /**
     * 标签
     */
    private String tag;
}
