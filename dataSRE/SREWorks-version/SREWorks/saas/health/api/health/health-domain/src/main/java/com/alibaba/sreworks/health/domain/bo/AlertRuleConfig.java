package com.alibaba.sreworks.health.domain.bo;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.sreworks.health.common.alert.AlertLevel;
import lombok.Data;

import java.util.Map;


/**
 * 告警规则配置
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/19 16:30
 */

@Data
public class AlertRuleConfig {
    /**
     * 时段
     */
    @JSONField(name = "duration")
    protected Integer duration;

    /**
     * 连续次数
     */
    @JSONField(name = "times")
    protected Integer times;

    /**
     * 阈值比较符号
     */
    @JSONField(name = "comparator")
    protected String comparator;

    /**
     * 绝对值
     */
    @JSONField(name = "math_abs")
    protected Boolean math_abs;

    /**
     * 阈值
     */
    @JSONField(name = "thresholds")
    protected Map<AlertLevel, Double> thresholds;
}
