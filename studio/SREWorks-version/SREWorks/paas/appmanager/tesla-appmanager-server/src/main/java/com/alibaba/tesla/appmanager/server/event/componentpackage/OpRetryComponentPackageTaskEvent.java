package com.alibaba.tesla.appmanager.server.event.componentpackage;

import com.alibaba.tesla.appmanager.common.enums.ComponentPackageTaskEventEnum;

/**
 * ComponentPackage 任务状态事件 - OP_RETRY
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class OpRetryComponentPackageTaskEvent extends ComponentPackageTaskEvent {

    public OpRetryComponentPackageTaskEvent(Object source, Long componentPackageTaskId) {
        super(source, componentPackageTaskId);
        this.CURRENT_EVENT = ComponentPackageTaskEventEnum.OP_RETRY;
    }
}
