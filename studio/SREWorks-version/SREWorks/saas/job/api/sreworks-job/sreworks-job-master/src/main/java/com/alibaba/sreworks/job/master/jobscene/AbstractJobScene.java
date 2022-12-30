package com.alibaba.sreworks.job.master.jobscene;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.springframework.stereotype.Service;

@Data
@Service
public abstract class AbstractJobScene<T extends AbstractJobSceneConf> {

    public String type;

    public abstract Class<T> getConfClass();

    public void create(Long id, JSONObject conf) throws Exception {
        create(id, JSONObject.toJavaObject(conf, getConfClass()));
    }

    public void modify(Long id, JSONObject conf) throws Exception {
        modify(id, JSONObject.toJavaObject(conf, getConfClass()));
    }

    public abstract T getConf(Long id) throws Exception;

    public abstract void create(Long id, T conf) throws Exception;

    public abstract void delete(Long id) throws Exception;

    public abstract void modify(Long id, T conf) throws Exception;

}
