package com.alibaba.tesla.server.controller.param;

import java.util.List;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: ProcessParam
 * @Author: dyj
 * @DATE: 2022-02-28
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessParam {

    @NotNull(message = "taskUUID can not be null!")
    @ApiModelProperty(notes = "Aisp平台任务UUID", required = true)
    private String taskUUID;

    @NotNull(message = "jobName can not be null!")
    @ApiModelProperty(notes = "作业平台作业jobName", required = true)
    private String jobName;

    @NotNull(message = "reqList can not be null!")
    @ApiModelProperty(notes = "请求参数列表", required = true)
    private List<Object> reqList;

    @NotNull(message = "step can not be null!")
    @ApiModelProperty(notes = "步长", required = true)
    private Integer step;

    @NotNull(message = "timeout can not be null!")
    @ApiModelProperty(notes = "超时", required = true)
    private Long timeout;
}
