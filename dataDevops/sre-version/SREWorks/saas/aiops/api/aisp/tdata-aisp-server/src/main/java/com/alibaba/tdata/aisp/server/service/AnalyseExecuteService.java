package com.alibaba.tdata.aisp.server.service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceFeedbackParam;
import com.alibaba.tdata.aisp.server.controller.param.AnalyzeTaskUpdateParam;
import com.alibaba.tdata.aisp.server.controller.param.CodeParam;
import com.alibaba.tdata.aisp.server.controller.param.TaskQueryParam;
import com.alibaba.tdata.aisp.server.controller.param.TaskTrendQueryParam;
import com.alibaba.tdata.aisp.server.controller.result.TaskQueryResult;
import com.alibaba.tdata.aisp.server.controller.result.TaskReportResult;

/**
 **/
public interface AnalyseExecuteService {

    /**
     * @param taskUUID
     * @param param
     * @param sceneCode
     * @param detectorCode
     * @return
     */
    public JSONObject exec(String taskUUID, JSONObject param, String sceneCode, String detectorCode);

    /**
     * @param detectorCode
     * @return
     */
    public String getDoc(String detectorCode);

    /**
     * @param param
     * @return
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    String code(CodeParam param) throws IOException, NoSuchAlgorithmException;

    /**
     * @param detectorCode
     * @return
     */
    JSONObject getInput(String detectorCode);

    /**
     * @param sceneCode
     * @param detectorCode
     * @param param
     * @return
     */
    boolean feedback(String sceneCode, String detectorCode, AnalyseInstanceFeedbackParam param);

}
