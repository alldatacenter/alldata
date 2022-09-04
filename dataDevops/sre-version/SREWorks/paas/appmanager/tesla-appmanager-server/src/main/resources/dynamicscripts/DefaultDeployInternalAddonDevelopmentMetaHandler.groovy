package dynamicscripts

import com.alibaba.fastjson.JSONObject
import com.alibaba.tesla.appmanager.api.provider.K8sMicroServiceMetaProvider
import com.alibaba.tesla.appmanager.autoconfig.SystemProperties
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant
import com.alibaba.tesla.appmanager.common.enums.ComponentInstanceStatusEnum
import com.alibaba.tesla.appmanager.common.enums.DeployComponentStateEnum
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode
import com.alibaba.tesla.appmanager.common.exception.AppException
import com.alibaba.tesla.appmanager.common.util.NetworkUtil
import com.alibaba.tesla.appmanager.common.util.ZipUtil
import com.alibaba.tesla.appmanager.domain.req.AppAddonCreateReq
import com.alibaba.tesla.appmanager.domain.req.K8sMicroServiceMetaQueryReq
import com.alibaba.tesla.appmanager.domain.req.componentinstance.ReportRtComponentInstanceStatusReq
import com.alibaba.tesla.appmanager.domain.req.deploy.GetDeployComponentHandlerReq
import com.alibaba.tesla.appmanager.domain.req.deploy.LaunchDeployComponentHandlerReq
import com.alibaba.tesla.appmanager.domain.res.deploy.GetDeployComponentHandlerRes
import com.alibaba.tesla.appmanager.domain.res.deploy.LaunchDeployComponentHandlerRes
import com.alibaba.tesla.appmanager.meta.helm.repository.condition.HelmMetaQueryCondition
import com.alibaba.tesla.appmanager.meta.helm.repository.domain.HelmMetaDO
import com.alibaba.tesla.appmanager.meta.helm.service.HelmMetaService
import com.alibaba.tesla.appmanager.meta.k8smicroservice.assembly.K8sMicroServiceMetaDtoConvert
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.domain.K8sMicroServiceMetaDO
import com.alibaba.tesla.appmanager.meta.k8smicroservice.service.K8sMicroserviceMetaService
import com.alibaba.tesla.appmanager.server.repository.condition.AppAddonQueryCondition
import com.alibaba.tesla.appmanager.server.repository.domain.AppAddonDO
import com.alibaba.tesla.appmanager.server.service.addon.AddonMetaService
import com.alibaba.tesla.appmanager.server.service.appaddon.AppAddonService
import com.alibaba.tesla.appmanager.server.service.deploy.handler.DeployComponentHandler
import com.alibaba.tesla.appmanager.server.service.rtcomponentinstance.RtComponentInstanceService
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
/**
 * 默认部署 AppMeta Groovy Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
class DefaultDeployInternalAddonDevelopmentMetaHandler implements DeployComponentHandler {

    private static final Logger log = LoggerFactory.getLogger(DefaultDeployInternalAddonDevelopmentMetaHandler.class)

    private static final String KEY_COMPONENT_PACKAGE_URL = "appmanager_deploy_component_package"

    /**
     * 当前内置 Handler 类型
     */
    public static final String KIND = DynamicScriptKindEnum.DEPLOY_IA_DEVELOPMENT_META_COMPONENT.toString()

    /**
     * 当前内置 Handler 名称
     */
    public static final String NAME = DefaultConstant.DEFAULT_GROOVY_HANDLER

    /**
     * 当前内置 Handler 版本
     */
    public static final Integer REVISION = 18

    private static final String EXPORT_OPTION_FILE = "option.json"
    private static final String ANNOTATIONS_VERSION = "annotations.appmanager.oam.dev/version"
    private static final String ANNOTATIONS_COMPONENT_INSTANCE_ID = "annotations.appmanager.oam.dev/componentInstanceId"
    private static final String ANNOTATIONS_APP_INSTANCE_NAME = "annotations.appmanager.oam.dev/appInstanceName"

    @Autowired
    private SystemProperties systemProperties

    @Autowired
    private K8sMicroServiceMetaProvider k8sMicroServiceMetaProvider

    @Autowired
    private K8sMicroserviceMetaService k8sMicroserviceMetaService

    @Autowired
    private K8sMicroServiceMetaDtoConvert k8sMicroServiceMetaDtoConvert

    @Autowired
    private RtComponentInstanceService componentInstanceService

    @Autowired
    private HelmMetaService helmMetaService

    @Autowired
    private AppAddonService appAddonService

    @Autowired
    private AddonMetaService addonMetaService

    /**
     * 部署组件过程
     *
     * @param request 部署请求
     */
    @Override
    LaunchDeployComponentHandlerRes launch(LaunchDeployComponentHandlerReq request) {
        def packageDir = getPackageDir(request)
        def componentSchema = request.getComponentSchema()
        def exportPath = Paths.get(packageDir.toString(), EXPORT_OPTION_FILE)

        // 准备参数
        def content = FileUtils.readFileToString(exportPath.toFile(), StandardCharsets.UTF_8)
        def data = JSONObject.parseObject(content)

        // microservices
        if (data.getJSONArray("microservices") != null) {
            for (raw in data.getJSONArray("microservices").toJavaList(K8sMicroServiceMetaDO.class)) {
                def dto = k8sMicroServiceMetaDtoConvert.to(raw)
                def appId = dto.getAppId()
                def microserviceId = dto.getMicroServiceId()
                def namespaceId = dto.getNamespaceId()
                def stageId = dto.getStageId()
                def metaList = k8sMicroServiceMetaProvider
                        .list(K8sMicroServiceMetaQueryReq.builder()
                                .microServiceId(microserviceId)
                                .appId(appId)
                                .namespaceId(namespaceId)
                                .stageId(stageId)
                                .withBlobs(true)
                                .pagination(false)
                                .build())
                if (CollectionUtils.isEmpty(metaList.getItems())) {
                    def response = k8sMicroServiceMetaProvider.create(dto)
                    log.info("import(create) app meta microservice config success|appId={}|dto={}|response={}",
                            request.getAppId(), JSONObject.toJSONString(dto), JSONObject.toJSONString(response))
                } else {
                    def response = k8sMicroServiceMetaProvider.update(dto)
                    log.info("import(update) app meta microservice config success|appId={}|dto={}|response={}",
                            request.getAppId(), JSONObject.toJSONString(dto), JSONObject.toJSONString(response))
                }
            }
        }

        // helms
        if (data.getJSONArray("helms") != null) {
            for (helmMetaDO in data.getJSONArray("helms").toJavaList(HelmMetaDO.class)) {
                def appId = helmMetaDO.getAppId()
                def helmPackageId = helmMetaDO.getHelmPackageId()
                def namespaceId = helmMetaDO.getNamespaceId()
                def stageId = helmMetaDO.getStageId()
                HelmMetaDO helmMeta = helmMetaService.getByHelmPackageId(appId, helmPackageId, namespaceId, stageId)
                if (helmMeta == null) {
                    def response = helmMetaService.create(helmMetaDO)
                    log.info("import(create) app meta helm config success|appId={}|content={}|response={}",
                            request.getAppId(), JSONObject.toJSONString(helmMetaDO), JSONObject.toJSONString(response))
                } else {
                    HelmMetaQueryCondition condition = HelmMetaQueryCondition.builder()
                            .helmPackageId(helmMeta.getHelmPackageId())
                            .appId(helmMeta.getAppId())
                            .namespaceId(helmMeta.getNamespaceId())
                            .stageId(helmMeta.getStageId())
                            .withBlobs(true)
                            .build();
                    helmMetaDO.setId(null);
                    def response = helmMetaService.update(helmMetaDO, condition)
                    log.info("import(update) app meta helm config success |appId={}|content={}|response={}",
                            request.getAppId(), JSONObject.toJSONString(helmMetaDO), JSONObject.toJSONString(response))
                }
            }
        }

        // addons
        if (data.getJSONArray("addons") != null) {
            for (addon in data.getJSONArray("addons").toJavaList(AppAddonDO.class)) {
                def item = appAddonService.get(AppAddonQueryCondition.builder()
                        .appId(addon.getAppId())
                        .namespaceId(addon.getNamespaceId())
                        .stageId(addon.getStageId())
                        .addonId(addon.getAddonId())
                        .addonName(addon.getName())
                        .build())
                if (item == null) {
                    def addonMeta = addonMetaService.get(addon.getAddonType(), addon.getAddonId())
                    if (addonMeta == null) {
                        throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                                String.format("invalid addon meta %s", addon.getAddonId()))
                    }
                    def addonConfig = addon.getAddonConfig()
                    if (StringUtils.isEmpty(addonConfig)) {
                        addonConfig = "{}"
                    }
                    appAddonService.create(AppAddonCreateReq.builder()
                            .appId(addon.getAppId())
                            .namespaceId(addon.getNamespaceId())
                            .stageId(addon.getStageId())
                            .addonMetaId(addonMeta.getId())
                            .addonType(addon.getAddonType().toString())
                            .addonId(addon.getAddonId())
                            .addonName(addon.getName())
                            .spec(JSONObject.parseObject(addonConfig))
                            .build())
                    log.info("app addon has created|addon={}", JSONObject.toJSONString(addon))
                } else {
                    addon.setId(null)
                    int count = appAddonService.update(addon, AppAddonQueryCondition.builder()
                            .appId(addon.getAppId())
                            .namespaceId(addon.getNamespaceId())
                            .stageId(addon.getStageId())
                            .addonId(addon.getAddonId())
                            .addonName(addon.getName())
                            .build())
                    if (count == 0) {
                        throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                                String.format("cannot update app addon, count=0|appId=%s|addonId=%s|addonName=%s",
                                        addon.getAppId(), addon.getAddonId(), addon.getName()))
                    }
                    log.info("app addon has updated|addon={}", JSONObject.toJSONString(addon))
                }
            }
        }

        // 上报状态
        def annotations = (JSONObject) componentSchema.getSpec().getWorkload().getMetadata().getAnnotations()
        def version = (String) annotations.getOrDefault(ANNOTATIONS_VERSION, "")
        def componentInstanceId = (String) annotations.getOrDefault(ANNOTATIONS_COMPONENT_INSTANCE_ID, "")
        def appInstanceName = (String) annotations.getOrDefault(ANNOTATIONS_APP_INSTANCE_NAME, "")
        componentInstanceService.report(ReportRtComponentInstanceStatusReq.builder()
                .componentInstanceId(componentInstanceId)
                .appInstanceName(appInstanceName)
                .clusterId(request.getClusterId())
                .namespaceId(request.getNamespaceId())
                .stageId(request.getStageId())
                .appId(request.getAppId())
                .componentType(request.getComponentType())
                .componentName(request.getComponentName())
                .version(version)
                .status(ComponentInstanceStatusEnum.COMPLETED.toString())
                .conditions(new ArrayList<>())
                .build())

        // 资源清理
        try {
            FileUtils.deleteDirectory(packageDir.toFile())
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
                .message("app meta config has imported")
                .build()
    }

    /**
     * 下载组件包到本地，并解压到临时目录中
     * @param request
     * @return 解压后的目录绝对路径
     */
    private static Path getPackageDir(LaunchDeployComponentHandlerReq request) {
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
        return workDirFile
    }
}
