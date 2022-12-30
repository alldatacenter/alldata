package com.alibaba.tdata.aisp.server.controller.param;

import javax.validation.constraints.NotNull;

import com.alibaba.fastjson.JSONObject;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: ModelUpsertParam
 * @Author: dyj
 * @DATE: 2022-05-10
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelUpsertParam {
    @NotNull(message = "sceneCode can not be null!")
    @ApiModelProperty(notes = "场景Code", required = true)
    private String sceneCode;

    @NotNull(message = "detectorCode can not be null!")
    @ApiModelProperty(notes = "检测器Code", required = true)
    private String detectorCode;

    @NotNull(message = "level can not be null!")
    @ApiModelProperty(notes = "level", required = true)
    private String level;

    private String entityId;

    @NotNull(message = "modelParam can not be null!")
    @ApiModelProperty(notes = "modelParam", required = true)
    private JSONObject modelParam;
}
