package com.alibaba.sreworks.job.master.jobtrigger;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Data
@Service
public abstract class AbstractJobTrigger<T extends AbstractJobTriggerConf> {

    public String type;

    public abstract Class<T> getConfClass();

    public void create(Long id, JSONObject conf) throws Exception {
        create(id, JSONObject.toJavaObject(conf, getConfClass()));
    }

    public void modify(Long id, JSONObject conf) throws Exception {
        modify(id, JSONObject.toJavaObject(conf, getConfClass()));
    }

    public abstract void create(Long id, T conf) throws Exception;

    public abstract void delete(Long id) throws Exception;

    public abstract void modify(Long id, T conf) throws Exception;

    public abstract T getConf(Long id) throws Exception;

    public abstract Map<Long, T> getConfBatch(Set<Long> ids) throws Exception;

    public abstract Boolean getState(Long id) throws Exception;

    public abstract Map<Long, Boolean> getStateBatch(Set<Long> ids) throws Exception;

    public abstract void toggleState(Long id, Boolean state) throws Exception;
}
