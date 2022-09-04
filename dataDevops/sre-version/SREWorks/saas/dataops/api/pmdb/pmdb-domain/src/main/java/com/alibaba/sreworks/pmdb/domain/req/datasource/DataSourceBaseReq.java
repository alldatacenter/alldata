package com.alibaba.sreworks.pmdb.domain.req.datasource;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 数据源配置基类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 19:34
 */
@ApiModel(value="数据源配置")
@Data
public class DataSourceBaseReq {
    @ApiModelProperty(value = "数据源名称", example = "pod_cpu_usage")
    String name;

    @ApiModelProperty(value = "数据源类型", example = "es")
    String type;

    @ApiModelProperty(value = "数据源链接配置")
    JSONObject connectConfig;

    @ApiModelProperty(value = "所属应用", example = "0")
    String appId;

    @ApiModelProperty(hidden = true)
    String creator;

    @ApiModelProperty(hidden = true)
    String lastModifier;

    @ApiModelProperty(value = "描述", example = "xxx")
    String description;
}
