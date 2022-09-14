package com.alibaba.tesla.appmanager.common.constants;

import java.io.Serializable;

/**
 * @ClassName: CustomComponentConstant
 * @Author: dyj
 * @DATE: 2020-12-22
 * @Description:
 **/
public class CustomComponentConstant implements Serializable {
    private static final long serialVersionUID = 4457701826521660952L;

    public static final String API_VERSION = "core.oam.dev/v1alpha2";
    public static final String KIND = "Component";
    public static final String WORKLOAD_API_VERSION = "flyadmin.alibaba.com/v1alpha1";
    public static final String WORKLOAD_KIND = "CustomAddon";


    public static String APP_ID_PATH = "addon.appId";
    public static String APP_ID_TEMPLATE = "{{ addon.appId }}";
    public static String NAMESPACE_PATH = "addon.namespaceId";
    public static String NAMESPACE_TEMPLATE = "{{ addon.namespaceId }}";
}
