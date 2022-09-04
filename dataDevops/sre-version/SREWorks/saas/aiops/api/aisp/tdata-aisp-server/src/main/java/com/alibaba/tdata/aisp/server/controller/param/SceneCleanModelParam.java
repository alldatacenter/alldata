package com.alibaba.tdata.aisp.server.controller.param;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: SceneCleanModelParam
 * @Author: dyj
 * @DATE: 2022-03-03
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneCleanModelParam {
    @NotNull(message = "sceneCode can not be null!")
    @ApiModelProperty(notes = "场景Code", required = true)
    private String sceneCode;

    @NotNull(message = "detectorCode can not be null!")
    @ApiModelProperty(notes = "检测器Code", required = true)
    private String detectorCode;
}
