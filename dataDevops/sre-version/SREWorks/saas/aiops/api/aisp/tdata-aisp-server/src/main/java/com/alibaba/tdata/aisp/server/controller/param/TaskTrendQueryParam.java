package com.alibaba.tdata.aisp.server.controller.param;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * @ClassName: TaskTrendQueryParam
 * @Author: dyj
 * @DATE: 2021-11-29
 * @Description:
 **/
@Data
@ApiModel(value = "调用趋势查询参数")
public class TaskTrendQueryParam {
    private String tsRange;
}
