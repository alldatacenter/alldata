package com.alibaba.sreworks.dataset.common.constant;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 常量
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 17:00
 */
public class Constant {

    public final static Integer SREWORKS_TEAM_ID = 0;

    public final static Integer SREWORKS_APP_ID = 0;

    public static final long MAX_SECONDS_TIMESTAMP = 9999999999L;

    /**
     * 模型数据落盘最大数量
     */
    public final static Integer MAX_MODEL_DATA_FLUSH_SIZE = 1000;

    /**
     * 模型数据落盘每批数量
     */
    public final static Integer MODEL_DATA_FLUSH_STEP = 100;

    /**
     * 缓存十分钟
     */
    public final static Long CACHE_EXPIRE_SECONDS = 1800L;
    public final static Long CACHE_MAX_SIZE = 50L;

    /*** meta配置的相关常量 */
    public final static String META_PREFIX = "_";
    public final static String ID = "id";
    public final static String META_TYPE = META_PREFIX + "type";
    public final static String META_ID = META_PREFIX + ID;
    public final static String INDEX_FORMAT = "<${alias}-{now/M{yyyy.MM}}>";

    /**
     * 接口配置模式
     */
    public final static String INTERFACE_CONFIG_SCRIPT_MODE = "script";
    public final static String INTERFACE_CONFIG_GUIDE_MODE = "guide";
    public static List<String> INTERFACE_CONFIG_MODE = Arrays.asList(INTERFACE_CONFIG_SCRIPT_MODE, INTERFACE_CONFIG_GUIDE_MODE);

    public final static String DEFAULT_CONTENT_TYPE = "application/json";
    public final static List<String> REQUEST_METHODS = Arrays.asList("GET", "POST");

    public static final String INTERFACE_NAME_REGEX = "^[a-zA-z][a-zA-Z0-9_-]{0,62}[a-zA-Z0-9]$";
    public static final Pattern INTERFACE_NAME_PATTERN = Pattern.compile(INTERFACE_NAME_REGEX);
}
