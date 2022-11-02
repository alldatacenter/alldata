package com.alibaba.tdata.aisp.server.controller.param;

import java.util.List;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: SolutionQueryParam
 * @Author: dyj
 * @DATE: 2022-05-09
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "algoInstance query参数")
public class SolutionQueryParam {
    private String algoInstanceCode;

    private String owners;

    private String algoInstanceName;

    private List<String> productList;

    @NotNull(message = "page can not be null!")
    @ApiModelProperty(required = true)
    private Integer page;

    @NotNull(message = "pageSize can not be null!")
    @ApiModelProperty(required = true)
    private Integer pageSize;
}
