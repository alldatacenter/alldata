package com.alibaba.sreworks.job.master.event;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.jobtrigger.AbstractJobTriggerConf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class JobEventConf extends AbstractJobTriggerConf {

    /**
     * 事件来源 异常/告警/故障
     */
    private EventSource source;

    /**
     * 事件源类型
     */
    private JobEventType type;

    /**
     * 事件源配置
     */
    private JSONObject config;

//    private String server;
//
//    private List<String> topics;
//
//    private String groupId;

}
