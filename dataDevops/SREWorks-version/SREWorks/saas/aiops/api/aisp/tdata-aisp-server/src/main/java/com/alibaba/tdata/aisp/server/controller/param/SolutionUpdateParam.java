package com.alibaba.tdata.aisp.server.controller.param;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.alibaba.tdata.aisp.server.common.dto.UserSimpleInfo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: SolutionUpdateParam
 * @Author: dyj
 * @DATE: 2022-05-09
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "algoInstance update参数")
public class SolutionUpdateParam {
    @NotNull(message = "algoInstanceCode can not be null!")
    @ApiModelProperty(notes = "algoInstanceCode", required = true)
    private String algoInstanceCode;

    @ApiModelProperty(notes = "关联的产品")
    private List<String> productList;

    @NotNull(message = "ownerInfoList can not be null!")
    @ApiModelProperty(notes = "ownerInfoList")
    private List<UserSimpleInfo> ownerInfoList;

    private String algoInstanceName;

    private String comment;
}
