package com.alibaba.sreworks.health.domain.req.alert;

import com.alibaba.sreworks.health.common.alert.AlertLevel;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * 创建告警实例请求
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/04 17:23
 */
public class AlertInstanceCreateReq extends AlertInstanceBaseReq {
    @Override
    public String getAppInstanceId() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(appInstanceId), "应用实例不允许为空");
        return appInstanceId;
    }

    @Override
    public String getLevel() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(level), "告警等级不允许为空");
        level = level.toUpperCase();
        Preconditions.checkArgument(EnumUtils.isValidEnumIgnoreCase(AlertLevel.class, level),
                "告警等级非法, 有效值:" + Arrays.toString(AlertLevel.values()));
        return level;
    }

    @Override
    public String getSource() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(source), "告警来源不允许为空");
        return source;
    }

    @Override
    public Long getOccurTs() {
        occurTs = occurTs == null ? System.currentTimeMillis() : occurTs;
        if (String.valueOf(occurTs).length() == 10) {
            occurTs = occurTs * 1000;
        }

        return occurTs;
    }

    @Override
    public Integer getDefId() {
        Preconditions.checkArgument(defId != null, "告警定义ID不允许为空");
        return defId;
    }
}
