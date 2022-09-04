package com.alibaba.tdata.aisp.server.controller.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: QueryTaskReportResult
 * @Author: dyj
 * @DATE: 2021-12-08
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskReportResult {
    private int count;

    private double successPercent;
}
