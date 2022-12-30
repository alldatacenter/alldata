package com.alibaba.tesla.appmanager.common.constants;

import java.util.HashMap;
import java.util.Map;

/**
 * 默认常量类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class DefaultConstant {

    /**
     * APIVersion
     */
    public static final String API_VERSION_V1_ALPHA2 = "core.oam.dev/v1alpha2";

    /**
     * 默认每页大小
     */
    public static final Integer DEFAULT_PAGE_SIZE = 20;

    /**
     * 无限每页大小
     */
    public static final Integer UNLIMITED_PAGE_SIZE = 1000000;

    /**
     * 默认当前页
     */
    public static final Integer DEFAULT_PAGE_NUMBER = 1;

    /**
     * 最大单次获取量
     */
    public static final Integer MAX_PAGE_SIZE = 1000;

    /**
     * 默认文件过期时间
     */
    public static final int DEFAULT_FILE_EXPIRATION = 36000;

    /**
     * 默认架构
     */
    public static final String DEFAULT_ARCH = "x86";

    /**
     * 初始化版本
     */
    public static final String INIT_VERSION = "1.0.0";

    /**
     * 自动生成版本号的标识
     */
    public static final String AUTO_VERSION = "_";

    /**
     * 系统管理员
     */
    public static final String SYSTEM_OPERATOR = "SYSTEM";

    /**
     * DAG 中 global variables 中的 DAG 类型标识，用于在 Dag Inst Listener 中使用，判定当前的事件属于什么类型
     */
    public static final String DAG_TYPE = "dagType";

    /**
     * 空 Object
     */
    public static final Map<String, String> EMPTY_OBJ = new HashMap<>();

    /**
     * AddonAttr - App ID
     */
    public static final String ADDON_ATTR_APP_ID = "appId";

    /**
     * AddonAttr - Stage ID
     */
    public static final String ADDON_ATTR_STAGE_ID = "stageId";

    /**
     * JsonPath 的固定前缀头
     */
    public static final String JSONPATH_PREFIX = "$.";

    /**
     * 上架
     */
    public static final String ON_SALE = "on-sale";

    /**
     * Component 镜像标识前缀
     */
    public static final String MIRROR_COMPONENT_PREFIX = "__MIRROR__";

    /**
     * 默认 Groovy Handler 名称
     */
    public static final String DEFAULT_GROOVY_HANDLER = "default";

    public static final String DEFAULT_REPO_BRANCH = "master";

    /**
     * 默认 K8S Microservice 类型
     */
    public static final String DEFAULT_K8S_MICROSERVICE_KIND = "AdvancedStatefulSet";

    public static final String INTERNAL_ADDON_DEVELOPMENT_META = "developmentmeta";

    public static final String INTERNAL_ADDON_APP_META = "appmeta";

    public static final String UNIT = "Unit";

    public static final String PRIVATE_ABM_CATEGORY = "专有云";
}
