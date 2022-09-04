package dynamicscripts

import com.alibaba.fastjson.JSONObject
import com.alibaba.tesla.appmanager.api.provider.ClusterProvider
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode
import com.alibaba.tesla.appmanager.common.exception.AppException
import com.alibaba.tesla.appmanager.common.util.CommandUtil
import com.alibaba.tesla.appmanager.domain.req.destroy.DestroyComponentInstanceReq
import com.alibaba.tesla.appmanager.server.dynamicscript.handler.ComponentDestroyHandler
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

import java.nio.charset.StandardCharsets
import java.nio.file.Files

/**
 * Helm 组件销毁 Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
class HelmComponentDestroyHandler implements ComponentDestroyHandler {

    private static final Logger log = LoggerFactory.getLogger(HelmComponentDestroyHandler.class)

    /**
     * Handler 元信息
     */
    public static final String KIND = DynamicScriptKindEnum.COMPONENT_DESTROY.toString()
    public static final String NAME = "HelmDefault"
    public static final Integer REVISION = 1

    @Autowired
    private ClusterProvider clusterProvider

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
        def name = getMetaName(appId, componentName, stageId)

        // 获取集群信息
        def cluster = request.getClusterId()
        def clusterConfig = clusterProvider.get(cluster).getClusterConfig()
        def token = clusterConfig.getString("oauthToken")
        def apiserver = clusterConfig.getString("masterUrl")
        def kube = clusterConfig.getString("kube")

        // 执行 helm uninstall
        def command
        def kubeFile = null
        if (StringUtils.isNotEmpty(token) && StringUtils.isNotEmpty(apiserver)) {
            command = String.format(
                    "/app/helm uninstall %s --kube-token=%s --kube-apiserver=%s " +
                            "--kube-ca-file=/run/secrets/kubernetes.io/serviceaccount/ca.crt -n %s",
                    name, token, apiserver, namespace
            )
        } else if (StringUtils.isNotEmpty(kube)) {
            kubeFile = Files.createTempFile("kubeconfig", ".json").toFile();
            FileUtils.writeStringToFile(kubeFile, kube, StandardCharsets.UTF_8)
            command = String.format(
                    "/app/helm uninstall %s --kubeconfig %s -n %s", name, kubeFile.getAbsolutePath(), namespace)
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find cluster authorization info|clusterId=%s", cluster))
        }
        try {
            CommandUtil.runLocalCommand(command)
            log.info("execute helm uninstall succeed|command={}|name={}|request={}",
                    command, name, JSONObject.toJSONString(request))
        } catch (Exception e) {
            log.warn("execute helm uninstall failed|command={}|name={}|request={}|exception={}",
                    command, name, JSONObject.toJSONString(request), ExceptionUtils.getStackTrace(e))
        }
        if (kubeFile != null) {
            def path = kubeFile.getAbsoluteFile()
            if (!kubeFile.delete()) {
                log.error("cannot delete temp kubeconfig file, please check|path={}", path)
            }
        }
    }

    private static String getMetaName(String appId, String componentName, String stageId) {
        def name
        if (StringUtils.isEmpty(stageId)) {
            name = String.format("%s-%s", appId, componentName)
        } else {
            name = String.format("%s-%s-%s", stageId, appId, componentName)
        }
        return name
    }
}
