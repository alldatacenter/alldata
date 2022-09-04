package com.alibaba.sreworks.dataset.domain.req.inter;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 模型接口信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@Data
@ApiModel(value="创建数据接口配置(带参数)")
public class DataInterfaceCreateComplexReq extends DataInterfaceConfigCreateReq {

    @ApiModelProperty(value = "请求参数", example = "column", required = true)
    DataInterfaceParamCreateReq[] params;
}
