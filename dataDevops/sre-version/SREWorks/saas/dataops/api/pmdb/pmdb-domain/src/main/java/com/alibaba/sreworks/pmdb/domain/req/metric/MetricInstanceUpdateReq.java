package com.alibaba.sreworks.pmdb.domain.req.metric;

import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Setter;

/**
 * 创建指标实例配置
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 19:34
 */
@ApiModel(value="指标实例配置")
public class MetricInstanceUpdateReq extends  MetricInstanceBaseReq {
    @Setter
    @ApiModelProperty(value = "指标实例ID", example = "1")
    private Long id;

    public Long getId() {
        Preconditions.checkArgument(id != null, "id must not be null");
        return id;
    }
}
