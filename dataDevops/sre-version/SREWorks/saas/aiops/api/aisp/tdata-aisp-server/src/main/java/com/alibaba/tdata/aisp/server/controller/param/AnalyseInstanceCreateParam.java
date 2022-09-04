package com.alibaba.tdata.aisp.server.controller.param;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: AnalyseInstanceCreateParam
 * @Author: dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "instance create参数")
public class AnalyseInstanceCreateParam {
    @NotNull(message = "sceneCode can not be null!")
    @ApiModelProperty(notes = "场景Code", required = true)
    private String sceneCode;
    @NotNull(message = "detectorCode can not be null!")
    @ApiModelProperty(notes = "检测器Code", required = true)
    private String detectorCode;
    @NotNull(message = "entityId can not be null!")
    @ApiModelProperty(notes = "entityId", required = true)
    private String entityId;
}
