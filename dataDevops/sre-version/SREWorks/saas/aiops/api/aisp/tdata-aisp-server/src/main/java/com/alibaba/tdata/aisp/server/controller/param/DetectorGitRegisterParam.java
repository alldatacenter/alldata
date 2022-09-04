package com.alibaba.tdata.aisp.server.controller.param;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: DetectorGitRegisterParam
 * @Author: dyj
 * @DATE: 2021-11-17
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "detector git register参数")
public class DetectorGitRegisterParam {
    @NotNull(message = "detectorCode can not be null!")
    @ApiModelProperty(notes = "检测器Code", required = true)
    private String detectorCode;
    @NotNull(message = "rep can not be null!")
    @ApiModelProperty(notes = "rep", required = true)
    private String rep;
    @NotNull(message = "branch can not be null!")
    @ApiModelProperty(notes = "branch", required = true)
    private String branch;
    @NotNull(message = "gitToken can not be null!")
    @ApiModelProperty(notes = "gitToken", required = true)
    private String gitToken;
    @NotNull(message = "gitAccount can not be null!")
    @ApiModelProperty(notes = "gitAccount", required = true)
    private String gitAccount;
}
