package com.alibaba.sreworks.health.domain.req.event;

import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import org.apache.commons.lang3.StringUtils;

/**
 * 创建事件实例请求
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/05 16:27
 */
@ApiModel(value = "创建事件实例")
public class EventInstanceCreateReq extends EventInstanceBaseReq {
    @Override
    public String getAppInstanceId() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(appInstanceId), "应用实例不允许为空");
        return appInstanceId;
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
    public String getSource() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(source), "事件来源不允许为空");
        return source;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public Integer getDefId() {
        Preconditions.checkArgument(defId != null, "事件定义ID不允许为空");
        return defId;
    }
}
