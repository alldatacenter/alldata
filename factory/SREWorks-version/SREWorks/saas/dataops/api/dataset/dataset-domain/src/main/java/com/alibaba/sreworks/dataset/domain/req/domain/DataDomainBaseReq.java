package com.alibaba.sreworks.dataset.domain.req.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 模型配置信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@Data
@ApiModel(value="数据域信息")
public class DataDomainBaseReq {

    @ApiModelProperty(value = "数据域名称", example = "domainName")
    String name;

    @ApiModelProperty(value = "数据域缩写", example = "")
    String abbreviation;

//    @ApiModelProperty(value = "内置模型")
//    private Boolean buildIn;

    @ApiModelProperty(value = "归属主题ID", example = "subjectId")
    Integer subjectId;

    @ApiModelProperty(value = "说明")
    String description;
}
