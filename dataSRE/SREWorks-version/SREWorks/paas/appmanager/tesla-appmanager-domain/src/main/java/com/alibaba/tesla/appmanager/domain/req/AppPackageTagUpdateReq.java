package com.alibaba.tesla.appmanager.domain.req;

import lombok.Data;

import java.util.List;

/**
 * 应用包打包请求
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@Data
public class AppPackageTagUpdateReq {

    /**
     * 应用标识
     */
    private String appId;

    /**
     * Tag 列表
     */
    private List<String> tagList;
}
