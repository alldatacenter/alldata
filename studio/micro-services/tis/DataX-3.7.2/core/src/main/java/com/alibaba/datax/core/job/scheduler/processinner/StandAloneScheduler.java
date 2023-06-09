package com.alibaba.datax.core.job.scheduler.processinner;

import com.alibaba.datax.core.job.IJobContainerContext;
import com.alibaba.datax.core.statistics.container.communicator.AbstractContainerCommunicator;

/**
 * Created by hongjiao.hj on 2014/12/22.
 */
public class StandAloneScheduler extends ProcessInnerScheduler {

    public StandAloneScheduler(IJobContainerContext containerContext, AbstractContainerCommunicator containerCommunicator) {
        super(containerContext, containerCommunicator);
    }

    @Override
    protected boolean isJobKilling(Long jobId) {
        return false;
    }

}
