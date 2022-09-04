package com.alibaba.sreworks.pmdb.domain.req.metric;

import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Setter;

/**
 * 修改指标配置
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 19:34
 */
@ApiModel(value="指标配置")
public class MetricUpdateReq extends  MetricBaseReq {

    @Setter
    @ApiModelProperty(value = "指标id", example = "1")
    Integer id;

    public Integer getId() {
        Preconditions.checkArgument(id != null, "metric id must not be empty");
        return id;
    }

}
