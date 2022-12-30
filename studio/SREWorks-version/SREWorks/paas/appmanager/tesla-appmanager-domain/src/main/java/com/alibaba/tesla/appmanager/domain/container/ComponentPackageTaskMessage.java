package com.alibaba.tesla.appmanager.domain.container;

import com.alibaba.tesla.appmanager.common.enums.PackageTaskEnum;
import com.alibaba.tesla.appmanager.domain.req.apppackage.ComponentBinder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 组件包任务消息对象
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentPackageTaskMessage {

    private Long appPackageTaskId;

    private Long componentPackageTaskId;

    private String appId;

    private String namespaceId;

    private String stageId;

    private String operator;

    private ComponentBinder component;

    private PackageTaskEnum packageTaskEnum;
}
