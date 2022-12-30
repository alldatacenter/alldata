package com.alibaba.tdata.aisp.server.controller.param;

import javax.validation.constraints.NotNull;

import com.alibaba.fastjson.JSONObject;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: SolutionUpsertModelParam
 * @Author: dyj
 * @DATE: 2022-05-09
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "algoInstance upset model参数")
public class SolutionUpsertModelParam {
    @NotNull(message = "algoInstanceCode can not be null!")
    @ApiModelProperty(notes = "algoInstanceCode", required = true)
    private String algoInstanceCode;

    @NotNull(message = "detectorCode can not be null!")
    @ApiModelProperty(notes = "检测器Code", required = true)
    private String detectorCode;

    @NotNull(message = "instanceModelParam can not be null!")
    @ApiModelProperty(notes = "instanceModelParam", required = true)
    private JSONObject instanceModelParam;
}
