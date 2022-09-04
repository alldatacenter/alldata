package com.alibaba.sreworks.job.worker.taskscene.collect;

import com.alibaba.sreworks.job.worker.taskscene.AbstractTaskSceneConf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class CollectTaskSceneConf extends AbstractTaskSceneConf {

    /**
     * 关联指标ID
     */
    private Integer relatedMetricId;

    /**
     * 推送消息队列
     */
    private String isPushQueue;

    private String syncDw;

    private String type;

    private Long id;

}
