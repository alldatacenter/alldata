package com.alibaba.tdata.aisp.server.controller.param;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: DetectorRegisterParam
 * @Author: dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "detector register参数")
public class DetectorRegisterParam {
    @NotNull(message = "detectorCode can not be null!")
    @ApiModelProperty(notes = "检测器Code", required = true)
    private String detectorCode;
    @NotNull(message = "detectorUrl can not be null!")
    @ApiModelProperty(notes = "检测器Url", required = true)
    private String detectorUrl;
    @ApiModelProperty(notes = "检测器备注")
    private String comment;
}
