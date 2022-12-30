package com.alibaba.tesla.appmanager.server.event.componentpackage;

import com.alibaba.tesla.appmanager.common.enums.PackageTaskEnum;
import com.alibaba.tesla.appmanager.domain.req.apppackage.ComponentBinder;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 组件包任务启动 Event
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class ComponentPackageTaskStartEvent extends ApplicationEvent {

    @Getter
    private final Long appPackageTaskId;

    @Getter
    private final Long componentPackageTaskId;

    @Getter
    private final String appId;

    @Getter
    private final String namespaceId;

    @Getter
    private final String stageId;

    @Getter
    private final String operator;

    @Getter
    private final ComponentBinder component;

    @Getter
    private final PackageTaskEnum packageTaskEnum;

    public ComponentPackageTaskStartEvent(
            Object source, Long appPackageTaskId, Long componentPackageTaskId, String appId, String namespaceId,
            String stageId, String operator, ComponentBinder component, PackageTaskEnum packageTaskEnum) {
        super(source);
        this.appPackageTaskId = appPackageTaskId;
        this.componentPackageTaskId = componentPackageTaskId;
        this.appId = appId;
        this.namespaceId = namespaceId;
        this.stageId = stageId;
        this.operator = operator;
        this.component = component;
        this.packageTaskEnum = packageTaskEnum;
    }
}
