package com.alibaba.tdata.aisp.server.controller.param;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @ClassName: TaskQueryParam
 * @Author: dyj
 * @DATE: 2021-11-18
 * @Description:
 **/
@Data
@ApiModel(value = "根据条件查找检测任务")
public class TaskQueryParam {
    private String taskUuid;

    private String taskStatus;

    private String taskType;

    private String entityId;

    @NotNull(message = "page can not be null!")
    @ApiModelProperty(required = true)
    private Integer page;

    @NotNull(message = "pageSize can not be null!")
    @ApiModelProperty(required = true)
    private Integer pageSize;
}
