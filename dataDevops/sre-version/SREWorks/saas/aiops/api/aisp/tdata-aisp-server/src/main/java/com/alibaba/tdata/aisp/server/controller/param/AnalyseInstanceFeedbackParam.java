package com.alibaba.tdata.aisp.server.controller.param;

import javax.validation.constraints.NotNull;

import com.alibaba.fastjson.JSONObject;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @ClassName: AnalyseInstanceFeedbackParam
 * @Author: dyj
 * @DATE: 2022-02-17
 * @Description:
 **/
@Data
public class AnalyseInstanceFeedbackParam {
    @NotNull(message = "entityId can not be null!")
    @ApiModelProperty(notes = "entityId", required = true)
    private String entityId;

    private JSONObject feedback;
}
