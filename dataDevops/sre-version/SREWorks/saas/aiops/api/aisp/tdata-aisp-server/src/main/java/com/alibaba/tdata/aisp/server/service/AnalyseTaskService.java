package com.alibaba.tdata.aisp.server.service;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseTaskCreateParam;
import com.alibaba.tdata.aisp.server.controller.param.AnalyzeTaskUpdateParam;
import com.alibaba.tdata.aisp.server.controller.param.TaskQueryParam;
import com.alibaba.tdata.aisp.server.controller.param.TaskTrendQueryParam;
import com.alibaba.tdata.aisp.server.controller.result.TaskQueryResult;
import com.alibaba.tdata.aisp.server.controller.result.TaskReportResult;
import com.alibaba.tdata.aisp.server.repository.domain.TaskDO;

/**
 **/
public interface AnalyseTaskService {
    /**
     * @param param
     * @return
     */
    public String create(AnalyseTaskCreateParam param);

    /**
     * @param param
     * @return
     */
    boolean updateTaskRecord(AnalyzeTaskUpdateParam param);

    /**
     * @param param
     * @param sceneCode
     * @param detectorCode
     * @return
     */
    TaskQueryResult taskList(TaskQueryParam param, String sceneCode, String detectorCode);

    /**
     * @param taskUUID
     * @return
     */
    JSONObject queryTaskRes(String taskUUID, String empId);

    /**
     * @param param
     * @param sceneCode
     * @param detectorCode
     * @return
     */
    Map<String, List<JSONArray>> queryTaskTrend(TaskTrendQueryParam param, String sceneCode, String detectorCode);

    /**
     * @param param
     * @param sceneCode
     * @param detectorCode
     * @return
     */
    TaskReportResult queryTaskReport(TaskTrendQueryParam param, String sceneCode, String detectorCode);

    /**
     * @param taskUUID
     * @return
     */
    Map<String, List<JSONArray>> queryAdLine(String taskUUID);
}
