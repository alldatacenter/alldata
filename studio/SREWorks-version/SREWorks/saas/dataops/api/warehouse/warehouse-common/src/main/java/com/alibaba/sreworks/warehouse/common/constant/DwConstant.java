package com.alibaba.sreworks.warehouse.common.constant;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.warehouse.common.type.ColumnType;
import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class DwConstant {

    public static final String ENTITY_MODEL_NAME_REGEX = "^[A-Z][A-Z0-9_]{0,126}[A-Z0-9]$";

    public static final Pattern ENTITY_MODEL_NAME_PATTERN = Pattern.compile(ENTITY_MODEL_NAME_REGEX);

    public static final String ENTITY_MODEL_FIELD_REGEX = "^[a-zA-Z][a-zA-Z0-9_-]{0,126}[a-zA-Z0-9]$";

    public static final Pattern ENTITY_MODEL_FIELD_PATTERN = Pattern.compile(ENTITY_MODEL_FIELD_REGEX);

    public static final String ENTITY_MODEL_DIM_FIELD_REGEX = "^[a-z][a-z0-9_-]{0,126}[a-z0-9]$";

    public static final Pattern ENTITY_MODEL_DIM_FIELD_PATTERN = Pattern.compile(ENTITY_MODEL_DIM_FIELD_REGEX);


    public static final String PARTITION_FIELD = "ds";
    public static final Boolean PARTITION_BUILD_IN = true;
    public static final ColumnType PARTITION_TYPE = ColumnType.STRING;
    public static final String PARTITION_DIM = "ds";
    public static final String PARTITION_ALIAS = "分区列";
    public static final Boolean PARTITION_NULLABLE = false;
    public static final String PARTITION_DESCRIPTION = "时间分区字段";


    public static final String PRIMARY_FIELD = "id";

    /*** meta配置的相关常量 */
    public final static String META_PREFIX = "_";
    public final static String META_TYPE = META_PREFIX + "type";
    public final static String META_ID = META_PREFIX + "id";

    public static final List<String> DW_LAYERS = Arrays.asList("ods", "dim", "dwd", "dws", "ads");
    public static final List<String> DW_CDM_LAYERS = Arrays.asList("dim", "dwd", "dws");
    public static final String DW_ADS_LAYER = "ads";
    public static final String DW_DWS_LAYER = "dws";
    public static final String DW_DIM_LAYER = "dim";
    public static final String DW_DWD_LAYER = "dwd";
    public static final String DW_ODS_LAYER = "ods";

    /**
     * 分区规范
     */
    public static final String PARTITION_BY_HOUR = "h";
    public static final String PARTITION_BY_DAY = "d";
    public static final String PARTITION_BY_WEEK = "w";
    public static final String PARTITION_BY_MONTH = "M";
    public static final String PARTITION_BY_YEAR = "y";
    public static final ImmutableList<String> PARTITION_FORMATS = ImmutableList.of(PARTITION_BY_HOUR, PARTITION_BY_DAY,
            PARTITION_BY_WEEK, PARTITION_BY_MONTH, PARTITION_BY_YEAR);
    public static final JSONObject INDEX_DATE_MATH = new JSONObject(){
        {
            put(PARTITION_BY_HOUR, "{now/h{yyyy.MM.dd.HH}}");
            put(PARTITION_BY_DAY, "{now/d}");
            put(PARTITION_BY_MONTH, "{now/M{yyyy.MM}}");
            put(PARTITION_BY_YEAR, "{now/y{yyyy}}");
            put(PARTITION_BY_WEEK, "{now/w{yyyy.ww}}");
        }
    };
    public static final JSONObject INDEX_DATE_PATTERN = new JSONObject(){
        {
            put(PARTITION_BY_HOUR, "yyyy.MM.dd.HH");
            put(PARTITION_BY_DAY, "yyyy.MM.dd");
            put(PARTITION_BY_MONTH, "yyyy.MM");
            put(PARTITION_BY_YEAR, "yyyy");
            put(PARTITION_BY_WEEK, "yyy.ww");
        }
    };

    /**
     * 分区数据模式(全量/增量)
     */
    public static final String PARTITION_DATA_MODE_DF = "df";
    public static final String PARTITION_DATA_MODE_DI = "di";
    public static final ImmutableList<String> PARTITION_DATA_MODE = ImmutableList.of(PARTITION_DATA_MODE_DF, PARTITION_DATA_MODE_DI);

    /**
     * 数据统计周期
     */
    public static final ImmutableList<String> STAT_PERIODS = ImmutableList.of("1d", "7d", "30d", "nd", "cw", "cm", "cq", "cy", "hfy", "fy", "td", "ytd", "qtd", "mtd", "wtd", "ftd", "1h", "nh", "dth", "dtr");

    public static final String FORMAT_HOUR = "yyyyMMddHH";
    public static final String FORMAT_DAY = "yyyyMMdd";
    public static final String FORMAT_WEEK = "yyyyww";
    public static final String FORMAT_MONTH = "yyyyMM";
    public static final String FORMAT_YEAR = "yyyy";

    public static final JSONObject DATE_FORMAT = new JSONObject(){
        {
            put(PARTITION_BY_HOUR, FORMAT_HOUR);
            put(PARTITION_BY_DAY, FORMAT_DAY);
            put(PARTITION_BY_MONTH, FORMAT_MONTH);
            put(PARTITION_BY_YEAR, FORMAT_YEAR);
            put(PARTITION_BY_WEEK, FORMAT_WEEK);
        }
    };

    public static final ImmutableList<String> ADS_MODEL_TAGS = ImmutableList.of("交付", "监测", "管理", "控制", "运营", "服务");

    /**
     * 生命周期
     */
    public static final Integer DEFAULT_LIFE_CYCLE = 365;
    public static final Integer MAX_LIFE_CYCLE = 3650;
    public static final Integer MIN_LIFE_CYCLE = 1;
    /**
     * 生命周期策略名前缀
     */
    public final static String LIFECYCLE_POLICY_PREFIX = "sw_dw_lifecycle_%sd";

}
