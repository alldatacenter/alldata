package com.alibaba.tesla.appmanager.domain.req.componentpackage;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 组件包 - 任务创建请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentPackageTaskCreateReq implements Serializable {

    private static final long serialVersionUID = 1056721321469156076L;

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * Component 类型
     */
    private String componentType;

    /**
     * Component 名称
     */
    private String componentName;

    /**
     * 包版本
     */
    private String version;

    /**
     * 配置项 JSON
     */
    private JSONObject options;

    /**
     * 应用打包任务ID
     */
    private Long appPackageTaskId;
}
