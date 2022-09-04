package dynamicscripts

import com.alibaba.tesla.appmanager.server.dynamicscript.handler.ComponentHandler

/**
 * Internal Addon V2 Productops 组件
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
class InternalAddonV2ProductopsComponentHandler implements ComponentHandler {

    /**
     * Handler 元信息
     */
    public static final String KIND = "COMPONENT"
    public static final String NAME = "INTERNAL_ADDON_productopsv2"
    public static final Integer REVISION = 0

    /**
     * 获取 `COMPONENT_BUILD` 类型下的映射名称
     *
     * @return 示例：`AbmChartDefault`
     */
    @Override
    String buildScriptName() {
        return ""
    }

    /**
     * 获取 `COMPONENT_DEPLOY` 类型下的映射名称
     *
     * @return 示例：`JobDefault` / `HelmDefault`
     */
    @Override
    String deployScriptName() {
        return ""
    }

    /**
     * 获取 `COMPONENT_DESTROY` 类型下的映射名称
     *
     * @return 示例：`HelmDefault`
     */
    @Override
    String destroyName() {
        return "InternalAddonV2ProductopsDefault"
    }

    /**
     * 获取状态监听类型
     *
     * @return 返回 `KUBERNETES_INFORMER` 或 `CRON`
     */
    @Override
    String watchKind() {
        return ""
    }

    /**
     * 如果 `watchKind` 返回 `KUBERNETES_INFORMER`，则对应 `COMPONENT_WATCH_KUBERNETES_INFORMER` 类型下的映射名称
     * <p>
     * 如果 `watchKind` 返回 `CRON`，则对应 `COMPONENT_WATCH_CRON` 类型下的映射名称
     *
     * @return 返回对应类型下的映射名称
     */
    @Override
    String watchScriptName() {
        return ""
    }
}
