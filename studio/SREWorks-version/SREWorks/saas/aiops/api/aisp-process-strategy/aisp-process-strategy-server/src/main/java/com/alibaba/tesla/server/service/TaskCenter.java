package com.alibaba.tesla.server.service;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.server.controller.param.ProcessParam;

/**
 * @ClassName: TaskCenter
 * @Author: dyj
 * @DATE: 2022-02-28
 * @Description:
 **/
public class TaskCenter {
    /**
     * 结构为：
     * ｛
     * "taskUuid": {
     *     "running": ["id","id"],
     *     "success": ["id","id"],
     *     "error": ["id","id"]
     *  }
     * ｝
     */
    public static Map<String, Map<String, Set<String>>> resultMap = new HashMap<>();

    /**
     * 启动作业失败列表
     */
    public static Map<String, List<Object>> failCreateMap = new HashMap<>();

    /**
     * 请求参数队列
     */
    public static Map<String, Queue<Object>> requestWaitQueue = new HashMap<>();

    /**
     * 对应作业
     */
    public static Map<String, String> taskJobRelMap = new HashMap<>();

    /**
     * 运行的作业实例列表
     * {
     *     "taskUuid": ["id", "id"]
     * }
     */
    public static Map<String, List<String>> jobInstanceList = new HashMap<>();

    /**
     * 剩余超时时间
     */
    public static Map<String, Long> timeoutMap = new HashMap<>();

    /**
     * 步长
     */
    public static Map<String, Integer> taskStepMap = new HashMap<>();

    public static void initTask(ProcessParam param){
        String taskUUID = param.getTaskUUID();
        Map<String, Set<String>> initRes = new HashMap<>();
        initRes.put("running", new HashSet<>());
        initRes.put("success", new HashSet<>());
        initRes.put("error", new HashSet<>());
        resultMap.put(taskUUID, initRes);
        failCreateMap.put(taskUUID, new LinkedList<>());
        requestWaitQueue.put(taskUUID, new ArrayDeque<>(param.getReqList()));
        jobInstanceList.put(taskUUID, new LinkedList<>());
        timeoutMap.put(taskUUID, param.getTimeout());
        taskJobRelMap.put(taskUUID, param.getJobName());
        taskStepMap.put(taskUUID, param.getStep());
    }

    public static void cleanTask(String taskUuid){
        resultMap.remove(taskUuid);
        requestWaitQueue.remove(taskUuid);
        jobInstanceList.remove(taskUuid);
        taskJobRelMap.remove(taskUuid);
        timeoutMap.remove(taskUuid);
        taskStepMap.remove(taskUuid);
        failCreateMap.remove(taskUuid);
    }

    public static JSONObject toJson(){
        JSONObject o = new JSONObject();
        o.put("resultMap", resultMap);
        o.put("failCreateMap", failCreateMap);
        o.put("requestQueue", requestWaitQueue);
        o.put("taskJobRelMap", taskJobRelMap);
        o.put("jobInstanceList", jobInstanceList);
        o.put("timeoutMap", timeoutMap);
        o.put("taskStepMap", taskStepMap);
        return o;

    }
}
