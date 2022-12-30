package com.alibaba.sreworks.health.domain.req.failure;

import com.alibaba.sreworks.health.common.constant.Constant;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import org.apache.commons.lang3.StringUtils;


/**
 * 创建故障实例请求
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 14:32
 */

@ApiModel(value = "创建故障实例")
public class FailureInstanceCreateReq extends FailureInstanceBaseReq {

    @Override
    public Integer getDefId() {
        Preconditions.checkArgument(defId != null, "故障定义ID不允许为空");
        return defId;
    }

    @Override
    public String getAppInstanceId() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(appInstanceId), "应用实例不允许为空");
        return appInstanceId;
    }

    @Override
    public Long getIncidentId() {
        Preconditions.checkArgument(incidentId != null, "异常实例ID不允许为空");
        return incidentId;
    }

    @Override
    public String getName() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(name), "故障名称不允许为空");
        return name;
    }

    @Override
    public String getLevel() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(level), "故障等级不允许为空");
        Preconditions.checkArgument(Constant.FAILURE_LEVEL_PATTERN.matcher(level).find(), "故障等级参数非法,目前仅支持[P4, P3, P2, P1, P0]");
        return level;
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
        if (recoveryTs != null) {
            if (String.valueOf(recoveryTs).length() == 10) {
                recoveryTs = recoveryTs * 1000;
            }
        }
        return recoveryTs;
    }
}
