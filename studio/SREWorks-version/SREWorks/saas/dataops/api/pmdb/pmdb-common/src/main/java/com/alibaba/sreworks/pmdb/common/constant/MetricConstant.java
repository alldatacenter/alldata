package com.alibaba.sreworks.pmdb.common.constant;

import com.google.common.collect.ImmutableList;

/**
 * 指标类型
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 20:41
 */
public class MetricConstant {
    public static final ImmutableList<String> METRIC_TYPES = ImmutableList.of("性能指标", "状态指标", "业务指标");

    public static final String METRIC_TAGS_SPLITTER = ",";

    public static final String METRIC_LABEL_APP_INSTANCE = "app_instance_id";

    public static final String METRIC_LABEL_APP_COMPONENT_INSTANCE = "app_component_instance_id";
}
