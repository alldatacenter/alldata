package com.alibaba.sreworks.job.master.jobschedule;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Data
@Slf4j
@Service
public abstract class AbstractJobSchedule<T extends AbstractJobScheduleConf> {

    public String type;

    public abstract Class<T> getConfClass();

    public void create(Long id, JSONObject conf) throws Exception {
        create(id, JSONObject.toJavaObject(conf, getConfClass()));
    }

    public void modify(Long id, JSONObject conf) throws Exception {
        modify(id, JSONObject.toJavaObject(conf, getConfClass()));
    }

    public abstract JSONObject getConf(Long id) throws Exception;

    public abstract void create(Long id, T conf) throws Exception;

    public abstract void delete(Long id) throws Exception;

    public abstract void modify(Long id, T conf) throws Exception;

    public abstract Long start(Long id, JSONObject varConf) throws Exception;

    public abstract void stop(Long instanceId) throws Exception;

    public abstract Object get(Long instanceId) throws Exception;

}
