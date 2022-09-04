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
import com.alibaba.tesla.appmanager.kubernetes.KubernetesClientFactory
import com.alibaba.tesla.appmanager.server.service.deploy.handler.DeployComponentHandler
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

/**
 * 默认构建 Job Groovy Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
class DefaultDeployJobHandler implements DeployComponentHandler {

    private static final Logger log = LoggerFactory.getLogger(DefaultDeployJobHandler.class)

    private static final String KEY_COMPONENT_PACKAGE_URL = "appmanager_deploy_component_package"

    /**
     * 当前内置 Handler 类型
     */
    public static final String KIND = DynamicScriptKindEnum.DEPLOY_JOB_COMPONENT.toString()

    /**
     * 当前内置 Handler 名称
     */
    public static final String NAME = DefaultConstant.DEFAULT_GROOVY_HANDLER

    /**
     * 当前内置 Handler 版本
     */
    public static final Integer REVISION = 10

    /**
     * CRD Context
     */
    private static final CustomResourceDefinitionContext CRD_CONTEXT = new CustomResourceDefinitionContext.Builder()
            .withName("jobs.apps.abm.io")
            .withGroup("apps.abm.io")
            .withVersion("v1")
            .withPlural("jobs")
            .withScope("Namespaced")
            .build();

    @Autowired
    private KubernetesClientFactory kubernetesClientFactory;

    @Autowired
    private SystemProperties systemProperties;

    /**
     * 部署组件过程
     *
     * @param request 部署请求
     */
    @Override
    LaunchDeployComponentHandlerRes launch(LaunchDeployComponentHandlerReq request) {
        def packageDir = getPackageDir(request)
        def componentSchema = request.getComponentSchema()
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
        return GetDeployComponentHandlerRes.builder()
                .status(DeployComponentStateEnum.SUCCESS)
                .message("SUCCESS")
                .build()
    }

    /**
     * 兼容历史版本使用：将 workload 中的 jobs 里面的 images 推送到目标环境中
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
                    log.info("cr yaml has updated in kubernetes|cluster={}|namespace={}|stageId={}|name={}|cr={}|result={}",
                            cluster, namespace, stageId, name, cr, JSONObject.toJSONString(result))
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
