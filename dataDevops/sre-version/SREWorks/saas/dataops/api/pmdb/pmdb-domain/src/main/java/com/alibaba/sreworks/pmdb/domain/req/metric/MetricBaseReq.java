package com.alibaba.sreworks.pmdb.domain.req.metric;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 指标配置基类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 19:34
 */
@ApiModel(value="指标配置")
@Data
public class MetricBaseReq {
    @ApiModelProperty(value = "指标名称", example = "pod_cpu_usage")
    String name;

    @ApiModelProperty(value = "指标别名", example = "xxx")
    String alias;

    @ApiModelProperty(value = "指标类型", example = "性能指标")
    String type;

    @ApiModelProperty(value = "指标标签", example = "")
    JSONObject labels;

    @ApiModelProperty(hidden = true)
    String creator;

    @ApiModelProperty(hidden = true)
    String lastModifier;

    @ApiModelProperty(value = "指标描述", example = "xxx")
    String description;
}
