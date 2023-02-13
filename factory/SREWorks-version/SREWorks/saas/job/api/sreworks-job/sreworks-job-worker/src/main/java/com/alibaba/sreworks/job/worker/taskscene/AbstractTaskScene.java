package com.alibaba.sreworks.job.worker.taskscene;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstance;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
@Service
public abstract class AbstractTaskScene<T extends AbstractTaskSceneConf> {

    public final static int SECOND_TS_LENGTH = 10;

    public final static int MILLISECOND_TS_LENGTH = 13;

    public String type;

    public abstract Class<T> getConfClass();

    public T getConf(ElasticTaskInstance taskInstance) {
        return JSONObject.parseObject(taskInstance.getSceneConf(), getConfClass());
    }

    public void toInit() {}

    public void toRunning() {}

    public void toWaitRetry() {}

    public void toException() {}

    public void toSuccess(String taskInstanceId) throws Exception {}

    public void toTimeout() {}

    public void toCancelled() {}

    public void toStopped() {}

    public String getStdout(String stdout) {

        if (stdout.contains("<result>")) {
            stdout = stdout.split("<result>")[1];
        }
        if (stdout.contains("</result>")) {
            stdout = stdout.split("</result>")[0];
        }

        return stdout;
    }

    protected List<JSONObject> parseOutputToList(String stdout) {
        List<JSONObject> results = new ArrayList<>();
        try {
            results = JSONObject.parseArray(stdout, JSONObject.class);
        } catch (Exception ex) {
            results.add(JSONObject.parseObject(stdout));
        }
        return results;
    }

    protected String parseOutputToArrayString(String stdout) {
        return JSONObject.toJSONString(parseOutputToList(stdout));
    }
}
