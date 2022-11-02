package com.alibaba.tesla.server.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.server.common.properties.TaskPlatformProperties;
import com.alibaba.tesla.server.common.util.RequestUtil;
import com.alibaba.tesla.server.common.util.UrlUtil;
import com.alibaba.tesla.server.controller.param.ProcessParam;
import com.alibaba.tesla.server.service.ProcessService;
import com.alibaba.tesla.server.service.TaskCenter;

import io.micrometer.core.instrument.util.IOUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * @ClassName: ProcessServiceImpl
 * @Author: dyj
 * @DATE: 2022-02-28
 * @Description:
 **/
@Slf4j
@Service
public class ProcessServiceImpl implements ProcessService {
    @Autowired
    private TaskPlatformProperties taskPlatformProperties;
    /**
     * @param param
     * @return
     */
    @Override
    public boolean process(ProcessParam param) {
        TaskCenter.initTask(param);
        shuffleWindow(param.getTaskUUID());
        return true;
    }

    @Override
    public List<String> start(String taskUuid, String jobName, List<Object> windowList) {
        List<String> instanceList = new LinkedList<>();
        for (Object o : windowList) {
            String url = UrlUtil.buildUrl(taskPlatformProperties.getSubmitUrl());
            JSONObject param = new JSONObject();
            param.put("jobName", jobName);
            try {
                Response response = RequestUtil.postRaw(url, param, JSONObject.toJSONString(o), null);
                if (response.isSuccessful()){
                    assert response.body()!=null;
                    JSONObject body = JSONObject.parseObject(response.body().string());
                    assert body.containsKey("data");
                    assert body.getJSONObject("data").containsKey("id");
                    instanceList.add(body.getJSONObject("data").getString("id"));
                } else {
                    log.warn("action=start || start job failed! message:{}", response.message());
                    TaskCenter.failCreateMap.get(taskUuid).add(o);
                }
            } catch (IOException e) {
                log.error("action=start || internal error, start job failed!", e);
                TaskCenter.failCreateMap.get(taskUuid).add(o);
            }
        }

        return instanceList;
    }

    @Override
    public void shuffleWindow(String taskUuid) {
        List<String> idList = TaskCenter.jobInstanceList.get(taskUuid);
        Queue<Object> queue = TaskCenter.requestWaitQueue.get(taskUuid);
        Integer step = TaskCenter.taskStepMap.get(taskUuid);
        String jobName = TaskCenter.taskJobRelMap.get(taskUuid);
        if (CollectionUtils.isEmpty(idList) && !CollectionUtils.isEmpty(queue)){
            List<Object> params = new LinkedList<>();
            int size = queue.size();
            for (int i=0; i<size && i<step; i++){
                params.add(queue.poll());
            }
            List<String> instanceList = start(taskUuid, jobName, params);
            TaskCenter.jobInstanceList.put(taskUuid, instanceList);
        }
    }

    @Override
    public String doc() throws IOException {
        String s = loadClassPathFile("README.md");
        return JSONObject.toJSONString(s);
    }

    private String loadClassPathFile(String filePath) throws IOException {
        Resource config = new ClassPathResource(filePath);
        InputStream inputStream = config.getInputStream();
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }
}
