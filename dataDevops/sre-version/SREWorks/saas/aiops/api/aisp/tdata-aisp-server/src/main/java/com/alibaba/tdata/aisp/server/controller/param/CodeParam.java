package com.alibaba.tdata.aisp.server.controller.param;

import javax.validation.constraints.NotNull;

import com.alibaba.fastjson.JSONObject;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: CodeParam
 * @Author: dyj
 * @DATE: 2021-11-24
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeParam {
    @NotNull(message = "input can not be null!")
    @ApiModelProperty(notes = "input", required = true)
    private JSONObject input;

    @NotNull(message = "codeType can not be null!")
    @ApiModelProperty(notes = "codeType", required = true)
    private String codeType;

    @NotNull(message = "url can not be null!")
    @ApiModelProperty(notes = "url", required = true)
    private String url;
}
