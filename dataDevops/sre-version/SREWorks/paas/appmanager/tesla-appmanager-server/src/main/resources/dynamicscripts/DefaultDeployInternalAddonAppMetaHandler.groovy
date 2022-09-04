package dynamicscripts

import com.alibaba.fastjson.JSONObject
import com.alibaba.tesla.appmanager.api.provider.AppMetaProvider
import com.alibaba.tesla.appmanager.autoconfig.SystemProperties
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant
import com.alibaba.tesla.appmanager.common.enums.DeployComponentStateEnum
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode
import com.alibaba.tesla.appmanager.common.exception.AppException
import com.alibaba.tesla.appmanager.common.util.NetworkUtil
import com.alibaba.tesla.appmanager.common.util.ZipUtil
import com.alibaba.tesla.appmanager.domain.req.AppMetaUpdateReq
import com.alibaba.tesla.appmanager.domain.req.deploy.GetDeployComponentHandlerReq
import com.alibaba.tesla.appmanager.domain.req.deploy.LaunchDeployComponentHandlerReq
import com.alibaba.tesla.appmanager.domain.res.deploy.GetDeployComponentHandlerRes
import com.alibaba.tesla.appmanager.domain.res.deploy.LaunchDeployComponentHandlerRes
import com.alibaba.tesla.appmanager.server.service.deploy.handler.DeployComponentHandler
import com.alibaba.tesla.appmanager.server.service.rtcomponentinstance.RtComponentInstanceService
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 默认部署 AppMeta Groovy Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
class DefaultDeployInternalAddonAppMetaHandler implements DeployComponentHandler {

    private static final Logger log = LoggerFactory.getLogger(DefaultDeployInternalAddonAppMetaHandler.class)

    private static final String KEY_COMPONENT_PACKAGE_URL = "appmanager_deploy_component_package"

    /**
     * 当前内置 Handler 类型
     */
    public static final String KIND = DynamicScriptKindEnum.DEPLOY_IA_APP_META_COMPONENT.toString()

    /**
     * 当前内置 Handler 名称
     */
    public static final String NAME = DefaultConstant.DEFAULT_GROOVY_HANDLER

    /**
     * 当前内置 Handler 版本
     */
    public static final Integer REVISION = 10

    private static final String EXPORT_OPTION_FILE = "option.json"
    private static final String ANNOTATIONS_VERSION = "annotations.appmanager.oam.dev/version"
    private static final String ANNOTATIONS_COMPONENT_INSTANCE_ID = "annotations.appmanager.oam.dev/componentInstanceId"
    private static final String ANNOTATIONS_APP_INSTANCE_NAME = "annotations.appmanager.oam.dev/appInstanceName"

    @Autowired
    private SystemProperties systemProperties

    @Autowired
    private AppMetaProvider appMetaProvider

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
        def spec = (JSONObject) componentSchema.getSpec().getWorkload().getSpec()
        def exportPath = Paths.get(packageDir.toString(), EXPORT_OPTION_FILE)

        // 准备参数
        def content = FileUtils.readFileToString(exportPath.toFile(), Charset.defaultCharset())
        def contentJson = JSONObject.parseObject(content)
        def data = new AppMetaUpdateReq()
        def options = contentJson.getJSONObject("options")
        // 针对 options 导入的时候默认 isDevelopment=0
        def overwriteIsDevelopment = spec.getBoolean("overwriteIsDevelopment")
        if (overwriteIsDevelopment != null && overwriteIsDevelopment) {
            options.put("isDevelopment", "0")
            log.info("find overwriteIsDevelopment flag, set app option isDevelopment to 0|appId={}", request.getAppId())
        }
        def overwriteBuiltIn = spec.getBoolean("overwriteBuiltIn")
        if (overwriteBuiltIn != null && overwriteBuiltIn) {
            options.put("builtIn", "1")
            log.info("find overwriteBuiltIn flag, set app option builtIn to 1|appId={}", request.getAppId())
        }
        data.setAppId(request.getAppId())
        data.setOptions(options)
        def response = appMetaProvider.save(data, DefaultConstant.SYSTEM_OPERATOR)
        log.info("import app meta config success|appId={}|response={}",
                request.getAppId(), JSONObject.toJSONString(response))

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
