package com.alibaba.sreworks.health.common.constant;

import com.google.common.collect.ImmutableList;

import java.util.regex.Pattern;

/**
 * 常量
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 17:00
 */
public class Constant {

    /**
     * 基础常量
     */
    public final static String APP_NAME = "health";

    public final static String SREWORKS_APP_ID = "0";

    public static final long MAX_SECONDS_TIMESTAMP = 9999999999L;

    public final static Long ONE_DAY_MILLISECOND = 86400000L;

    /**
     * 数仓相关常量
     */
    public final static String DW_DB_NAME = "sw_dw";
    public final static String DW_DWD_LAYER = "dwd";
    public final static String DOMAIN_FOR_HEALTH = "stab";   // stability 稳定性域
    public static final String PARTITION_BY_DAY = "{now/d}";
    public static final String PARTITION_BY_MONTH = "{now/M{yyyy.MM}}";
    public static final Integer DEFAULT_LIFE_CYCLE = 365;
    public static final Integer MAX_LIFE_CYCLE = 3650;
    public static final Integer MIN_LIFE_CYCLE = 1;

    public final static String DW_ALIAS_NAME = "sw_dw";


    public final static Integer MAX_DATA_FLUSH_SIZE = 2000;   // 模型数据落盘最大数量
    public final static Integer DATA_FLUSH_STEP = 200;   // 模型数据落盘每批数量

    /*** meta配置的相关常量 */
    public final static String META_PREFIX = "_";
    public final static String ID = "id";
    public final static String META_TYPE = META_PREFIX + "type";
    public final static String META_ID = META_PREFIX + ID;

//    public static final String PARTITION_FIELD = "ds";
//    public static final Boolean PARTITION_BUILD_IN = true;
//    public static final ColumnType PARTITION_TYPE = ColumnType.STRING;
//    public static final String PARTITION_DIM = "ds";
//    public static final String PARTITION_ALIAS = "分区列";
//    public static final Boolean PARTITION_NULLABLE = false;
//    public static final String PARTITION_DESCRIPTION = "时间分区字段";

    /**
     * 缓存相关配置
     */
    public final static Long CACHE_EXPIRE_SECONDS = 300L;
    public final static Long CACHE_MAX_SIZE = 5L;
    public final static String CACHE_INCIDENT_DEF_KEY = "incident_def";
    public final static String CACHE_INCIDENT_TYPE_KEY = "incident_type";
    public final static String CACHE_RISK_TYPE_KEY = "risk_type";

    /**
     * 事件定义相关常量
     */
    public final static String EVENT = "event";
    public final static String RISK = "risk";
    public final static String ALERT = "alert";
    public final static String INCIDENT = "incident";
    public final static String FAILURE = "failure";
    public final static ImmutableList<String> DEFINITION_CATEGORIES = ImmutableList.of(EVENT, RISK, ALERT, INCIDENT, FAILURE);
    public final static Integer DEFAULT_STORAGE_DAYS = 7;

    /**
     * 事件定义屏蔽规则
     */
    public final static ImmutableList<String> SHIELD_TYPES = ImmutableList.of("all", "custom");
    public final static ImmutableList<String> SHIELD_TIME_TYPES = ImmutableList.of("period", "absolute");

    /**
     * 事件定义收敛规则
     */
    public final static ImmutableList<String> CONVERGENCE_TYPES = ImmutableList.of("all", "custom");

    // 故障相关常量
    /**
     * 故障等级定义
     */
    public final static Pattern FAILURE_LEVEL_PATTERN = Pattern.compile("^[P|p][0-4]$");
    public final static String FAILURE_LEVEL_PREFIX = "P";
    public final static Integer MAX_FAILURE_LEVEL = 0;
    public final static Integer MIN_FAILURE_LEVEL = 4;

    // 告警相关常量
    /**
     * 告警等级
     */
    public final static ImmutableList<String> ALERT_RULE_OPERATORS = ImmutableList.of(">", ">=", "=", "<=", "<", "!=");

    // 风险相关常量
//    public final static ImmutableList<String> RISK_LEVELS = ImmutableList.of("low", "medium", "high");
}
