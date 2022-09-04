package dynamicscripts

import com.alibaba.fastjson.JSONObject
import com.alibaba.tesla.appmanager.autoconfig.SystemProperties
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant
import com.alibaba.tesla.appmanager.common.enums.DeployComponentStateEnum
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode
import com.alibaba.tesla.appmanager.common.exception.AppException
import com.alibaba.tesla.appmanager.common.util.NetworkUtil
import com.alibaba.tesla.appmanager.common.util.RequestUtil
import com.alibaba.tesla.appmanager.common.util.ZipUtil
import com.alibaba.tesla.appmanager.domain.req.deploy.GetDeployComponentHandlerReq
import com.alibaba.tesla.appmanager.domain.req.deploy.LaunchDeployComponentHandlerReq
import com.alibaba.tesla.appmanager.domain.res.deploy.GetDeployComponentHandlerRes
import com.alibaba.tesla.appmanager.domain.res.deploy.LaunchDeployComponentHandlerRes
import com.alibaba.tesla.appmanager.kubernetes.KubernetesClientFactory
import com.alibaba.tesla.appmanager.server.provider.impl.ClusterProviderImpl
import com.alibaba.tesla.appmanager.server.service.deploy.handler.DeployComponentHandler
import com.alibaba.tesla.appmanager.server.service.rtcomponentinstance.RtComponentInstanceService
import okhttp3.OkHttpClient
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
/**
 * 默认构建 ProductOps V2 Groovy Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
class ProductopsDeployHandler implements DeployComponentHandler {

    private static final Logger log = LoggerFactory.getLogger(ProductopsDeployHandler.class)

    private static final String KEY_COMPONENT_PACKAGE_URL = "appmanager_deploy_component_package"

    /**
     * 当前内置 Handler 类型
     */
    public static final String KIND = DynamicScriptKindEnum.DEPLOY_IA_V2_PRODUCTOPS_COMPONENT.toString()

    /**
     * 当前内置 Handler 名称
     */
    public static final String NAME = DefaultConstant.DEFAULT_GROOVY_HANDLER

    /**
     * 当前内置 Handler 版本
     */
    public static final Integer REVISION = 22

    private static final String ANNOTATIONS_VERSION = "annotations.appmanager.oam.dev/version"
    private static final String ANNOTATIONS_COMPONENT_INSTANCE_ID = "annotations.appmanager.oam.dev/componentInstanceId"

    @Autowired
    private KubernetesClientFactory kubernetesClientFactory

    @Autowired
    private SystemProperties systemProperties

    @Autowired
    private ClusterProviderImpl clusterProvider

    @Autowired
    private RtComponentInstanceService componentInstanceService

    @Override
    LaunchDeployComponentHandlerRes launch(LaunchDeployComponentHandlerReq request) {
        def packageDir = getPackageDir(request)
        def componentSchema = request.getComponentSchema()
        def spec = (JSONObject) componentSchema.getSpec().getWorkload().getSpec()
        def targetEndpoint = spec.getString("targetEndpoint")
        if (StringUtils.isEmpty(targetEndpoint)) {
            targetEndpoint = "prod-flycore-paas-action"
        }

        apply(request, targetEndpoint, packageDir)

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

    @Override
    GetDeployComponentHandlerRes get(GetDeployComponentHandlerReq request) {
        GetDeployComponentHandlerRes res = GetDeployComponentHandlerRes.builder()
                .status(DeployComponentStateEnum.SUCCESS)
                .message("")
                .build()
        return res
    }

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

    private static void apply(LaunchDeployComponentHandlerReq request, String targetEndpoint, String packageDir) {
        def stageId = request.getStageId()
        def contentJsonContent = FileUtils.readFileToString(new File(packageDir + "/content.json"), "UTF-8")
        if (StringUtils.isEmpty(stageId)) {
            stageId = "prod"
        }
        def importUrl = "http://" + targetEndpoint + "/frontend/exImport/import?stageId=" + stageId
        def httpClientBuilder = new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.MINUTES)
                .writeTimeout(30, TimeUnit.MINUTES)
        def httpClient = RequestUtil.newHttpClient(httpClientBuilder)
        def ret = RequestUtil.post(httpClient, importUrl, contentJsonContent, String.class)
        log.info("import frontend-service {} {}", importUrl, contentJsonContent)
        def retJson = JSONObject.parseObject(ret)
        if (retJson.getIntValue("code") != 200) {
            throw new Exception("apply productops error with ret: " + ret)
        }
    }
}
