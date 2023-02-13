package com.alibaba.sreworks.warehouse.common.constant;

import com.google.common.collect.ImmutableList;

/**
 * 常量
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/18 20:42
 */
public class Constant {

    public static final long MAX_SECONDS_TIMESTAMP = 9999999999L;

    public final static String DW_ENTITY = "entity";
    public final static String DW_MODEL = "model";

    public final static ImmutableList<String> DW_TYPES = ImmutableList.of(DW_ENTITY, DW_MODEL);

    /**
     * 缓存五分钟
     */
    public final static Long CACHE_EXPIRE_SECONDS = 300L;
    public final static Long CACHE_MAX_SIZE = 5L;

    public final static String DW_DB_NAME = "sw_dw";

    /**
     * 模型数据落盘最大数量
     */
    public final static Integer MAX_MODEL_DATA_FLUSH_SIZE = 1000;

    /**
     * 模型数据落盘每批数量
     */
    public final static Integer MODEL_DATA_FLUSH_STEP = 100;
}
