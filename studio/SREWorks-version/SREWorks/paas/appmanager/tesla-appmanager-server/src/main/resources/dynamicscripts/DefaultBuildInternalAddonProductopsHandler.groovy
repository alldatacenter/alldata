package dynamicscripts

import com.alibaba.fastjson.JSONObject
import com.alibaba.tesla.appmanager.autoconfig.PackageProperties
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode
import com.alibaba.tesla.appmanager.common.exception.AppException
import com.alibaba.tesla.appmanager.common.util.PackageUtil
import com.alibaba.tesla.appmanager.common.util.StringUtil
import com.alibaba.tesla.appmanager.common.util.ZipUtil
import com.alibaba.tesla.appmanager.domain.core.StorageFile
import com.alibaba.tesla.appmanager.domain.req.componentpackage.BuildComponentHandlerReq
import com.alibaba.tesla.appmanager.domain.res.componentpackage.LaunchBuildComponentHandlerRes
import com.alibaba.tesla.appmanager.server.factory.JinjaFactory
import com.alibaba.tesla.appmanager.server.service.componentpackage.handler.BuildComponentHandler
import com.alibaba.tesla.appmanager.server.service.imagebuilder.ImageBuilderService
import com.alibaba.tesla.appmanager.server.storage.Storage
import okhttp3.HttpUrl
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

/**
 * 默认 ProductOps Internal Addon Groovy Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
class DefaultInternalAddonProductopsHandler implements BuildComponentHandler {

    private static final Logger log = LoggerFactory.getLogger(DefaultBuildJobHandler.class)

    /**
     * 当前内置 Handler 类型
     */
    public static final String KIND = DynamicScriptKindEnum.BUILD_IA_PRODUCTOPS_COMPONENT.toString()

    /**
     * 当前内置 Handler 名称
     */
    public static final String NAME = DefaultConstant.DEFAULT_GROOVY_HANDLER

    /**
     * 当前内置 Handler 版本
     */
    public static final Integer REVISION = 15

    /**
     * 弹内导出应用需要排除的应用 ID
     */
    private static final List<String> EXCLUDE_APPS = Arrays.asList(
            "pai",
            "fuxi",
            "pangu",
            "tianji",
            "tesla"
    )

    private static final String TEMPLATE_INTERNAL_ADDON_PRODUCTOPS_FILENAME = "default_internal_addon_productops.tpl"
    private static final String EXPORT_TMP_FILE = "productops_tmp_export.zip"

    @Autowired
    private PackageProperties packageProperties

    @Autowired
    private Storage storage

    @Autowired
    private ImageBuilderService imageBuilderService

    /**
     * 构建一个实体 Component Package
     *
     * @param request ComponentPackage 创建任务对象
     * @return 实体包信息
     */
    @Override
    LaunchBuildComponentHandlerRes launch(BuildComponentHandlerReq request) {
        def appId = request.getAppId()
        def componentType = request.getComponentType()
        def componentName = request.getComponentName()
        def namespaceId = request.getNamespaceId()
        def stageId = request.getStageId()
        def version = request.getVersion()
        def options = request.getOptions()

        // 创建当前组件包的临时组装目录，用于存储 meta 信息及构建后的镜像
        def packageDir
        try {
            packageDir = Files.createTempDirectory("appmanager_component_package_")
            packageDir.toFile().deleteOnExit()
        } catch (IOException e) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, "cannot create temp directory", e)
        }

        // 如果已经有了远端包，那么直接拉取这个 zip 文件
        def exportPath = Paths.get(packageDir.toString(), EXPORT_TMP_FILE)
        def remoteFile = options.getString("remoteFile")
        if (!StringUtils.isEmpty(remoteFile)) {
            FileUtils.copyURLToFile(new URL(remoteFile), exportPath.toFile(), 10 * 1000, 60 * 1000)
        } else {
            // 准备参数并导出数据
            String endpoint = getEndpoint(options)
            String envIds = getEnvId(namespaceId, stageId, options)
            def url = String.format("%s/maintainer/export/apps", endpoint)
            def params = new HashMap<String, String>()
            params.put("appIds", appId)
            params.put("envIds", envIds)
            def urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder()
            for (Map.Entry<String, String> entry : params.entrySet()) {
                urlBuilder.addQueryParameter(entry.getKey(), entry.getValue())
            }
            FileUtils.copyURLToFile(new URL(urlBuilder.build().toString()), exportPath.toFile(), 10 * 1000, 60 * 1000)
        }

        // 解压当前导出文件并清理
        ZipUtil.unzip(exportPath.toString(), packageDir.toString())
        FileUtils.deleteQuietly(exportPath.toFile())

        // 创建 meta.yaml 元信息存储到 packageDir 顶层目录中
        def jinjava = JinjaFactory.getJinjava()
        def template = getTemplate(TEMPLATE_INTERNAL_ADDON_PRODUCTOPS_FILENAME)
        def optionsClone = (JSONObject) options.clone()
        options.put("appId", appId)
        options.put("componentType", componentType)
        options.put("componentName", componentName)
        options.put("version", version)
        options.putIfAbsent("options", new JSONObject())
        options.getJSONObject("options").putIfAbsent("targetEndpoint", "")
        options.getJSONObject("options").putIfAbsent("multipleEnv", false)
        options.getJSONObject("options").putIfAbsent("stageId", "")
        options.getJSONObject("options").putAll(optionsClone)
        def metaYamlContent = jinjava.render(template, options)
        def metaYamlFile = Paths.get(packageDir.toString(), "meta.yaml").toFile()
        FileUtils.writeStringToFile(metaYamlFile, metaYamlContent, StandardCharsets.UTF_8)
        log.info("meta yaml config has rendered|appId={}|namespaceId={}|stageId={}|componentType={}|componentName={}|" +
                "packageVersion={}", appId, namespaceId, stageId, componentType, componentName, version)

        // 将 packageDir 打包为 zip 文件
        String zipPath = packageDir.resolve("app_package.zip").toString()
        Files.list(packageDir).map({ p -> p.toFile() }).forEach({ item ->
            if (item.isDirectory()) {
                ZipUtil.zipDirectory(zipPath, item)
            } else {
                ZipUtil.zipFile(zipPath, item)
            }
        })
        def targetFileMd5 = StringUtil.getMd5Checksum(zipPath)
        log.info("zip file has generated|appId={}|namespaceId={}|stageId={}|componentType={}|componentName={}|" +
                "packageVersion={}|zipPath={}|md5={}", appId, namespaceId, stageId, componentType, componentName,
                version, zipPath, targetFileMd5)

        // 上传导出包到 Storage 中
        String bucketName = packageProperties.getBucketName()
        String remotePath = PackageUtil
                .buildComponentPackageRemotePath(appId, componentType, componentName, version)
        storage.putObject(bucketName, remotePath, zipPath)
        log.info("component package has uploaded to storage|appId={}|namespaceId={}|stageId={}|bucketName={}|" +
                "remotePath={}|localPath={}", appId, namespaceId, stageId, bucketName, remotePath, zipPath)

        // 删除临时数据 (正常流程下)
        try {
            FileUtils.deleteDirectory(packageDir.toFile())
        } catch (Exception ignored) {
            log.warn("cannot delete component package build directory|directory={}", packageDir.toString())
        }
        LaunchBuildComponentHandlerRes res = LaunchBuildComponentHandlerRes.builder()
                .logContent("get zip file from productops succeed")
                .storageFile(new StorageFile(bucketName, remotePath))
                .packageMetaYaml(metaYamlContent)
                .packageMd5(targetFileMd5)
                .build()
        return res
    }

    /**
     * 获取指定名称的 resources 下的 Jinja 模板
     * @param templateName 模板名称
     * @return 模板内容
     */
    private static String getTemplate(String templateName) {
        def config = new ClassPathResource("jinja/" + templateName)
        return IOUtils.toString(config.getInputStream(), StandardCharsets.UTF_8)
    }

    /**
     * 获取 ProductOps 组件需要的 Endpoint (兼容各种历史问题)
     * @param options 应用选项
     * @return Endpoint String
     */
    private static String getEndpoint(JSONObject options) {
        def endpoint = "http://" + (StringUtils.isEmpty(System.getenv("ENDPOINT_PAAS_PRODUCTOPS"))
                ? "productops.gateway.tesla.alibaba-inc.com/"
                : System.getenv("ENDPOINT_PAAS_PRODUCTOPS"))
        if (StringUtils.isNotEmpty(options.getString("endpoint"))) {
            endpoint = options.getString("endpoint")
        }
        return endpoint
    }

    /**
     * 获取 EnvIds
     * @param namespaceId Namespace ID
     * @param stageId Stage ID
     * @param options 应用选项
     * @return
     */
    private static String getEnvId(String namespaceId, String stageId, JSONObject options) {
        def envIds = "prod"
        if (StringUtils.isNotEmpty(options.getString("envIds"))) {
            envIds = options.getString("envIds")
        } else if (StringUtils.isNotEmpty(namespaceId) && StringUtils.isNotEmpty(stageId)) {
            envIds = namespaceId + "," + stageId
        } else if (StringUtils.isNotEmpty(options.getString("namespaceId"))
                && StringUtils.isNotEmpty(options.getString("stageId"))) {
            envIds = options.getString("namespaceId") + "," + options.getString("stageId")
        }
        return envIds
    }
}
