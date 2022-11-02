package com.alibaba.tdata.aisp.server.controller.param;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.alibaba.tdata.aisp.server.common.dto.UserSimpleInfo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: SolutionCreateParam
 * @Author: dyj
 * @DATE: 2022-05-09
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "algoInstance create参数")
public class SolutionCreateParam {
    @NotNull(message = "algoInstanceCode can not be null!")
    @ApiModelProperty(notes = "algoInstanceCode", required = true)
    private String algoInstanceCode;

    @NotNull(message = "algoInstanceName can not be null!")
    @ApiModelProperty(notes = "algoInstanceName", required = true)
    private String algoInstanceName;

    @ApiModelProperty(notes = "关联的产品")
    private List<String> productList;

    @NotNull(message = "ownerInfoList can not be null!")
    @ApiModelProperty(notes = "owner列表")
    private List<UserSimpleInfo> ownerInfoList;

    @ApiModelProperty(notes = "关联检测器:anomaly_detection,series_predict,qa_generator,drill_down,period_identify.")
    private String detectorBinder;

    @ApiModelProperty(notes = "备注")
    private String comment;
}
