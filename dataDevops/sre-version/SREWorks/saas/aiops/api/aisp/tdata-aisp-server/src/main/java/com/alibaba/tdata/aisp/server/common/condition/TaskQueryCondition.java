package com.alibaba.tdata.aisp.server.common.condition;

import java.util.Date;

import com.alibaba.tdata.aisp.server.common.constant.AnalyseTaskStatusEnum;
import com.alibaba.tdata.aisp.server.common.constant.AnalyseTaskTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: TaskQueryCondition
 * @Author: dyj
 * @DATE: 2021-11-18
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskQueryCondition {
    private String taskUuid;

    private String sceneCode;

    private String detectorCode;

    private String instanceCode;

    private Date startTime;

    private Date endTime;

    private AnalyseTaskTypeEnum taskType;

    private AnalyseTaskStatusEnum taskStatus;
}
