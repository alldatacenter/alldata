package com.alibaba.tesla.appmanager.server.dynamicscript.handler;

import com.alibaba.tesla.appmanager.dynamicscript.core.GroovyHandler;

/**
 * 组件 Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface ComponentHandler extends GroovyHandler {

    /**
     * 获取 `COMPONENT_BUILD` 类型下的映射名称
     *
     * @return 示例：`MicroserviceDefault`
     */
    String buildScriptName();

    /**
     * 获取 `COMPONENT_DEPLOY` 类型下的映射名称
     *
     * @return 示例：`JobDefault` / `HelmDefault`
     */
    String deployScriptName();

    /**
     * 获取 `COMPONENT_DESTROY` 类型下的映射名称
     *
     * @return 示例：`HelmDefault`
     */
    String destroyName();

    /**
     * 获取状态监听类型
     *
     * @return 返回 `KUBERNETES_INFORMER` 或 `CRON`
     */
    String watchKind();

    /**
     * 如果 `watchKind` 返回 `KUBERNETES_INFORMER`，则对应 `COMPONENT_WATCH_KUBERNETES_INFORMER` 类型下的映射名称
     * <p>
     * 如果 `watchKind` 返回 `CRON`，则对应 `COMPONENT_WATCH_CRON` 类型下的映射名称
     *
     * @return 返回对应类型下的映射名称
     */
    String watchScriptName();
}
