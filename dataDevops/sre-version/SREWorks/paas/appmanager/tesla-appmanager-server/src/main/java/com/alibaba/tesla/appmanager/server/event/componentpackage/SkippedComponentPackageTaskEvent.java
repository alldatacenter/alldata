package com.alibaba.tesla.appmanager.server.event.componentpackage;

import com.alibaba.tesla.appmanager.common.enums.ComponentPackageTaskEventEnum;

/**
 * ComponentPackage 任务状态事件 - SKIPPED
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class SkippedComponentPackageTaskEvent extends ComponentPackageTaskEvent {

    public SkippedComponentPackageTaskEvent(Object source, Long componentPackageTaskId) {
        super(source, componentPackageTaskId);
        this.CURRENT_EVENT = ComponentPackageTaskEventEnum.SKIPPED;
    }
}
