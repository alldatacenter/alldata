package com.alibaba.tesla.appmanager.common.enums;

/**
 * 集群类型
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum ClusterTypeEnum {

    /**
     * 普通 K8S 集群
     */
    KUBERNETES("kubernetes"),

    /**
     * 资源集群
     */
    RESOURCE_CLUSTER("resource-cluster"),

    /**
     * 虚拟集群
     */
    VC("vc");

    private final String text;

    ClusterTypeEnum(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

    /**
     * String to Enum
     *
     * @param text String
     * @return Enum
     */
    public static ClusterTypeEnum fromString(String text) {
        for (ClusterTypeEnum item : ClusterTypeEnum.values()) {
            if (item.text.equalsIgnoreCase(text)) {
                return item;
            }
        }
        return null;
    }
}
