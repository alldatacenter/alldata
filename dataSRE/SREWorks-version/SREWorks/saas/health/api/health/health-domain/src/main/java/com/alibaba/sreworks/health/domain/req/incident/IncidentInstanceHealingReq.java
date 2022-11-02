package com.alibaba.sreworks.health.domain.req.incident;

import com.alibaba.sreworks.health.common.incident.SelfHealingStatus;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * 异常实例自愈请求
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 14:32
 */
@Data
@ApiModel(value = "更新异常实例")
public class IncidentInstanceHealingReq {

    @ApiModelProperty(value = "自愈开始时间戳", example = "1635391617000")
    Long selfHealingStartTs;

    @ApiModelProperty(value = "自愈结束时间戳", example = "1635391617000")
    Long selfHealingEndTs;

    @ApiModelProperty(value = "自愈状态", example = "running")
    String selfHealingStatus;

    public Long getSelfHealingStartTs() {
        if (selfHealingStartTs != null && String.valueOf(selfHealingStartTs).length() == 10) {
            selfHealingStartTs = selfHealingStartTs * 1000;
        }
        return selfHealingStartTs;
    }

    public Long getSelfHealingEndTs() {
        if (selfHealingEndTs != null && String.valueOf(selfHealingEndTs).length() == 10) {
            selfHealingEndTs = selfHealingEndTs * 1000;
        }
        return selfHealingEndTs;
    }

    public String getSelfHealingStatus() {
        if (StringUtils.isNotEmpty(selfHealingStatus)) {
            selfHealingStatus = selfHealingStatus.toUpperCase();
            Preconditions.checkArgument(EnumUtils.isValidEnumIgnoreCase(SelfHealingStatus.class, selfHealingStatus), "自愈状态非法, 合法取值:" + Arrays.toString(SelfHealingStatus.values()));
        }
        return selfHealingStatus;
    }

}
