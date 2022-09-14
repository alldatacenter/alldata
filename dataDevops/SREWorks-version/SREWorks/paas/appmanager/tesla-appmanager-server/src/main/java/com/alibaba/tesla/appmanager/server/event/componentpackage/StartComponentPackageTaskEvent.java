package com.alibaba.tesla.appmanager.server.event.componentpackage;

import com.alibaba.tesla.appmanager.common.enums.ComponentPackageTaskEventEnum;

/**
 * ComponentPackage 任务状态事件 - START
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class StartComponentPackageTaskEvent extends ComponentPackageTaskEvent {

    public StartComponentPackageTaskEvent(Object source, Long componentPackageTaskId) {
        super(source, componentPackageTaskId);
        this.CURRENT_EVENT = ComponentPackageTaskEventEnum.START;
    }
}
