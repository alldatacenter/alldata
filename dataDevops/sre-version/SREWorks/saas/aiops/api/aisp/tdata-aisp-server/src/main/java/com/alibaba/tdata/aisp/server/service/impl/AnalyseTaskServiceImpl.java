package com.alibaba.tdata.aisp.server.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tdata.aisp.server.common.condition.TaskQueryCondition;
import com.alibaba.tdata.aisp.server.common.constant.AnalyseTaskStatusEnum;
import com.alibaba.tdata.aisp.server.common.constant.AnalyseTaskTypeEnum;
import com.alibaba.tdata.aisp.server.common.properties.TaskRemainProperties;
import com.alibaba.tdata.aisp.server.common.utils.AispAlgorithmUtil;
import com.alibaba.tdata.aisp.server.common.utils.MessageDigestUtil;
import com.alibaba.tdata.aisp.server.common.utils.TaskCacheUtil;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseTaskCreateParam;
import com.alibaba.tdata.aisp.server.controller.param.AnalyzeTaskUpdateParam;
import com.alibaba.tdata.aisp.server.controller.param.TaskQueryParam;
import com.alibaba.tdata.aisp.server.controller.param.TaskTrendQueryParam;
import com.alibaba.tdata.aisp.server.controller.result.TaskQueryResult;
import com.alibaba.tdata.aisp.server.controller.result.TaskReportResult;
import com.alibaba.tdata.aisp.server.repository.AnalyseTaskRepository;
import com.alibaba.tdata.aisp.server.repository.domain.TaskDO;
import com.alibaba.tdata.aisp.server.repository.domain.TaskTrendDO;
import com.alibaba.tdata.aisp.server.service.AnalyseTaskService;

import com.alicp.jetcache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @ClassName: AnalyseTaskServiceImpl
 * @Author: dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
@Slf4j
@Service
public class AnalyseTaskServiceImpl implements AnalyseTaskService {
    @Autowired
    private AnalyseTaskRepository taskRepository;
    @Autowired
    @Qualifier("taskResultCache")
    private Cache<String, JSONObject> taskResultCache;
    @Autowired
    private TaskRemainProperties taskRemainProperties;

    /**
     * @param param
     * @return
     */
    @Override
    public String create(AnalyseTaskCreateParam param) {
        TaskDO taskDO = new TaskDO();
        taskDO.setTaskUuid(param.getTaskUUID());
        taskDO.setSceneCode(param.getSceneCode());
        taskDO.setDetectorCode(param.getDetectorCode());
        taskDO.setTaskType(param.getTaskTypeEnum().getValue());
        taskDO.setTaskStatus(param.getTaskStatusEnum().getValue());
        taskDO.setInstanceCode(param.getInstanceCode());
        taskDO.setTaskReq(param.getReqParam().toJSONString());
        taskRepository.insert(taskDO);
        return taskDO.getTaskUuid();
    }

    @Override
    public boolean updateTaskRecord(AnalyzeTaskUpdateParam param) {
        TaskDO taskDO = taskRepository.queryById(param.getTaskUUID());
        if (taskDO == null) {
            throw new IllegalArgumentException(
                "action=updateTaskRecord || Can not find task record from taskUUID:" + param.getTaskUUID());
        }
        AnalyseTaskStatusEnum taskStatusEnum = AnalyseTaskStatusEnum.fromValue(param.getStatus());
        taskDO.setTaskStatus(taskStatusEnum.getValue());
        taskDO.setTaskResult(JSONObject.toJSONString(param));
        Date gmtCreate = taskDO.getGmtCreate();
        long cost = System.currentTimeMillis() - gmtCreate.getTime();
        taskDO.setCostTime(cost);
        taskRepository.updateSelectiveById(taskDO);
        taskResultCache.put(TaskCacheUtil.genResultKey(param.getTaskUUID()),
            JSONObject.parseObject(JSONObject.toJSONString(param)));
        return true;
    }

    /**
     * @param param
     * @param sceneCode
     * @param detectorCode
     * @return
     */
    @Override
    public TaskQueryResult taskList(TaskQueryParam param, String sceneCode, String detectorCode) {
        TaskQueryCondition taskQueryCondition = new TaskQueryCondition();
        taskQueryCondition.setSceneCode(sceneCode);
        taskQueryCondition.setDetectorCode(detectorCode);
        if (!StringUtils.isEmpty(param.getTaskUuid())) {
            taskQueryCondition.setTaskUuid(param.getTaskUuid());
        }
        if (!StringUtils.isEmpty(param.getTaskType())) {
            taskQueryCondition.setTaskType(AnalyseTaskTypeEnum.fromValue(param.getTaskType()));
        }
        if (!StringUtils.isEmpty(param.getTaskStatus())) {
            taskQueryCondition.setTaskStatus(AnalyseTaskStatusEnum.fromValue(param.getTaskStatus()));
        }
        if (!StringUtils.isEmpty(param.getEntityId())) {
            taskQueryCondition.setInstanceCode(MessageDigestUtil.genSHA256(
                sceneCode
                    .concat(detectorCode)
                    .concat(param.getEntityId())));
        }
        List<TaskDO> taskDOList = taskRepository.queryByRowBounds(taskQueryCondition, param.getPage(),
            param.getPageSize());
        long count = taskRepository.count(taskQueryCondition);
        return TaskQueryResult.builder().total(count).items(taskDOList).build();
    }

    /**
     * @param taskUUID
     * @return
     */
    @Override
    public JSONObject queryTaskRes(String taskUUID, String empId) {
        JSONObject result = new JSONObject();
        TaskDO taskDO = taskRepository.queryById(taskUUID);
        if (taskDO == null) {
            throw new IllegalArgumentException(
                "action=queryTaskRes || Can not find task record from taskUUID:" + taskUUID);
        }
        AnalyseTaskStatusEnum taskStatusEnum = AnalyseTaskStatusEnum.fromValue(taskDO.getTaskStatus());

        JSONObject reqParam = taskResultCache.get(TaskCacheUtil.genReqKey(taskUUID));
        if (reqParam != null) {
            result.put("requestParam", reqParam);
        }

        JSONObject response = taskResultCache.get(TaskCacheUtil.genResultKey(taskUUID));
        if (response != null) {
            AispAlgorithmUtil.filterAlgorithmParam(response, empId);
            result.put("response", response);
        }

        result.put("status", taskStatusEnum.getValue());

        return result;
    }

    /**
     * @param param
     * @param sceneCode
     * @param detectorCode
     * @return
     */
    @Override
    public Map<String, List<JSONArray>> queryTaskTrend(TaskTrendQueryParam param, String sceneCode,
        String detectorCode) {
        Date end;
        Date start;
        if (!StringUtils.isEmpty(param.getTsRange())) {
            String[] split = param.getTsRange().split(",");
            start = new Date(Long.parseLong(split[0]));
            end = new Date(Long.parseLong(split[1]));
        } else {
            end = new Date();
            start = DateUtils.addDays(end, -taskRemainProperties.getDays());
        }
        List<TaskTrendDO> queryTrend = taskRepository.queryTrend(sceneCode, detectorCode, start, end);
        List<JSONArray> trendSeries = queryTrend.stream().map(x -> {
            JSONArray array = new JSONArray();
            array.add(x.getTime().getTime());
            array.add(x.getCount());
            return array;
        }).collect(Collectors.toList());
        Map<String, List<JSONArray>> res = new LinkedHashMap<>();
        res.put("qps", trendSeries);
        return res;
    }

    /**
     * @param param
     * @param sceneCode
     * @param detectorCode
     * @return
     */
    @Override
    public TaskReportResult queryTaskReport(TaskTrendQueryParam param, String sceneCode, String detectorCode) {
        Date end;
        Date start;
        if (!StringUtils.isEmpty(param.getTsRange())) {
            String[] split = param.getTsRange().split(",");
            start = new Date(Long.parseLong(split[0]));
            end = new Date(Long.parseLong(split[1]));
        } else {
            end = new Date();
            start = DateUtils.addDays(end, -taskRemainProperties.getDays());
        }
        List<TaskDO> taskDOList = taskRepository.queryByCondition(TaskQueryCondition.builder()
            .sceneCode(sceneCode)
            .detectorCode(detectorCode)
            .startTime(start)
            .endTime(end)
            .build());
        int count = taskDOList.size();
        long successCount = taskDOList.stream().filter(
            x -> AnalyseTaskStatusEnum.SUCCESS.getValue().equalsIgnoreCase(x.getTaskStatus())).count();
        double successPercent = count > 0 ? (double)Math.round((double)successCount * 10000 / count) / 100 : 0;
        return TaskReportResult.builder()
            .count(count)
            .successPercent(successPercent)
            .build();
    }

    @Override
    public Map<String, List<JSONArray>> queryAdLine(String taskUUID) {
        Map<String, List<JSONArray>> lineMap = new HashMap<>();
        JSONObject taskRes = queryTaskRes(taskUUID, "");
        if (CollectionUtils.isEmpty(taskRes)) {
            return lineMap;
        }
        if (taskRes.containsKey("requestParam") && taskRes.getJSONObject("requestParam").containsKey("series")) {
            JSONArray series = taskRes.getJSONObject("requestParam").getJSONArray("series");
            List<JSONArray> seriesLine = new LinkedList<>();
            int size = series.size();
            for (int i = 0; i < size; i++) {
                JSONArray a = series.getJSONArray(i);
                a.set(0, a.getLong(0) * 1000);
                seriesLine.add(a);
            }
            lineMap.put("origin", seriesLine);
        }

        if (taskRes.containsKey("response") && taskRes.getJSONObject("response").containsKey("data")
            && taskRes.getJSONObject("response").getJSONObject("data").containsKey("detectSeries")) {
            List<JSONArray> ads = new LinkedList<>();
            List<JSONArray> ups = new LinkedList<>();
            List<JSONArray> lows = new LinkedList<>();
            JSONArray cols = taskRes.getJSONObject("response").getJSONObject("data").getJSONArray(
                "detectSeriesColumns");
            int tsIdx = cols.indexOf("timestamp");
            int vIdx = cols.indexOf("value");
            int anomalyIdx = cols.indexOf("anomaly");
            int upIdx = cols.indexOf("upperbound");
            int lowIdx = cols.indexOf("lowerbound");
            JSONArray detectSeries = taskRes.getJSONObject("response").getJSONObject("data").getJSONArray(
                "detectSeries");
            int size = detectSeries.size();
            for (int i = 0; i < size; i++) {
                JSONArray d = detectSeries.getJSONArray(i);
                if (d.getBoolean(anomalyIdx)) {
                    long timestamp = d.getLong(tsIdx) * 1000;

                    JSONArray ad = new JSONArray();
                    ad.set(0, timestamp);
                    ad.set(1, d.getFloat(vIdx));
                    ads.add(ad);

                    if (upIdx >= 0) {
                        JSONArray up = new JSONArray();
                        up.set(0, timestamp);
                        up.set(1, d.getFloat(upIdx));
                        ups.add(up);
                    }

                    if (lowIdx >= 0) {
                        JSONArray low = new JSONArray();
                        low.set(0, timestamp);
                        low.set(1, d.getFloat(lowIdx));
                        lows.add(low);
                    }
                }
            }
            lineMap.put("ads", ads);
            if (upIdx >= 0) {
                lineMap.put("ups", ups);
            }
            if (lowIdx >= 0) {
                lineMap.put("lows", lows);
            }

        }
        return lineMap;
    }
}
