package com.alibaba.tesla.appmanager.common.constants;

/**
 * 用于在 Component Package 部署流程中的 SYSTEM_VAR variable 的 Map 中声明一些内部使用的 Key
 *
 * in OVERWRITE_PARAMETER_VALUES
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class AppFlowSystemVarKey {

    /**
     * 分批数量
     */
    public static String BATCH_NUMBER = "batchNumber";

    /**
     * 部署目标 StageId
     */
    public static String STAGE_ID = "stageId";

    /**
     * 部署目标机器 IP 列表，逗号分隔
     */
    public static String HOST_IPS = "hostIps";

    /**
     * Python Bin 绝对路径
     */
    public static String PYTHON_BIN = "pythonBin";
}
