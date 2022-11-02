package dynamicscripts

import com.alibaba.fastjson.JSONObject
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum
import com.alibaba.tesla.appmanager.common.util.RequestUtil
import com.alibaba.tesla.appmanager.domain.req.destroy.DestroyComponentInstanceReq
import com.alibaba.tesla.appmanager.server.dynamicscript.handler.ComponentDestroyHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
/**
 * Internal Addon Productops V2 组件销毁 Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
class InternalAddonV2ProductopsComponentDestroyHandler implements ComponentDestroyHandler {

    private static final Logger log = LoggerFactory.getLogger(InternalAddonV2ProductopsComponentDestroyHandler.class)

    /**
     * Handler 元信息
     */
    public static final String KIND = DynamicScriptKindEnum.COMPONENT_DESTROY.toString()
    public static final String NAME = "InternalAddonV2ProductopsDefault"
    public static final Integer REVISION = 3

    /**
     * 销毁组件实例
     *
     * @param request 销毁请求
     */
    @Override
    void destroy(DestroyComponentInstanceReq request) {
        def appId = request.getAppId()
        def componentName = request.getComponentName()
        def namespace = request.getNamespaceId()
        def stageId = request.getStageId()

        log.info("frontend-service app {} start destroy", appId)

        def targetEndpoint = "prod-flycore-paas-action"

        def removeAppUrl = "http://" + targetEndpoint + "/frontend/apps/" + appId
        def ret = RequestUtil.delete(removeAppUrl, new JSONObject(), new JSONObject())
        log.info("frontend-service remove app {} {}", removeAppUrl, ret)
        def retJson = JSONObject.parseObject(ret)
        if (retJson.getIntValue("code") != 200) {
            throw new Exception("frontend-service remove app error with ret: " + ret)
        }

    }
}
