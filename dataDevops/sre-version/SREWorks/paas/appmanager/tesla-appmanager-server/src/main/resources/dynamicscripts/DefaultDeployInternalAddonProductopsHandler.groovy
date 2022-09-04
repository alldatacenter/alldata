package dynamicscripts

import com.alibaba.fastjson.JSONObject
import com.alibaba.tesla.appmanager.autoconfig.SystemProperties
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant
import com.alibaba.tesla.appmanager.common.enums.ComponentInstanceStatusEnum
import com.alibaba.tesla.appmanager.common.enums.DeployComponentStateEnum
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode
import com.alibaba.tesla.appmanager.common.exception.AppException
import com.alibaba.tesla.appmanager.common.util.NetworkUtil
import com.alibaba.tesla.appmanager.common.util.ZipUtil
import com.alibaba.tesla.appmanager.domain.req.componentinstance.ReportRtComponentInstanceStatusReq
import com.alibaba.tesla.appmanager.domain.req.deploy.GetDeployComponentHandlerReq
import com.alibaba.tesla.appmanager.domain.req.deploy.LaunchDeployComponentHandlerReq
import com.alibaba.tesla.appmanager.domain.res.deploy.GetDeployComponentHandlerRes
import com.alibaba.tesla.appmanager.domain.res.deploy.LaunchDeployComponentHandlerRes
import com.alibaba.tesla.appmanager.kubernetes.KubernetesClientFactory
import com.alibaba.tesla.appmanager.server.factory.HttpClientFactory
import com.alibaba.tesla.appmanager.server.service.deploy.handler.DeployComponentHandler
import com.alibaba.tesla.appmanager.server.service.rtcomponentinstance.RtComponentInstanceService
import okhttp3.*
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.exception.ExceptionUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

import java.nio.file.Files
import java.nio.file.Path

/**
 * 默认部署 ProductOps Groovy Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
class DefaultDeployInternalAddonProductopsHandler implements DeployComponentHandler {

    private static final Logger log = LoggerFactory.getLogger(DefaultDeployInternalAddonProductopsHandler.class)

    private static final String KEY_COMPONENT_PACKAGE_URL = "appmanager_deploy_component_package"

    /**
     * 当前内置 Handler 类型
     */
    public static final String KIND = DynamicScriptKindEnum.DEPLOY_IA_PRODUCTOPS_COMPONENT.toString()

    /**
     * 当前内置 Handler 名称
     */
    public static final String NAME = DefaultConstant.DEFAULT_GROOVY_HANDLER

    /**
     * 当前内置 Handler 版本
     */
    public static final Integer REVISION = 10

    private static final String IMPORT_TMP_FILE = "productops_tmp_import.zip"
    private static final String ANNOTATIONS_VERSION = "annotations.appmanager.oam.dev/version"
    private static final String ANNOTATIONS_COMPONENT_INSTANCE_ID = "annotations.appmanager.oam.dev/componentInstanceId"
    private static final String ANNOTATIONS_APP_INSTANCE_NAME = "annotations.appmanager.oam.dev/appInstanceName"

    @Autowired
    private KubernetesClientFactory kubernetesClientFactory

    @Autowired
    private SystemProperties systemProperties

    @Autowired
    private RtComponentInstanceService componentInstanceService

    /**
     * 部署组件过程
     *
     * @param request 部署请求
     */
    @Override
    LaunchDeployComponentHandlerRes launch(LaunchDeployComponentHandlerReq request) {
        def packageDir = getPackageDir(request)
        def componentSchema = request.getComponentSchema()

        // 将当前目录下的配置进行打包处理
        String zipPath = packageDir.resolve(IMPORT_TMP_FILE).toString()
        Files.list(packageDir).map({ p -> p.toFile() }).forEach({ item ->
            if (item.isDirectory()) {
                ZipUtil.zipDirectory(zipPath, item)
            } else {
                ZipUtil.zipFile(zipPath, item)
            }
        })

        // 准备参数
        def options = (JSONObject) componentSchema.getSpec().getWorkload().getSpec()
        def endpoint, namespaceId, stageId, envId
        if ("PaaS" == System.getenv("CLOUD_TYPE") && StringUtils.isNotEmpty(options.getString("targetEndpoint"))) {
            endpoint = options.getString("targetEndpoint")
            namespaceId = request.getNamespaceId()
            stageId = options.getString("stageId")
        } else if ("OXS" == System.getenv("CLOUD_TYPE") && StringUtils.isNotEmpty(options.getString("targetEndpoint"))) {
            endpoint = options.getString("targetEndpoint")
            namespaceId = ""
            stageId = options.getString("stageId")
        } else {
            endpoint = "http://" + (StringUtils.isEmpty(System.getenv("ENDPOINT_PAAS_PRODUCTOPS"))
                    ? "" : System.getenv("ENDPOINT_PAAS_PRODUCTOPS"))
            if (endpoint == "http://") {
                endpoint = options.getString("targetEndpoint")
            }
            def multipleEnv = options.getBoolean("multipleEnv")
            if (multipleEnv != null && multipleEnv) {
                namespaceId = request.getNamespaceId()
                stageId = request.getStageId()
            } else {
                namespaceId = ""
                stageId = "prod"
            }
        }
        if (StringUtils.isNotEmpty(namespaceId)) {
            envId = namespaceId + "," + stageId
        } else {
            envId = stageId
        }

        // 上传配置
        uploadConfiguration(request, options, endpoint, zipPath, namespaceId, envId)
        try {
            reportToElasticsearch(endpoint, request.getAppId(), envId)
        } catch (Exception e) {
            log.warn("report to elasticsearch failed, exception={}", ExceptionUtils.getStackTrace(e))
        }

        // 上报状态
        def annotations = (JSONObject) componentSchema.getSpec().getWorkload().getMetadata().getAnnotations()
        def version = annotations.getOrDefault(ANNOTATIONS_VERSION, "")
        def componentInstanceId = annotations.getOrDefault(ANNOTATIONS_COMPONENT_INSTANCE_ID, "")
        def appInstanceName = annotations.getOrDefault(ANNOTATIONS_APP_INSTANCE_NAME, "")
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
     * 上报某个应用下的全量 AppTree ID 数据到 Elasticsearch 后端中
     * @param endpoint ProductOps Endpoint
     * @param appId 应用 ID
     * @param envId 环境 ID
     */
    private static void reportToElasticsearch(String endpoint, String appId, String envId) {
        def appTreeIds = getAppTreeIds(endpoint, appId, envId)
        for (String appTreeId : appTreeIds) {
            try {
                reportSingleAppTreeToElasticsearch(endpoint, appTreeId, envId)
            } catch (Exception e) {
                log.warn("report app tree {} failed, exception={}", appTreeId, ExceptionUtils.getStackTrace(e))
            }
        }
    }

    /**
     * 上报单个 appTreeId 的数据到 Elasticsearch 后端中
     * @param endpoint ProductOps Endpoint
     * @param appTreeId AppTree ID
     * @param envId 环境 ID
     */
    private static void reportSingleAppTreeToElasticsearch(String endpoint, String appTreeId, String envId) {
        def httpClient = HttpClientFactory.getHttpClient()
        def times = 2
        while (true) {
            def urlPrefix = String.format("%s/jobs/report_app_tree_structures/%s/start", endpoint, appTreeId)
            def urlBuilder = Objects.requireNonNull(HttpUrl.parse(urlPrefix)).newBuilder()
            def reqBuilder = new Request.Builder()
                    .url(urlBuilder.build())
                    .headers(Headers.of("X-Env", envId))
                    .put(RequestBody.create(null, ""))
            try {
                def response = NetworkUtil.sendRequestSimple(httpClient, reqBuilder, "")
                def responseBody = response.body()
                if (responseBody == null) {
                    throw new AppException(AppErrorCode.DEPLOY_ERROR,
                            "cannot report apptree {}, null response", appTreeId)
                }
                def bodyStr = responseBody.string()
                if (bodyStr.contains("Server exception: \'meta_region\'")) {
                    log.info("[app_tree_id.{}] report productops config to elasticsearch success", appTreeId)
                    return
                } else if (bodyStr.contains("protocol=http/1.1, code=503, message=Service Unavailable")
                        || bodyStr.contains("502 Bad Gateway")) {
                    Thread.sleep(30 * 1000L)
                    throw new AppException(AppErrorCode.DEPLOY_ERROR,
                            String.format("cool down because of busy elasticsearch...|appTreeId=%s|response=%s",
                                    appTreeId, bodyStr))
                }
                if (response.code() != 200) {
                    throw new AppException(AppErrorCode.DEPLOY_ERROR,
                            String.format("send request failed, http status not 200|response=%s", bodyStr))
                }
                JSONObject body
                try {
                    body = JSONObject.parseObject(bodyStr)
                } catch (Exception e) {
                    throw new AppException(AppErrorCode.DEPLOY_ERROR,
                            String.format("send request failed, response not json|response=%s", bodyStr))
                }
                int code = body.getIntValue("code")
                if (code != 200) {
                    throw new AppException(AppErrorCode.DEPLOY_ERROR,
                            String.format("send request failed, response code not 200|response=%s", bodyStr))
                }
                log.info("[app_tree_id.{}] report productops config to elasticsearch success", appTreeId)
                return
            } catch (Exception e) {
                if (times <= 0) {
                    throw e
                } else {
                    log.error("[app_tree_id.{}] report productops config to elasticsearch failed, exception={}",
                            appTreeId, ExceptionUtils.getStackTrace(e))
                    times -= 1
                }
            }
        }
    }

    /**
     * 获取 ProdcutOps 指定环境下的指定应用存在哪些 AppTree ID
     * @param endpoint ProductOps Endpoint
     * @param appId 应用 ID
     * @param envId 环境 ID
     * @return ArrayList of String
     */
    private static List<String> getAppTreeIds(String endpoint, String appId, String envId) {
        def httpClient = HttpClientFactory.getHttpClient()
        def urlPrefix = String.format("%s/appTrees", endpoint)
        def urlBuilder = Objects.requireNonNull(HttpUrl.parse(urlPrefix)).newBuilder()
        urlBuilder.addQueryParameter("search", appId)
        urlBuilder.addQueryParameter("page", "1")
        urlBuilder.addQueryParameter("pageSize", "10000")
        def reqBuilder = new Request.Builder()
                .url(urlBuilder.build())
                .headers(Headers.of("X-Env", envId))
                .get()
        def response = NetworkUtil.sendRequest(httpClient, reqBuilder, "")
        def items = response.getJSONObject("data").getJSONArray("items")
        def appTreeIds = new ArrayList<String>()
        for (Object item : items) {
            def appTreeId = ((JSONObject) item).getString("appTreeId")
            if (StringUtils.isEmpty(appTreeId)) {
                continue
            }
            appTreeIds.add(appTreeId)
        }
        log.info("get app tree id list from productops service|appTreeIds={}", JSONObject.toJSONString(appTreeIds))
        return appTreeIds
    }

    /**
     * 上传配置到 ProductOps 服务中
     * @param request 请求内容
     * @param options 选项字典
     * @param endpoint 目标 ProductOps Endpoint
     * @param zipPath 压缩包路径
     * @param namespaceId Namespace ID
     * @param envId Env ID
     */
    private static void uploadConfiguration(
            LaunchDeployComponentHandlerReq request, JSONObject options, String endpoint, String zipPath,
            String namespaceId, String envId) {
        def httpClient = HttpClientFactory.getHttpClient()
        def times = 2
        def cloudType = System.getenv("CLOUD_TYPE")
        while (true) {
            def urlPrefix = String.format("%s/maintainer/upload", endpoint)
            def urlBuilder = Objects.requireNonNull(HttpUrl.parse(urlPrefix)).newBuilder()
            def resetVersion = options.getString("resetVersion")
            if (resetVersion == null) {
                resetVersion = "false"
            }
            urlBuilder.addQueryParameter("resetVersion", resetVersion)
            if (StringUtils.isNotEmpty(namespaceId)
                    || "OXS" == cloudType
                    || "ApsaraStack" == cloudType
                    || "ApsaraStackAgility" == cloudType) {
                urlBuilder.addQueryParameter("envId", envId)
            }
            def body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", IMPORT_TMP_FILE,
                            RequestBody.create(MediaType.parse("application/octet-stream"),
                                    new File(zipPath)))
                    .build()
            def reqBuilder = new Request.Builder()
                    .url(urlBuilder.build())
                    .post(body)
            try {
                NetworkUtil.sendRequest(httpClient, reqBuilder, "")
                log.info("[app_id.{}] import productops config success", request.getAppId())
                times -= 1
                if (times <= 0) {
                    break
                }
            } catch (Exception e) {
                if (times <= 0) {
                    throw e
                } else {
                    log.error("[app_id.{}] import productops config failed, exception={}",
                            request.getAppId(), ExceptionUtils.getStackTrace(e))
                    times -= 1
                }
            }
        }
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
                .message("productops config has imported")
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
