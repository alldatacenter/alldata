package com.alibaba.sreworks.health.domain.req.incident;

import com.alibaba.sreworks.health.common.incident.SelfHealingStatus;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;


/**
 * 创建异常实例请求
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 14:32
 */

@ApiModel(value = "创建异常实例")
public class IncidentInstanceCreateReq extends IncidentInstanceBaseReq {
    @Override
    public Integer getDefId() {
        Preconditions.checkArgument(defId != null, "异常定义ID不允许为空");
        return defId;
    }

    @Override
    public String getAppInstanceId() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(appInstanceId), "应用实例不允许为空");
        return appInstanceId;
    }

    @Override
    public String getTraceId() {
//        Preconditions.checkArgument(StringUtils.isNotEmpty(traceId), "任务链ID不允许为空");
        return traceId;
    }

    @Override
    public String getSpanId() {
//        Preconditions.checkArgument(StringUtils.isNotEmpty(spanId), "任务链层次ID不允许为空");
        return spanId;
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
    public Long getRecoveryTs() {
        if (recoveryTs != null && String.valueOf(recoveryTs).length() == 10) {
            recoveryTs = recoveryTs * 1000;
        }
        return recoveryTs;
    }

    @Override
    public Long getSelfHealingStartTs() {
        if (selfHealingStartTs != null && String.valueOf(selfHealingStartTs).length() == 10) {
            selfHealingStartTs = selfHealingStartTs * 1000;
        }
        return selfHealingStartTs;
    }

    @Override
    public Long getSelfHealingEndTs() {
        if (selfHealingEndTs != null && String.valueOf(selfHealingEndTs).length() == 10) {
            selfHealingEndTs = selfHealingEndTs * 1000;
        }
        return selfHealingEndTs;
    }

    @Override
    public String getSelfHealingStatus() {
        if (StringUtils.isNotEmpty(selfHealingStatus)) {
            selfHealingStatus = selfHealingStatus.toUpperCase();
            Preconditions.checkArgument(EnumUtils.isValidEnumIgnoreCase(SelfHealingStatus.class, selfHealingStatus), "自愈状态非法, 合法取值:" + Arrays.toString(SelfHealingStatus.values()));
        }
        return selfHealingStatus;
    }

    @Override
    public String getSource() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(source), "异常来源不允许为空");
        return source;
    }
}
