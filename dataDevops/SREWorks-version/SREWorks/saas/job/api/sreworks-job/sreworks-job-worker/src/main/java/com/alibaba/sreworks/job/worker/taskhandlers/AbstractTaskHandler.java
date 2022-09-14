package com.alibaba.sreworks.job.worker.taskhandlers;

import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstanceWithBlobs;
import lombok.Data;

@Data
public abstract class AbstractTaskHandler {

    public static String execType;

    private ElasticTaskInstanceWithBlobs taskInstance;

    private boolean cancelByTimeout = false;

    private boolean cancelByStop = false;

    public abstract void execute() throws Exception;

    public void destroy() {}

}
