package com.alibaba.sreworks.pmdb.common.constant;

import com.google.common.collect.ImmutableList;

/**
 * 指标类型
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 20:41
 */
public class DataSourceConstant {
    public static final String ES = "es";
    public static final String MYSQL = "mysql";
    public static final ImmutableList<String> DATASOURCE_TYPES = ImmutableList.of(ES, MYSQL);

}
