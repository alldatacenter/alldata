package com.alibaba.tdata.aisp.server.controller.param;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tdata.aisp.server.common.constant.AnalyseTaskStatusEnum;
import com.alibaba.tdata.aisp.server.common.constant.AnalyseTaskTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: AnalyseTaskCreateParam
 * @Author: dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyseTaskCreateParam {
    private String taskUUID;
    private String sceneCode;
    private String detectorCode;
    private AnalyseTaskTypeEnum taskTypeEnum;
    private AnalyseTaskStatusEnum taskStatusEnum;
    private String instanceCode;
    private JSONObject reqParam;
}
