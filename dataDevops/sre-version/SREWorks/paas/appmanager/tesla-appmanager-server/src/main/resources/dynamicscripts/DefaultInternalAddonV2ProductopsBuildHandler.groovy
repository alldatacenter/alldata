package dynamicscripts

import com.alibaba.fastjson.JSONObject
import com.alibaba.tesla.appmanager.autoconfig.PackageProperties
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode
import com.alibaba.tesla.appmanager.common.exception.AppException
import com.alibaba.tesla.appmanager.common.util.PackageUtil
import com.alibaba.tesla.appmanager.common.util.RequestUtil
import com.alibaba.tesla.appmanager.common.util.StringUtil
import com.alibaba.tesla.appmanager.domain.core.StorageFile
import com.alibaba.tesla.appmanager.domain.req.componentpackage.BuildComponentHandlerReq
import com.alibaba.tesla.appmanager.domain.res.componentpackage.LaunchBuildComponentHandlerRes
import com.alibaba.tesla.appmanager.server.service.componentpackage.handler.BuildComponentHandler
import com.alibaba.tesla.appmanager.server.storage.Storage
import com.hubspot.jinjava.Jinjava
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
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
 * 默认构建 Microservice Groovy Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
class ProductopsBuildHandler implements BuildComponentHandler {

    private static final Logger log = LoggerFactory.getLogger(ProductopsBuildHandler.class)

    /**
     * 当前内置 Handler 类型
     */
    public static final String KIND = DynamicScriptKindEnum.BUILD_IA_V2_PRODUCTOPS_COMPONENT.toString()

    /**
     * 当前内置 Handler 名称
     */
    public static final String NAME = DefaultConstant.DEFAULT_GROOVY_HANDLER

    /**
     * 当前内置 Handler 版本
     */
    public static final Integer REVISION = 16

    @Autowired
    private PackageProperties packageProperties

    @Autowired
    private Storage storage

    private static final String EXPORT_TMP_FILE = "content.json"

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
        def version = request.getVersion()
        def options = request.getOptions()
        def endpoint = options.getString("endpoint")

        if (StringUtils.isEmpty(endpoint)) {
            endpoint = "prod-flycore-paas-action"
            // endpoint未传入的时候，传入缺省的endpoint
            // throw new AppException(AppErrorCode.INVALID_USER_ARGS, "cannot find endpoint in build options")
        }

        // 创建当前组件包的临时组装目录，用于存储 meta 信息及构建后的镜像
        def packageDir
        try {
            packageDir = Files.createTempDirectory("appmanager_component_package_")
            packageDir.toFile().deleteOnExit()
        } catch (IOException e) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, "cannot create temp directory", e)
        }
        log.info("packageDir {}", packageDir.toString())
        // 扫描 package options，构建所有需要的镜像，并存储到 packageDir 中
        def logContent = new StringBuilder()
        options.put("appId", appId)
        options.put("componentType", componentType)
        options.put("componentName", componentName)
        options.put("version", version)
        log.info("all images have built||appId={}||componentType={}||componentName={}||packageVersion={}||",
                appId, componentType, componentName, version)

        // 创建 meta.yaml 元信息存储到 packageDir 顶层目录中
        def jinjava = new Jinjava()

        def template = getTemplate("default_internal_addon_v2_productops.tpl")
        def metaYamlContent = jinjava.render(template, options)
        def metaYamlFile = Paths.get(packageDir.toString(), "meta.yaml").toFile()
        FileUtils.writeStringToFile(metaYamlFile, metaYamlContent, StandardCharsets.UTF_8)

        // 支持远程读文件
        def exportPath = Paths.get(packageDir.toString(), EXPORT_TMP_FILE)
        def remoteFile = options.getString("remoteFile")
        if (!StringUtils.isEmpty(remoteFile)) {
            FileUtils.copyURLToFile(new URL(remoteFile), exportPath.toFile(), 10 * 1000, 60 * 1000)
        } else {
            def response = RequestUtil.get("http://" + endpoint + "/frontend/exImport/export?stageId=dev&appId=" + appId, JSONObject.class)
            log.info("response from action: {}", response.toJSONString())
            def contentJsonContent = response.getString("data")
            def contentJsonFile = Paths.get(packageDir.toString(), "content.json").toFile()
            FileUtils.writeStringToFile(contentJsonFile, contentJsonContent, StandardCharsets.UTF_8)
        }

        log.info("meta yaml config has rendered||appId={}||componentType={}||componentName={}||packageVersion={}",
                appId, componentType, componentName, version)

        // 将 packageDir 打包为 zip 文件
        String zipPath = packageDir.resolve("app_package.zip").toString()
        def zp = new ZipParameters()
        zp.setIncludeRootFolder(false)
        new ZipFile(zipPath).addFolder(new File(packageDir.toString()), zp)
        def targetFileMd5 = StringUtil.getMd5Checksum(zipPath)
        log.info("zip file has generated||appId={}||componentType={}||componentName={}||packageVersion={}||" +
                "zipPath={}||md5={}", appId, componentType, componentName, version,
                zipPath, targetFileMd5)

        // 上传导出包到 Storage 中
        String bucketName = packageProperties.getBucketName()
        String remotePath = PackageUtil
                .buildComponentPackageRemotePath(appId, componentType, componentName, version)
        storage.putObject(bucketName, remotePath, zipPath)
        log.info("component package has uploaded to storage||bucketName={}||" +
                "remotePath={}||localPath={}", bucketName, remotePath, zipPath)

        // 删除临时数据 (正常流程下)
        try {
            FileUtils.deleteDirectory(packageDir.toFile())
        } catch (Exception ignored) {
            log.warn("cannot delete component package build directory||directory={}", packageDir.toString())
        }
        LaunchBuildComponentHandlerRes res = LaunchBuildComponentHandlerRes.builder()
                .logContent(logContent.toString())
                .storageFile(new StorageFile(bucketName, remotePath))
                .packageMetaYaml(metaYamlContent)
                .packageMd5(targetFileMd5)
                .build()
        return res
    }


    private static String getTemplate(String templateName) {
        def config = new ClassPathResource("jinja/" + templateName)
        return IOUtils.toString(config.getInputStream(), StandardCharsets.UTF_8)
    }

}
