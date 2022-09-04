package com.alibaba.sreworks.health.operator;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.common.properties.ApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * 作业平台工具类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/29 16:50
 */
@Service
@Slf4j
public class JobMasterOperator extends HttpOperator{

    @Autowired
    ApplicationProperties properties;

    private String getJobByNamePath = "/job/getByName";
    private String deleteJobWithTaskPath = "/job/deleteWithTask";
    private String toggleCronJobStatePath = "/job/toggleCronJobState";
    private String importJobWithTaskPath = "/imEx/im";

    public JSONObject getJobByName(String jobName) throws Exception {
        String url = properties.getJobMasterProtocol() + "://" + properties.getJobMasterHost() + ":" + properties.getJobMasterPort() + getJobByNamePath;
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        urlBuilder.addQueryParameter("jobName", jobName);

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();
        JSONObject ret = doRequest(request);
        if (CollectionUtils.isEmpty(ret)) {
            return null;
        } else {
            return ret.getJSONObject("data");
        }
    }

    public void deleteJob(Long id) throws Exception {
        String url = properties.getJobMasterProtocol() + "://" + properties.getJobMasterHost() + ":" + properties.getJobMasterPort() + deleteJobWithTaskPath;
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        urlBuilder.addQueryParameter("id", String.valueOf(id));

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .delete()
                .build();
        doRequest(request);
    }

    public void toggleCronJob(Long id, boolean state) throws Exception {
        String url = properties.getJobMasterProtocol() + "://" + properties.getJobMasterHost() + ":" + properties.getJobMasterPort() + toggleCronJobStatePath;
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        urlBuilder.addQueryParameter("id", String.valueOf(id));
        JSONObject body = new JSONObject();
        body.put("state", state);
        RequestBody requestBody = RequestBody.create(MediaType.get("application/json; charset=utf-8"), JSONObject.toJSONString(body));

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .post(requestBody)
                .build();
        doRequest(request);
    }

    public void createJob(List<JSONObject> jobs) throws Exception {
        String url = properties.getJobMasterProtocol() + "://" + properties.getJobMasterHost() + ":" + properties.getJobMasterPort() + importJobWithTaskPath;
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();

        RequestBody requestBody = RequestBody.create(MediaType.get("application/json; charset=utf-8"), JSONObject.toJSONString(jobs));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .post(requestBody)
                .build();
        JSONObject ret = doRequest(request);
    }
}
