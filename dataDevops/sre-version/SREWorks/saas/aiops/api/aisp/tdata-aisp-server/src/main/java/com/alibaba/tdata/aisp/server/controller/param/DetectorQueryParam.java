package com.alibaba.tdata.aisp.server.controller.param;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: DtectorQueryParam
 * @Author: dyj
 * @DATE: 2021-11-23
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectorQueryParam {
    @ApiModelProperty(notes = "检测器Code")
    private String detectorCode;

    @NotNull(message = "page can not be null!")
    @ApiModelProperty(required = true)
    private Integer page;

    @NotNull(message = "pageSize can not be null!")
    @ApiModelProperty(required = true)
    private Integer pageSize;
}
