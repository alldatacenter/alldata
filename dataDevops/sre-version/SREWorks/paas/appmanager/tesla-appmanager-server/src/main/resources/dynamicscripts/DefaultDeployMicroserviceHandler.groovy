package dynamicscripts

import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.alibaba.tesla.appmanager.autoconfig.SystemProperties
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant
import com.alibaba.tesla.appmanager.common.enums.DeployComponentStateEnum
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode
import com.alibaba.tesla.appmanager.common.exception.AppException
import com.alibaba.tesla.appmanager.common.util.CommandUtil
import com.alibaba.tesla.appmanager.common.util.ImageUtil
import com.alibaba.tesla.appmanager.common.util.NetworkUtil
import com.alibaba.tesla.appmanager.common.util.ZipUtil
import com.alibaba.tesla.appmanager.domain.req.deploy.GetDeployComponentHandlerReq
import com.alibaba.tesla.appmanager.domain.req.deploy.LaunchDeployComponentHandlerReq
import com.alibaba.tesla.appmanager.domain.res.deploy.GetDeployComponentHandlerRes
import com.alibaba.tesla.appmanager.domain.res.deploy.LaunchDeployComponentHandlerRes
import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema
import com.alibaba.tesla.appmanager.kubernetes.KubernetesClientFactory
import com.alibaba.tesla.appmanager.server.service.deploy.handler.DeployComponentHandler
import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder
import io.fabric8.kubernetes.api.model.ResourceQuotaBuilder
import io.fabric8.kubernetes.api.model.ResourceQuotaSpec
import io.fabric8.kubernetes.api.model.ResourceQuotaSpecBuilder
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.joda.time.DateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

/**
 * 默认构建 Microservice Groovy Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
class DefaultDeployMicroserviceHandler implements DeployComponentHandler {

    private static final Logger log = LoggerFactory.getLogger(DefaultDeployMicroserviceHandler.class)

    private static final String KEY_COMPONENT_PACKAGE_URL = "appmanager_deploy_component_package"

    /**
     * 当前内置 Handler 类型
     */
    public static final String KIND = DynamicScriptKindEnum.DEPLOY_MICROSERVICE_COMPONENT.toString()

    /**
     * 当前内置 Handler 名称
     */
    public static final String NAME = DefaultConstant.DEFAULT_GROOVY_HANDLER

    /**
     * 当前内置 Handler 版本
     */
    public static final Integer REVISION = 36

    /**
     * CRD Context
     */
    private static final CustomResourceDefinitionContext CRD_CONTEXT = new CustomResourceDefinitionContext.Builder()
            .withName("microservices.apps.abm.io")
            .withGroup("apps.abm.io")
            .withVersion("v1")
            .withPlural("microservices")
            .withScope("Namespaced")
            .build()

    @Autowired
    private KubernetesClientFactory kubernetesClientFactory

    @Autowired
    private SystemProperties systemProperties

    /**
     * 部署组件过程
     *
     * @param request 部署请求
     */
    @Override
    LaunchDeployComponentHandlerRes launch(LaunchDeployComponentHandlerReq request) {
        log.info("default deploy launch request|{}", JSONObject.toJSONString(request))
        def packageDir = getPackageDir(request)
        def componentSchema = request.getComponentSchema()

        // 如果存在 namespace autoCreate 参数，那么在目标集群中动态创建该 namespace
        def options = request.getComponentOptions()
        if (CollectionUtils.isNotEmpty(options.getScopes())) {
            List<DeployAppSchema.SpecComponentScope> scopeList = options.getScopes()
            for (DeployAppSchema.SpecComponentScope scope : scopeList) {
                if ("namespace".equalsIgnoreCase(scope.getScopeRef().getKind())) {
                    def namespace = scope.getScopeRef().getName()
                    if (scope.getScopeRef().getSpec() != null) {
                        // auto create
                        def autoCreate = scope.getScopeRef().getSpec().getBooleanValue("autoCreate")
                        if (autoCreate) {
                            def cluster = request.getClusterId()
                            def client = kubernetesClientFactory.get(cluster)
                            def namespaceObj = client.namespaces().withName(namespace).get()
                            if (namespaceObj == null) {
                                log.info("find autocreate flag in request, create namespace {} in cluster {}",
                                        namespace, cluster)
                                def ns = new NamespaceBuilder()
                                        .withMetadata(new ObjectMetaBuilder().withName(namespace).build())
                                        .build()
                                try {
                                    client.namespaces().create(ns)
                                } catch (KubernetesClientException e) {
                                    if (e.getCode() == 409) {
                                        log.error("namespace {} already exists, skip", namespace);
                                    } else {
                                        throw e
                                    }
                                }
                                log.info("namespace {} in cluster {} has created", namespace, cluster)
                            } else {
                                log.info("namespace {} in cluster {} already exists", namespace, cluster)
                            }
                        }

                        // resource quota
                        def resourceQuota = scope.getScopeRef().getSpec().getJSONObject("resourceQuota")
                        if (resourceQuota) {
                            def name = resourceQuota.getString("name")
                            def spec = resourceQuota.getJSONObject("spec")
                            if (StringUtils.isEmpty(name) || spec == null) {
                                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                                        "cannot find name/spec in namespace resourceQuota configuration")
                            }
                            def cluster = request.getClusterId()
                            def client = kubernetesClientFactory.get(cluster)
                            def resourceQuotaObj = client.resourceQuotas().inNamespace(namespace).withName(name).get()
                            if (resourceQuotaObj == null) {
                                log.info("find resource quota in namespace scope, create resource quota {} in " +
                                        "namespace {}", name, namespace)
                                def rq = new JSONObject()
                                rq.put("apiVersion", "v1")
                                rq.put("kind", "ResourceQuota")
                                rq.put("metadata", new JSONObject())
                                rq.getJSONObject("metadata").put("name", name)
                                rq.getJSONObject("metadata").put("namespace", namespace)
                                rq.put("spec", spec)
                                try {
                                    def obj = client.resourceQuotas().load(new ByteArrayInputStream(rq.toJSONString()
                                            .getBytes(StandardCharsets.UTF_8))).get()
                                    client.resourceQuotas().create(obj)
                                } catch (KubernetesClientException e) {
                                    if (e.getCode() == 409) {
                                        log.error("resource quota {} already exists in namespace {}, skip",
                                                name, namespace)
                                    } else {
                                        throw e
                                    }
                                }
                                log.info("resource quota {} in namespace {} has created", name, namespace)
                            } else {
                                log.info("resource quota {} in namespace {} already exists", name, namespace)
                            }
                        }
                    }
                }
            }
        }

        refreshComponentSchemaWorkload(componentSchema)
        pushImages(componentSchema, packageDir)
        apply(request, componentSchema)

        try {
            FileUtils.deleteDirectory(Paths.get(packageDir).toFile())
        } catch (Exception ignored) {
            log.warn("cannot delete component package directory|directory={}", packageDir)
        }
        LaunchDeployComponentHandlerRes res = LaunchDeployComponentHandlerRes.builder()
                .componentSchema(componentSchema)
                .build()
        log.info("default deploy launch res|{}", JSONObject.toJSONString(res))
        return res
    }

    /**
     * 查询部署组件结果
     *
     * @param request 部署请求
     * @return 查询结果
     */
    @Override
    GetDeployComponentHandlerRes get(GetDeployComponentHandlerReq request) {
        def cluster = request.getClusterId()
        def namespace = request.getNamespaceId()
        def name = getMetaName(request.getAppId(), request.getComponentName(), request.getStageId())
        def client = kubernetesClientFactory.get(cluster)
        def result = client.customResource(CRD_CONTEXT).get(namespace, name)
        if (result == null) {
            return GetDeployComponentHandlerRes.builder()
                    .status(DeployComponentStateEnum.FAILURE)
                    .message(String.format("cr not exists|request=%s", JSONObject.toJSONString(request)))
                    .build()
        } else if (result.get("status") == null) {
            return GetDeployComponentHandlerRes.builder()
                    .status(DeployComponentStateEnum.RUNNING)
                    .message(JSONObject.toJSONString(result))
                    .build()
        }

        def statusStr = JSONObject.toJSONString(result.get("status"))
        def status = JSONObject.parseObject(statusStr).getString("condition")
        DeployComponentStateEnum finalStatus
        switch (status) {
            case "Available":
                def lastTransitionTime = ""
                def reconcileSuccessStatus = "False"
                def conditions = JSONObject.parseObject(statusStr).getJSONArray("conditions")
                if (conditions == null || conditions.size() == 0) {
                    finalStatus = DeployComponentStateEnum.RUNNING
                } else {
                    for (Object c : conditions) {
                        def condition = (JSONObject) c
                        if ("ReconcileSuccess" != condition.getString("type")) {
                            continue
                        }
                        lastTransitionTime = condition.getString("lastTransitionTime")
                        reconcileSuccessStatus = condition.getString("status")
                    }
                    if (StringUtils.isEmpty(lastTransitionTime)) {
                        log.error("cannot get last transition time from microservice cr status|status={}", statusStr)
                        finalStatus = DeployComponentStateEnum.FAILURE
                    } else if (reconcileSuccessStatus != "True") {
                        log.error("microservice cr reconcile status is False|status={}", statusStr)
                        finalStatus = DeployComponentStateEnum.FAILURE
                    } else if (System.currentTimeMillis() -
                            (new DateTime(lastTransitionTime).toInstant().getMillis()) > 5 * 1000) {
                        finalStatus = DeployComponentStateEnum.SUCCESS
                    } else {
                        Long now = System.currentTimeMillis()
                        Long calc = new DateTime(lastTransitionTime).toInstant().getMillis()
                        log.info("microservice cr status is ok, but still unstable, waiting for it...|status={}|" +
                                "now={}|calc={}", statusStr, now, calc)
                        finalStatus = DeployComponentStateEnum.RUNNING
                    }
                }
                break
            case "Failure":
                finalStatus = DeployComponentStateEnum.FAILURE
                break
            default:
                finalStatus = DeployComponentStateEnum.RUNNING
                break
        }
        GetDeployComponentHandlerRes res = GetDeployComponentHandlerRes.builder()
                .status(finalStatus)
                .message(statusStr)
                .build()
        log.info("fetch microservice deployment result|result={}", JSONObject.toJSONString(res))
        return res
    }

    /**
     * 兼容历史版本使用：将 workload 中的 containers/initContainers 里面的 images 推送到目标环境中
     * @param request 部署组件请求
     * @param componentSchema ComponentSchema 对象
     * @param packageDir 解压包目录
     */
    private void pushImages(ComponentSchema componentSchema, String packageDir) {
        def images = componentSchema.getSpec().getImages()
        for (def image : images) {
            if (StringUtils.isNotEmpty(image.getName())) {
                def systemArch = CommandUtil.runLocalCommand("uname -i").strip()
                if (systemArch == "x86_64") {
                    if (StringUtils.isNotEmpty(image.getArch()) && image.getArch() != "x86") {
                        log.info("image arch {} is incompatible with system arch {}, skip", image.getArch(), systemArch)
                        continue
                    }
                } else if (systemArch == "aarch64") {
                    if (StringUtils.isNotEmpty(image.getArch()) && image.getArch() != "arm") {
                        log.info("image arch {} is incompatible with system arch {}, skip", image.getArch(), systemArch)
                        continue
                    }
                } else if (systemArch == "sw_64") {
                    if (StringUtils.isNotEmpty(image.getArch()) && image.getArch() != "sw6b") {
                        log.info("image arch {} is incompatible with system arch {}, skip", image.getArch(), systemArch)
                        continue
                    }
                }
                singleImagePush(packageDir, image.getImage(), image.getName(), image.getSha256())
                log.info("image {} has put into registry|arch={}|name={}",
                        image.getImage(), image.getArch(), image.getName())
            }
        }
    }

    /**
     * 兼容历史版本使用：刷新 workload 中的 env 对象
     * @param request 部署组件请求
     * @param componentSchema ComponentSchema 对象
     */
    private static void refreshComponentSchemaWorkload(ComponentSchema componentSchema) {
        def workload = componentSchema.getSpec().getWorkload()
        def specJson = (JSONObject) (workload.getSpec())
        updateEnvironments(specJson)
        workload.setSpec(JSONObject.parseObject(specJson.toJSONString()))
    }

    /**
     * 下载组件包到本地，并解压到临时目录中
     * @param request
     * @return 解压后的目录绝对路径
     */
    private static String getPackageDir(LaunchDeployComponentHandlerReq request) {
        def remoteUrl = request.getComponentPackageUrl()
        if (StringUtils.isEmpty(remoteUrl)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "empty \"componentPackageUrl\" parameter")
        }
        def zipFile = Files.createTempFile(KEY_COMPONENT_PACKAGE_URL, ".zip").toFile()
        def zipFileAbsPath = zipFile.getAbsolutePath()
        NetworkUtil.download(remoteUrl, zipFile.getAbsolutePath())
        def workDirFile = Files.createTempDirectory(KEY_COMPONENT_PACKAGE_URL)
        def workDirAbsPath = workDirFile.toFile().getAbsolutePath()
        ZipUtil.unzip(zipFileAbsPath, workDirAbsPath)
        FileUtils.deleteQuietly(zipFile)
        log.info("action=getPackageDir|zipPath={}|workDir={}", zipFileAbsPath, workDirAbsPath)
        return workDirAbsPath
    }

    /**
     * 将所有的 env 的 value 转换为字符串，避免出现数字
     * @param workloadResource workload resource
     */
    private static void updateEnvironments(JSONObject specJson) {
        JSONObject envJson = specJson.getJSONObject("env")
        if (envJson != null && envJson.size() > 0) {
            envJson.replaceAll({ k, v -> String.valueOf(envJson.get(k)) })
        }
    }

    /**
     * 将单个位于本地 imagePath 的 image 推送到当前仓库中
     *
     * @param packageDir 包目录绝对路径
     * @param image 镜像名称
     * @param imagePath 镜像位于本地的路径
     * @param sha256 Sha256
     */
    private void singleImagePush(String packageDir, String image, String imagePath, String sha256) {
        def dockerRegistry = systemProperties.getDockerRegistry()
        def dockerNamespace = systemProperties.getDockerNamespace()
        def daemonEnv = systemProperties.getRemoteDockerDaemon()
        def newImage = ImageUtil.generateLatestImage(dockerRegistry, dockerNamespace, image)
        if (StringUtils.isEmpty(daemonEnv)) {
            daemonEnv = ""
        } else {
            daemonEnv = "-H " + daemonEnv
        }

        // 加载 Docker 镜像
        String loadCmd = String.format("cd %s && docker %s load -i %s", packageDir, daemonEnv, imagePath)
        CommandUtil.runLocalCommand(loadCmd)

        // 重新 tag 镜像，增加 UUID
        String tagCmd = String.format("docker %s tag %s %s", daemonEnv, sha256, newImage)
        CommandUtil.runLocalCommand(tagCmd)

        // 推送到环境仓库
        String pushCmd = String.format("docker %s push %s", daemonEnv, newImage)
        CommandUtil.runLocalCommand(pushCmd)
    }

    /**
     * 应用 CR 到指定的 Kubernetes 环境中
     * @param request 部署请求
     * @param componentSchema ComponentSchema 对象
     */
    private void apply(LaunchDeployComponentHandlerReq request, ComponentSchema componentSchema) {
        def appId = request.getAppId()
        def componentName = request.getComponentName()
        def cluster = request.getClusterId()
        def namespace = request.getNamespaceId()
        def stageId = request.getStageId()
        def name = getMetaName(appId, componentName, stageId)

        // 将所有 spec.env 转换为字符串
        def workload = componentSchema.getSpec().getWorkload()
        def envObject = ((JSONObject) workload.getSpec()).getJSONObject("env")
        if (envObject != null) {
            def transformMap = new HashMap<String, String>()
            def removeKeyMap = new HashSet<String>()
            for (Map.Entry<String, Object> entry : envObject.entrySet()) {
                def key = entry.getKey()
                def value = entry.getValue()
                // null 直接忽略
                if (value == null) {
                    removeKeyMap.add(key)
                    continue
                }
                // 字符串直接忽略
                if (value instanceof String) {
                    if (StringUtils.isEmpty(value)) {
                        removeKeyMap.add(key)
                    }
                    continue
                }
                // 其他类型全部 to string
                transformMap.put(key, String.valueOf(value))
            }
            envObject.putAll(transformMap)
            for (String key : removeKeyMap) {
                envObject.remove(key)
            }
        }

        // 将全部的 PLACEHOLDER 进行渲染
        def cr = JSONObject.toJSONString(workload)
        try {
            def client = kubernetesClientFactory.get(cluster)
            def resource = new JSONObject(client.customResource(CRD_CONTEXT)
                    .load(new ByteArrayInputStream(cr.getBytes(StandardCharsets.UTF_8))))
            // 以下 id 字段为历史兼容使用
            resource.getJSONObject("metadata").getJSONObject("annotations").put("id", UUID.randomUUID().toString())
            // 填入 owner reference 信息
            if (StringUtils.isNotEmpty(request.getOwnerReference())) {
                resource.getJSONObject("metadata").put("ownerReferences", new JSONArray())
                resource.getJSONObject("metadata").getJSONArray("ownerReferences")
                        .add(JSONObject.parseObject(request.getOwnerReference()))
            }
            try {
                def currentCr = client.customResource(CRD_CONTEXT).get(namespace, name)
                if (currentCr == null || currentCr.size() == 0) {
                    def result = client.customResource(CRD_CONTEXT).create(namespace, resource)
                    log.info("cr yaml has created in kubernetes|cluster={}|namespace={}|stageId={}|name={}|cr={}|" +
                            "result={}", cluster, namespace, stageId, name, cr, JSONObject.toJSONString(result))
                } else {
                    def current = new JSONObject(currentCr)
                    def labels = resource.getJSONObject("metadata").getJSONObject("labels")
                    def annotations = resource.getJSONObject("metadata").getJSONObject("annotations")
                    current.getJSONObject("metadata").put("labels", labels)
                    current.getJSONObject("metadata").put("annotations", annotations)
                    // 填入 owner reference 信息
                    if (StringUtils.isNotEmpty(request.getOwnerReference())) {
                        current.getJSONObject("metadata").put("ownerReferences", new JSONArray())
                        current.getJSONObject("metadata").getJSONArray("ownerReferences")
                                .add(JSONObject.parseObject(request.getOwnerReference()))
                    }
                    current.put("spec", resource.getJSONObject("spec"))
                    def result = client.customResource(CRD_CONTEXT).edit(namespace, name, current)
                    log.info("cr yaml has updated in kubernetes|cluster={}|namespace={}|stageId={}|name={}|cr={}|" +
                            "result={}", cluster, namespace, stageId, name, cr, JSONObject.toJSONString(result))
                }
            } catch (KubernetesClientException e) {
                if (e.getCode() == 404) {
                    def result = client.customResource(CRD_CONTEXT).create(namespace, resource)
                    log.info("cr yaml has created in kubernetes|cluster={}|namespace={}|stageId={}|name={}|cr={}|" +
                            "result={}", cluster, namespace, stageId, name, cr, JSONObject.toJSONString(result))
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            log.error("apply cr yaml to kubernetes failed|cluster={}|namespace={}|stageId={}|" +
                    "exception={}|cr={}", cluster, namespace, stageId, ExceptionUtils.getStackTrace(e), cr)
            throw e
        }
    }

    /**
     * 获取部署 Component 的 meta 信息中的 name
     * @param request 部署组件请求
     * @return
     */
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
