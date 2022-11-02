package com.alibaba.tesla.appmanager.domain.req.converter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 向 launch.yaml 中增加 parameterValues 参数
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AddParametersToLaunchReq implements Serializable {

    private static final long serialVersionUID = 7197364725123831203L;

    /**
     * launch YAML
     */
    private String launchYaml;

    /**
     * 需要附加的全局参数列表
     */
    private List<ParameterValue> parameterValues = new ArrayList<>();

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 命名空间 ID
     */
    private String namespaceId;

    /**
     * 集群 ID
     */
    private String clusterId;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * App Instance Name
     */
    private String appInstanceName;

    /**
     * 单个参数值
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParameterValue implements Serializable {

        private static final long serialVersionUID = 1146547791815295656L;

        /**
         * 变量值 Key
         */
        private String name;

        /**
         * 变量值 Value
         */
        private Object value;
    }
}
