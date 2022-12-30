package com.alibaba.tesla.appmanager.domain.req.componentpackage;

import com.alibaba.tesla.appmanager.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 组件包查询请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentPackageQueryReq extends BaseRequest {

    /**
     * 组件包 ID
     */
    private Long id;

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 组件类型
     */
    private String componentType;

    /**
     * 组件名称
     */
    private String componentName;

    /**
     * 包版本
     */
    private String packageVersion;

    /**
     * 包创建者
     */
    private String packageCreator;

    /**
     * 排序规则
     */
    private String orderBy;
}
