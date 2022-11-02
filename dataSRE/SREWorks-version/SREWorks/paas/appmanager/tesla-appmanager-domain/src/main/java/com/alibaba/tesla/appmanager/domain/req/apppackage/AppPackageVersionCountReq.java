package com.alibaba.tesla.appmanager.domain.req.apppackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用包版本数查询请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPackageVersionCountReq {

    /**
     * 要查询的应用 ID 列表
     */
    private List<String> appIds = new ArrayList<>();

    /**
     * Tag
     */
    private String tag;
}
