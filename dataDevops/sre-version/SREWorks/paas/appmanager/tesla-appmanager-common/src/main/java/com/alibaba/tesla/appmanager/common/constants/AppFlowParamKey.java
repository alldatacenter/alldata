package com.alibaba.tesla.appmanager.common.constants;

/**
 * App 部署单 Param Key
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class AppFlowParamKey {

    /**
     * 实时组件实例 ID
     */
    public static String COMPONENT_INSTANCE_ID = "componentInstanceId";

    /**
     * 组件类型
     */
    public static String COMPONENT_TYPE = "componentType";

    /**
     * Component 部署单 ID
     */
    public static String DEPLOY_COMPONENT_ID = "componentDeployId";

    /**
     * Addon Instance 申请任务 ID
     */
    public static String ADDON_INSTANCE_TASK_ID = "addonInstanceTaskId";

    /**
     * Addon Instance ID
     */
    public static String ADDON_INSTANCE_ID = "addonInstanceId";

    /**
     * 覆写 Parameter Values Map
     */
    public static String OVERWRITE_PARAMETER_VALUES = "overwriteParameterValues";

    /**
     * Component Schema
     */
    public static String COMPONENT_SCHEMA = "componentSchema";

    /**
     * Component Schema Map (Key: revisionMap)
     */
    public static String COMPONENT_SCHEMA_MAP = "componentSchemaMap";

    /**
     * Workload Spec
     */
    public static String WORKLOAD_SPEC = "workloadSpec";

    /**
     * Component Schema Map Key 生成器
     *
     * @param revisionName revision name
     * @return key
     */
    public static String componentSchemaMapKeyGenerator(String revisionName) {
        return COMPONENT_SCHEMA_MAP + "_" + revisionName;
    }
}
