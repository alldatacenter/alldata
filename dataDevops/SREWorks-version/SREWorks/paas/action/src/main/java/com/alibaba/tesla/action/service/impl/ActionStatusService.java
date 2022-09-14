package com.alibaba.tesla.action.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.action.constant.TeslaAuth;
import com.alibaba.tesla.action.utils.TeslaOKHttpClient;
import com.alibaba.tesla.action.dao.ActionDAO;
import com.alibaba.tesla.action.dao.ActionDO;
import com.alibaba.tesla.action.dao.ActionExample;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@Service
public class ActionStatusService implements InitializingBean {

    @Autowired
    ActionDAO actionDAO;

    @Autowired
    ApiService apiService;

    // action status
    enum Status {
        INIT,
        RUNNING,
        STOPPED,
        APPROVING,
        REJECTED,
        APPROVED,
        SUCCESS,
        FAILED,
        CANCELED,
        UNKNOWN,
        EXCEPTION,
    }


    String[] TempStatuses = {Status.UNKNOWN.toString(), Status.INIT.toString(), Status.RUNNING.toString(), Status.STOPPED.toString(), Status.FAILED.toString()};
    List<String> tempStatus = Arrays.asList(TempStatuses);

    @Override
    public void afterPropertiesSet() {
        ScheduledExecutorService threadExecutor = new ScheduledThreadPoolExecutor(
            2, new ActionThreadFactory());
        threadExecutor.scheduleAtFixedRate(this::updateStatus, 0, 30, TimeUnit.SECONDS);
    }

    private class ActionThreadFactory implements ThreadFactory {
        private final String workerName = "ActionThreadFactory worker --";
        private final AtomicInteger threadId = new AtomicInteger(1);

        @Override
        public Thread newThread(@NotNull Runnable r) {
            String threadName = this.workerName + this.threadId.getAndIncrement();
            Thread thread = new Thread(r, threadName);
            log.info(String.format("new thread name is: %s", thread.getName()));
            return thread;
        }
    }


    // task instance status convert to action status
    public String getTaskStatus(String state) {

        Map<String, Status> taskstatusMap = new HashMap<>();
        taskstatusMap.put("UNSTART", Status.INIT);
        taskstatusMap.put("RUNNING", Status.RUNNING);
        taskstatusMap.put("SUCCESS", Status.SUCCESS);
        taskstatusMap.put("FAILED", Status.FAILED);
        taskstatusMap.put("EXCEPTION", Status.FAILED);
        taskstatusMap.put("CANCELED", Status.CANCELED);
        taskstatusMap.put("WAITING", Status.RUNNING);
        taskstatusMap.put("STOPPED", Status.STOPPED);
        taskstatusMap.put("TERMINATED", Status.SUCCESS);
        Status status = taskstatusMap.getOrDefault(state, Status.RUNNING);
        return status.toString();
    }


    // flow instance status convert to action status
    public String getFlowStatus(Integer state) {
        Map<Integer, Status> flowstatusMap = new HashMap<>();
        flowstatusMap.put(0, Status.RUNNING);
        flowstatusMap.put(1, Status.SUCCESS);
        flowstatusMap.put(2, Status.STOPPED);
        flowstatusMap.put(3, Status.FAILED);
        flowstatusMap.put(4, Status.CANCELED);
        Status status = flowstatusMap.getOrDefault(state, Status.RUNNING);
        return status.toString();
    }

    public void updateStatus() {

        ActionExample actionExample = new ActionExample();
        ActionExample.Criteria criteria = actionExample.createCriteria();
        criteria.andStatusIn(tempStatus);
        List<ActionDO> actionDOS = actionDAO.selectByParamWithBLOBs(actionExample);
        log.info("[ACTION_UPDATE] get {} action to update", actionDOS.size());
        for (ActionDO actionDo : actionDOS) {
            String type = actionDo.getActionType();
            if (type.equals("API_ASYNC")) {
                updateAsyncApiData(actionDo);
            }
        }
    }

    @Async
    public int updateAsyncApiData(ActionDO actionDO) {
        try {
            JSONObject execData = JSONObject.parseObject(actionDO.getExecData());
            String actionType = execData.get("actionInstanceType").toString();
            log.error("wrong action type: {}", actionType);
            return 1;
        } catch (Exception e) {
            log.error("[ActionStatus] get async api status failed");
            return 1;
        }
    }

    public void flowTest(String id) {
        String endpoint = "http://process.tesla.alibaba-inc.com";
        JSONObject result = call(endpoint + String.format("/instance/detail/%s",
            id), HttpMethod.GET, "", "prod");
        JSONObject flowData = result.getJSONObject("data");
        log.info("---flow data: {}", flowData);
        String status = getFlowStatus((Integer) flowData.get("state"));
        log.info("---flow status; {}", status);
        return;
    }


    public JSONObject call(String endpoint, HttpMethod method, Object params, String env, Cookie[] cookies, Map<String, String> headers) {
        log.info("begin to call endpoint {}", endpoint);
        headers.put("x-auth-app", TeslaAuth.DEFAULT_AUTH_APP);
        headers.put("x-auth-key", TeslaAuth.DEFAULT_AUTH_KEY);
        headers.put("x-auth-user", "huibang.lhb");
        headers.put("x-empid", "169715");
        headers.put("x-auth-passwd", "test");
        if (!headers.containsKey("x-env")) {
            headers.put("x-env", env);
        }
        log.info("[Action API Header] header: {}", headers);

        JSONObject jsonObject = apiService.call(method, endpoint, headers, params, cookies);

        JSONObject data = jsonObject.getJSONObject("data");

        log.info("api: {}, return data {}, jsonobject: {}", endpoint, data, jsonObject);
        if (jsonObject.get("code") != null && Integer.parseInt(jsonObject.getString("code")) >= 400) {
            return jsonObject;
        }
        return data;
    }

    public JSONObject call(String endpoint, HttpMethod method, Object params, String env, Map<String, String> headers) {
        log.info("begin to call endpoint {}", endpoint);
        headers.put("x-auth-app", TeslaAuth.DEFAULT_AUTH_APP);
        headers.put("x-auth-key", TeslaAuth.DEFAULT_AUTH_KEY);
        if (!headers.containsKey("x-auth-user")) {
            headers.put("x-auth-user", "huibang.lhb");
        }
        if (!headers.containsKey("x-empid")) {
            headers.put("x-empid", "169715");
        }
        headers.put("x-auth-passwd", "test");
        headers.put("x-env", env);
        JSONObject jsonObject = apiService.call(method, endpoint, headers, params, null);

        JSONObject data = jsonObject.getJSONObject("data");

        log.info("api: {}, return data {}, jsonobject: {}", endpoint, data, jsonObject);
        if (jsonObject.get("code") != null && Integer.parseInt(jsonObject.getString("code")) >= 400) {
            return jsonObject;
        }

        return data;
    }

    public JSONObject call(String endpoint, HttpMethod method, Object params, String env) {
        log.info("begin to call endpoint {}", endpoint);
        Map<String, Object> headers = new HashMap<>();
        headers.put("x-auth-app", TeslaAuth.DEFAULT_AUTH_APP);
        headers.put("x-auth-key", TeslaAuth.DEFAULT_AUTH_KEY);
        headers.put("x-auth-user", "huibang.lhb");
        headers.put("x-empid", "169715");
        headers.put("x-auth-passwd", "test");
        headers.put("x-env", env);

        JSONObject jsonObject = TeslaOKHttpClient.requestWithHeader(
            method, endpoint, params, headers);

        JSONObject data = jsonObject.getJSONObject("data");

        if (jsonObject.get("code") != null && Integer.parseInt(jsonObject.getString("code")) >= 400) {
            throw new RuntimeException(String.format("exec action failed: %s", jsonObject.toJSONString()));
        }

        return data;
    }


}
