package com.alibaba.tesla.server.service.refresh;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.server.common.constant.TaskPlatformStatusEnum;
import com.alibaba.tesla.server.common.dto.TaskCheckpointDto;
import com.alibaba.tesla.server.common.dto.TaskCheckpointDto.TaskCheckpointDtoBuilder;
import com.alibaba.tesla.server.common.properties.AispProperties;
import com.alibaba.tesla.server.common.properties.TaskPlatformProperties;
import com.alibaba.tesla.server.common.util.RequestUtil;
import com.alibaba.tesla.server.common.util.UrlUtil;
import com.alibaba.tesla.server.service.ProcessService;
import com.alibaba.tesla.server.service.TaskCenter;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @ClassName: ProcessRefreshSchduler
 * @Author: dyj
 * @DATE: 2022-02-28
 * @Description:
 **/
@Component
@Slf4j
public class ProcessRefreshScheduler {

    @Autowired
    private TaskPlatformProperties taskPlatformProperties;
    @Autowired
    private AispProperties aispProperties;
    @Autowired
    private ProcessService processService;

    @Scheduled(initialDelay = 10000L, fixedRate = 10000L)
    public void refresh() {
        log.info("action=refresh|| start refresh! TaskCenter:{}", TaskCenter.toJson());

        String url = UrlUtil.buildUrl(taskPlatformProperties.getQueryUrl());
        for (String taskUuid : TaskCenter.jobInstanceList.keySet()) {
            List<String> instanceList = TaskCenter.jobInstanceList.get(taskUuid);
            if (CollectionUtils.isEmpty(instanceList)) {
                continue;
            }
            for (String instance : new LinkedList<>(instanceList)) {
                JSONObject param = new JSONObject();
                param.put("id", instance);
                try {
                    Response response = RequestUtil.getRaw(url, param, new JSONObject());
                    if (response.isSuccessful()) {
                        assert response.body() != null;
                        String bodyStr = response.body().string();
                        JSONObject body = JSONObject.parseObject(bodyStr);
                        assert body.getJSONObject("data") != null;
                        String status = body.getJSONObject("data").getString("status");
                        assert !StringUtils.isEmpty(status);
                        TaskPlatformStatusEnum statusEnum = TaskPlatformStatusEnum.fromValue(status);
                        switch (statusEnum) {
                            case INIT:
                            case RUNNING: {
                                TaskCenter.resultMap.get(taskUuid).get("running").add(instance);
                                break;
                            }
                            case SUCCESS: {
                                instanceList.remove(instance);
                                TaskCenter.resultMap.get(taskUuid).get("running").remove(instance);
                                TaskCenter.resultMap.get(taskUuid).get("success").add(instance);
                                break;
                            }
                            case EXCEPTION: {
                                instanceList.remove(instance);
                                TaskCenter.resultMap.get(taskUuid).get("running").remove(instance);
                                TaskCenter.resultMap.get(taskUuid).get("error").add(instance);
                                break;
                            }
                            default: {
                                log.warn("action=refresh || task status can not been identify, status:" + statusEnum
                                    + ", id:" + instance);
                                break;
                            }
                        }
                    } else {
                        log.warn("action=refresh|| Send request to taskPlatform failed! message={}",
                            response.message());
                        Long remainTs = TaskCenter.timeoutMap.get(taskUuid);
                        if (remainTs <= 0L) {
                            // 超时停止
                            stopTask(taskUuid, "task timeout!");
                            break; // 直接跳至下个taskUuid。
                        }
                    }
                } catch (IOException e) {
                    log.warn("action=refresh|| Internal error! Send request to taskPlatform failed!", e);
                }
            }
            log.info("action=refresh|| fresh task:{} end!", taskUuid);

            try {
                // 更新剩余时间
                Long remainTs = TaskCenter.timeoutMap.get(taskUuid) - 10000L;
                TaskCenter.timeoutMap.put(taskUuid, remainTs);

                // 后置检查：终态、超时。
                check2UpdateStatus(taskUuid);
                // 判断窗口滑动
                processService.shuffleWindow(taskUuid);
            } catch (Exception e) {
                log.warn("action=refresh||update task status error!", e);
            }

        }

    }

    private void check2UpdateStatus(String taskUuid) {
        List<String> idList = TaskCenter.jobInstanceList.get(taskUuid);
        // 1.进入终态判断
        if (CollectionUtils.isEmpty(idList) && CollectionUtils.isEmpty(TaskCenter.requestWaitQueue.get(taskUuid))) {
            completeTask(taskUuid);
        } else {
            Long remain = TaskCenter.timeoutMap.get(taskUuid);
            if (remain <= 0L) { // 超时停止
                stopTask(taskUuid, "Task timeout!");
            } else {
                updateStatus(taskUuid);
            }
        }
    }

    private void updateStatus(String taskUuid) {
        TaskCheckpointDto checkpoint = TaskCheckpointDto.builder()
            .taskUUID(taskUuid)
            .status("running")
            .data(genCheckpointData(taskUuid))
            .build();
        String url = UrlUtil.buildUrl(aispProperties.getUrl().concat("/updateTaskRecord"));
        try {
            Response response = RequestUtil.postRaw(url, null, JSONObject.toJSONString(checkpoint), null);
            if (response.isSuccessful()) {
                log.info("action=updateStatus || taskUUID:" + taskUuid);
            } else {
                log.warn("action=updateStatus || update task failed! message:{}", response.message());
            }
        } catch (IOException e) {
            log.error("action=updateStatus || internal error, complete task failed!", e);
        }
    }

    private JSONObject genCheckpointData(String taskUuid) {
        JSONObject data = new JSONObject();
        data.put("jobResult", TaskCenter.resultMap.get(taskUuid));
        data.put("startFailed", TaskCenter.failCreateMap.get(taskUuid));
        return data;
    }

    private void stopTask(String taskUuid, String message) {
        Map<String, Set<String>> map = TaskCenter.resultMap.get(taskUuid);
        TaskCheckpointDto result = TaskCheckpointDto.builder()
            .taskUUID(taskUuid)
            .data(genCheckpointData(taskUuid))
            .status("failed")
            .message(message)
            .build();
        String url = UrlUtil.buildUrl(aispProperties.getUrl().concat("/updateTaskRecord"));
        log.info("action=cleanTask || taskUUID: {}", taskUuid);
        TaskCenter.cleanTask(taskUuid);
        try {
            Response response = RequestUtil.postRaw(url, null, JSONObject.toJSONString(result), null);
            if (response.isSuccessful()) {
                log.info("action=stopTask || taskUUID:" + taskUuid + " stoped!");
            } else {
                log.warn("action=stopTask || stop task failed! message:{}", response.message());
            }
        } catch (IOException e) {
            log.error("action=stopTask || internal error, stop job failed!", e);
        }
    }

    private void completeTask(String taskUuid) {
        Map<String, Set<String>> map = TaskCenter.resultMap.get(taskUuid);
        List<Object> failedReq = TaskCenter.failCreateMap.get(taskUuid);
        TaskCheckpointDtoBuilder builder = TaskCheckpointDto.builder()
            .taskUUID(taskUuid)
            .data(genCheckpointData(taskUuid));
        if (CollectionUtils.isEmpty(map.get("error")) && CollectionUtils.isEmpty(failedReq)) {
            builder.status("success");
        } else {
            builder.status("failed")
                .code("PartialFailed")
                .message("some task of taskplatform failed!");
        }
        String url = UrlUtil.buildUrl(aispProperties.getUrl().concat("/updateTaskRecord"));
        log.info("action=cleanTask || taskUUID: {}", taskUuid);
        TaskCenter.cleanTask(taskUuid);
        try {
            Response response = RequestUtil.postRaw(url, null, JSONObject.toJSONString(builder.build()), null);
            if (response.isSuccessful()) {
                log.info("action=completeTask || taskUUID:" + taskUuid + " Success!");
            } else {
                log.warn("action=completeTask || complete task failed! message:{}", response.message());
            }
        } catch (IOException e) {
            log.error("action=completeTask || internal error, complete task failed!", e);
        }
    }
}
