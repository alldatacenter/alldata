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
public class DataInterfaceConfigBaseReq {
    @ApiModelProperty(value = "接口标识", example = "appId")
    String label;

    @ApiModelProperty(value = "接口名称", example = "应用ID")
    String name;

//    @ApiModelProperty(value = "内置接口", example = "false")
//    Boolean buildIn;

    @ApiModelProperty(value = "归属模型ID", example = "1")
    Integer modelId;

    @ApiModelProperty(value = "团队ID", example = "0")
    Integer teamId;

    @ApiModelProperty(value = "创建人", example = "sreworks")
    String creator;

    @ApiModelProperty(value = "负责人", example = "sreworks")
    String owners;

    @ApiModelProperty(value = "请求方法", example = "GET")
    String requestMethod;

    @ApiModelProperty(value = "媒体类型", example = "application/json")
    String contentType;

    @ApiModelProperty(value = "响应字段列表", example = "a,b,c")
    String responseFields;

    @ApiModelProperty(value = "排序字段列表")
    DataInterfaceSortFieldReq[] sortFields;

    @ApiModelProperty(value = "分页", example = "false")
    Boolean paging;

    public DataInterfaceSortFieldReq[] getSortFields() {
        if (sortFields != null) {
            // 检查字段是否合法
            for (DataInterfaceSortFieldReq sortFieldReq : sortFields) {
                sortFieldReq.getFieldName();
                sortFieldReq.getOrder();
                sortFieldReq.getMode();
                sortFieldReq.getFormat();
            }
        }
        return sortFields;
    }
}
