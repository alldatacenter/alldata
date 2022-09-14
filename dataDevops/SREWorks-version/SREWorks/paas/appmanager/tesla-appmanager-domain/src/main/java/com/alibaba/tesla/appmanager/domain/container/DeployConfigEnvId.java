package com.alibaba.tesla.appmanager.domain.container;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 部署配置环境 ID
 */
public class DeployConfigEnvId {

    private static final String KEY_CLUSTER = "Cluster";
    private static final String KEY_NAMESPACE = "Namespace";
    private static final String KEY_STAGE = "Stage";

    /**
     * 集群 ID
     */
    private String clusterId;

    /**
     * Namespace Id
     */
    private String namespaceId;

    /**
     * Stage ID
     */
    private String stageId;

    public DeployConfigEnvId(String clusterId, String namespaceId, String stageId) {
        this.clusterId = clusterId;
        this.namespaceId = namespaceId;
        this.stageId = stageId;
    }

    /**
     * 返回 Unit 字符串标识
     *
     * @param unitId Unit ID
     * @return 字符串标识
     */
    public static String unitStr(String unitId) {
        return String.format("Unit:%s", unitId);
    }

    /**
     * 返回空环境字符串标识（通用类型）
     *
     * @return 字符串标识
     */
    public static String emptyUnitStr() {
        return "Unit";
    }

    /**
     * 返回 Stage 字符串标识
     *
     * @param stageId Stage ID
     * @return 字符串标识
     */
    public static String stageStr(String stageId) {
        return String.format("Stage:%s", stageId);
    }

    /**
     * 返回 Namespace 字符串标识
     *
     * @param namespaceId Namespace ID
     * @return 字符串标识
     */
    public static String namespaceStr(String namespaceId) {
        return String.format("Namespace:%s", namespaceId);
    }

    /**
     * 返回 Cluster 字符串标识
     *
     * @param clusterId Cluster ID
     * @return 字符串标识
     */
    public static String clusterStr(String clusterId) {
        return String.format("Cluster:%s", clusterId);
    }

    /**
     * 转换为数据库中存储的 envId
     *
     * @return string
     */
    @Override
    public String toString() {
        List<String> arr = new ArrayList<>();
        if (StringUtils.isNotEmpty(clusterId)) {
            arr.add(String.join(":", Arrays.asList(KEY_CLUSTER, clusterId)));
        }
        if (StringUtils.isNotEmpty(namespaceId)) {
            arr.add(String.join(":", Arrays.asList(KEY_NAMESPACE, namespaceId)));
        }
        if (StringUtils.isNotEmpty(stageId)) {
            arr.add(String.join(":", Arrays.asList(KEY_STAGE, stageId)));
        }
        if (arr.size() > 0) {
            return String.join("::", arr);
        } else {
            return "";
        }
    }
}
