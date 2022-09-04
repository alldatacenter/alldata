package com.alibaba.tesla.appmanager.server.service.appoption;

import com.alibaba.tesla.appmanager.common.enums.AppOptionTypeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * AppOption 常量类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class AppOptionConstant {

    /**
     * 应用名称 KEY
     */
    public static final String APP_NAME_KEY = "name";

    /**
     * 应用描述 KEY
     */
    public static final String APP_DESCRIPTION_KEY = "description";

    /**
     * 应用分类
     */
    public static final String APP_CATEGORY = "category";

    /**
     * Logo IMG
     */
    public static final String APP_LOGO_IMG = "logoImg";

    /**
     * App Source
     */
    public static final String APP_SOURCE = "appSource";

    /**
     * 可读名称
     */
    public static final String APP_NAME_CN = "nameCn";

    /**
     * 导航链接
     */
    public static final String APP_NAV_LINK = "navLink";

    /**
     * 应用配置 Key 类型映射字典
     */
    public static final Map<String, AppOptionTypeEnum> VALUE_TYPE_MAP =
            new HashMap<String, AppOptionTypeEnum>() {{
                // 应用名称
                put(APP_NAME_KEY, AppOptionTypeEnum.STRING);
                // 应用描述
                put(APP_DESCRIPTION_KEY, AppOptionTypeEnum.STRING);
                // 应用类型
                put("type", AppOptionTypeEnum.INTEGER);
                // 当前状态
                put("status", AppOptionTypeEnum.INTEGER);
                // 应用是否可用
                put("validflag", AppOptionTypeEnum.STRING);
                // 应用版本，v1/v2
                put("version", AppOptionTypeEnum.STRING);
                // 应用类型，如 PaaS
                put("appType", AppOptionTypeEnum.STRING);
                // 所属分类
                put(APP_CATEGORY, AppOptionTypeEnum.STRING);
                // 可读名称
                put(APP_NAME_CN, AppOptionTypeEnum.STRING);
                // 产品链接
                put("productLink", AppOptionTypeEnum.STRING);
                // 导航跳转地址
                put(APP_NAV_LINK, AppOptionTypeEnum.STRING);
                // 应用 Secret 配置
                put("secret", AppOptionTypeEnum.STRING);
                // readme
                put("readme", AppOptionTypeEnum.STRING);
                // Logo 图像地址
                put(APP_LOGO_IMG, AppOptionTypeEnum.STRING);
                // Aone ID
                put("aoneId", AppOptionTypeEnum.INTEGER);
                // Aone Name
                put("aoneName", AppOptionTypeEnum.STRING);
                // 兼容：流程平台配置
                put("flowIntid", AppOptionTypeEnum.STRING);
                put("flowIntInfo", AppOptionTypeEnum.STRING);
                // BU Code
                put("buCode", AppOptionTypeEnum.STRING);
                // SOURCE
                put("source", AppOptionTypeEnum.STRING);
                // StarAgent key
                put("staragentKey", AppOptionTypeEnum.STRING);
                // StarAgent secret
                put("staragentSecret", AppOptionTypeEnum.STRING);
                // StarAgent key
                put("saKey", AppOptionTypeEnum.STRING);
                // StarAgent secret
                put("saSecret", AppOptionTypeEnum.STRING);
                // 是否包含通知
                put("isHasNotify", AppOptionTypeEnum.INTEGER);
                // 是否包含搜索
                put("isHasSearch", AppOptionTypeEnum.BOOLEAN);
                // 所属组织
                put("organization", AppOptionTypeEnum.STRING);
                // 文档地址
                put("docsUrl", AppOptionTypeEnum.STRING);
                // 布局配置
                put("layout", AppOptionTypeEnum.JSON);
                // 模板标签
                put("templateLabel", AppOptionTypeEnum.STRING);
                // 初始访问地址
                put("initAccessUrl", AppOptionTypeEnum.STRING);
                // 是否需要日常环境
                put("isNeedDailyEnv", AppOptionTypeEnum.INTEGER);
                // 是否在导航中显示
                put("isShowInNav", AppOptionTypeEnum.INTEGER);
                // 是否包含钉钉二维码
                put("isHasDingdingQrcode", AppOptionTypeEnum.INTEGER);
                // 是否包含导航条
                put("isHasNav", AppOptionTypeEnum.INTEGER);
                // 钉钉二维码配置
                put("dingdingQrcodeConfig", AppOptionTypeEnum.STRING);
                // swapp
                put("swapp", AppOptionTypeEnum.STRING);
                // 应用来源
                put("appSource", AppOptionTypeEnum.STRING);
                // 前端全局参数数据源
                put("initDataSource", AppOptionTypeEnum.STRING);
                // 应用开发态
                put("isDevelopment", AppOptionTypeEnum.INTEGER);
                // 是否内置
                put("builtIn", AppOptionTypeEnum.INTEGER);
            }};

    /**
     * 默认值自定义字典 (特殊)
     */
    public static final Map<String, String> DEFAULT_VALUE_MAP =
            new HashMap<String, String>() {{
                put("validflag", "1");
                put("version", "v1");
                put("buCode", "JSPT");
                put("dingdingQrcodeConfig", "[]");
                put("isNeedDailyEnv", "1");
                put("templateLabel", "blank");
            }};
}
