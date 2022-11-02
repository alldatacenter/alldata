package com.alibaba.tesla.appmanager.domain.req.apppackage;

import com.alibaba.tesla.appmanager.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 应用包查询请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AppPackageQueryReq extends BaseRequest {

    /**
     * 应用包 ID
     */
    private Long id;

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 应用包版本
     */
    private String packageVersion;

    /**
     * 应用包版本大于某个版本
     */
    private String packageVersionGreaterThan;

    /**
     * 应用包版本小于某个版本
     */
    private String packageVersionLessThan;

    /**
     * 应用包创建者
     */
    private String packageCreator;

    /**
     * tag列表
     */
    private List<String> tagList;
}
