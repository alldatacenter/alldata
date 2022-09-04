package com.alibaba.tdata.aisp.server.controller.param;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: DetectorUpdateParam
 * @Author: dyj
 * @DATE: 2021-11-23
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectorUpdateParam {
    @NotNull(message = "detectorCode can not be null!")
    @ApiModelProperty(notes = "检测器Code", required = true)
    private String detectorCode;
    @NotNull(message = "detectorUrl can not be null!")
    @ApiModelProperty(notes = "检测器Url", required = true)
    private String detectorUrl;
    @ApiModelProperty(notes = "检测器备注")
    private String comment;
}
