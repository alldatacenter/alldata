package com.alibaba.sreworks.job.worker.taskscene.incident;

import com.alibaba.sreworks.job.worker.taskscene.AbstractTaskSceneConf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class IncidentTaskSceneConf extends AbstractTaskSceneConf {

    /**
     * 产出关联异常定义ID
     */
    private Long modelId;
}
