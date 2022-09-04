package com.alibaba.tdata.aisp.server.controller.param;

import javax.validation.constraints.NotNull;

import com.alibaba.fastjson.JSONObject;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @ClassName: AnalyseInstanceUpdateParam
 * @Author: dyj
 * @DATE: 2021-12-20
 * @Description:
 **/
@Data
public class AnalyseInstanceUpdateParam {
    @NotNull(message = "sceneCode can not be null!")
    @ApiModelProperty(notes = "场景Code", required = true)
    private String sceneCode;
    @NotNull(message = "detectorCode can not be null!")
    @ApiModelProperty(notes = "检测器Code", required = true)
    private String detectorCode;
    @NotNull(message = "entityId can not be null!")
    @ApiModelProperty(notes = "entityId", required = true)
    private String entityId;

    private JSONObject modelParam;
}
