package com.alibaba.sreworks.job.worker.taskscene.alert;

import com.alibaba.sreworks.job.worker.taskscene.AbstractTaskSceneConf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class AlertTaskSceneConf extends AbstractTaskSceneConf {

    private Long modelId;

}
