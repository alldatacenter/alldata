package com.alibaba.tdata.aisp.server.controller.param;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @ClassName: AnalyseInstanceQueryParam
 * @Author: dyj
 * @DATE: 2021-12-20
 * @Description:
 **/
@Data
public class AnalyseInstanceQueryParam {
    private String instanceCode;

    private String sceneCode;

    private String detectorCode;

    private String entityId;

    @NotNull(message = "page can not be null!")
    @ApiModelProperty(required = true)
    private Integer page;

    @NotNull(message = "pageSize can not be null!")
    @ApiModelProperty(required = true)
    private Integer pageSize;
}
