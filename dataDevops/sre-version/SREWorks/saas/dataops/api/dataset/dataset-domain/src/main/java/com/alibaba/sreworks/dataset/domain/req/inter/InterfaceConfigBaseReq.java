package com.alibaba.sreworks.dataset.domain.req.inter;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 数据接口配置信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@Data
@ApiModel(value="数据接口配置信息")
public class InterfaceConfigBaseReq {

    @ApiModelProperty(value = "接口名称", example = "appId", required = true)
    String name;

    @ApiModelProperty(value = "接口别名", example = "应用ID", required = true)
    String alias;

    @ApiModelProperty(value = "数据源类型", example = "es", required = true)
    String dataSourceType;

    @ApiModelProperty(value = "数据源ID", example = "sreworks", required = true)
    String dataSourceId;

    @ApiModelProperty(value = "数据表", example = "metricbeat", required = true)
    String dataSourceTable;

    @ApiModelProperty(value = "请求方法", example = "GET", required = true)
    String requestMethod;

    @ApiModelProperty(value = "媒体类型", example = "application/json", required = true)
    String contentType;

    @ApiModelProperty(value = "分页", example = "false", required = true)
    Boolean paging;

    @ApiModelProperty(value = "配置模式", example = "guide", required = true)
    String mode;

    @ApiModelProperty(value = "查询模板", example = "xxx", required = true)
    String qlTemplate;

    @ApiModelProperty(value = "查询字段")
    InterfaceQueryFieldReq[] queryFields;

    @ApiModelProperty(value = "分组字段")
    InterfaceGroupFieldReq[] groupFields;

    @ApiModelProperty(value = "排序字段")
    InterfaceSortFieldReq[] sortFields;

    @ApiModelProperty(value = "请求参数")
    InterfaceRequestParamReq[] requestParams;

//    @ApiModelProperty(value = "返回参数", required = true)
//    InterfaceResponseParamReq[] responseParams;

    @ApiModelProperty(value = "返回参数", required = true)
    String[] responseParams;

    @ApiModelProperty(hidden = true)
    Boolean buildIn;

    @ApiModelProperty(hidden = true)
    String creator;

    @ApiModelProperty(hidden = true)
    String lastModifier;
}
