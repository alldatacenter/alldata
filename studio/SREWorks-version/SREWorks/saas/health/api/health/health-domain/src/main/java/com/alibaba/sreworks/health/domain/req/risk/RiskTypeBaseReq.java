package com.alibaba.sreworks.health.domain.req.risk;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 风险类型请求基类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/20 11:47
 */
@Data
@ApiModel(value = "异常类型")
public class RiskTypeBaseReq {
    @ApiModelProperty(value = "类型标识", example = "traffic")
    String label;

    @ApiModelProperty(value = "类型名称", example = "流量")
    String name;

    @ApiModelProperty(value = "创建人", example = "user1")
    String creator;

    @ApiModelProperty(value = "最后修改人", example = "user2")
    String lastModifier;

    @ApiModelProperty(value = "类型说明")
    String description;
}
