package com.alibaba.sreworks.job.master.event;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public abstract class AbstractJobEventConsumer {

    public JobEventConf conf;

    public abstract List<JSONObject> poll();

    public abstract void close();

}
