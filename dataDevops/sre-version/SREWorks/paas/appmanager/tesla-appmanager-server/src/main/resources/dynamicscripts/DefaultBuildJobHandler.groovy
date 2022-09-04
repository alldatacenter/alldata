package dynamicscripts

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.TypeReference
import com.alibaba.tesla.appmanager.autoconfig.PackageProperties
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode
import com.alibaba.tesla.appmanager.common.exception.AppException
import com.alibaba.tesla.appmanager.common.util.PackageUtil
import com.alibaba.tesla.appmanager.common.util.StringUtil
import com.alibaba.tesla.appmanager.common.util.ZipUtil
import com.alibaba.tesla.appmanager.domain.core.ImageTar
import com.alibaba.tesla.appmanager.domain.core.StorageFile
import com.alibaba.tesla.appmanager.domain.req.componentpackage.BuildComponentHandlerReq
import com.alibaba.tesla.appmanager.domain.req.imagebuilder.ImageBuilderCreateReq
import com.alibaba.tesla.appmanager.domain.res.componentpackage.LaunchBuildComponentHandlerRes
import com.alibaba.tesla.appmanager.server.service.componentpackage.handler.BuildComponentHandler
import com.alibaba.tesla.appmanager.server.service.imagebuilder.ImageBuilderService
import com.alibaba.tesla.appmanager.server.storage.Storage
import com.hubspot.jinjava.Jinjava
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

/**
 * 默认构建 Job Groovy Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
class DefaultBuildJobHandler implements BuildComponentHandler {

    private static final Logger log = LoggerFactory.getLogger(DefaultBuildJobHandler.class)

    /**
     * 当前内置 Handler 类型
     */
    public static final String KIND = DynamicScriptKindEnum.BUILD_JOB_COMPONENT.toString()

    /**
     * 当前内置 Handler 名称
     */
    public static final String NAME = DefaultConstant.DEFAULT_GROOVY_HANDLER

    /**
     * 当前内置 Handler 版本
     */
    public static final Integer REVISION = 13

    private static final String TEMPLATE_JOB_FILENAME = "default_job.tpl"

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
        log.info("default build job request|{}", JSONObject.toJSONString(request))
        def appId = request.getAppId()
        def componentType = request.getComponentType()
        def componentName = request.getComponentName()
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

        // 针对 env 进行去重
        def envList = options.getJSONArray("env")
        if (envList != null) {
            def envSet = new HashSet<String>()
            for (Object envObj : envList) {
                envSet.add(String.valueOf(envObj))
            }
            options.put("env", JSONArray.parseArray(JSONArray.toJSONString(new ArrayList<String>(envSet))))
        }

        // 扫描 package options，构建所有需要的镜像，并存储到 packageDir 中
        def logContent = new StringBuilder()
        def imageTarList = buildImages(packageDir, request, logContent)
        options.put("imageTarList", imageTarList)
        options.put("appId", appId)
        options.put("componentType", componentType)
        options.put("componentName", componentName)
        options.put("version", version)
        log.info("all images have built|appId={}|componentType={}|componentName={}|packageVersion={}|" +
                "imageTarList={}", appId, componentType, componentName, version,
                JSONArray.toJSONString(imageTarList))

        // 创建 meta.yaml 元信息存储到 packageDir 顶层目录中
        def jinjava = new Jinjava()
        def template = getTemplate(TEMPLATE_JOB_FILENAME)
        def metaYamlContent = jinjava.render(template, options)
        def metaYamlFile = Paths.get(packageDir.toString(), "meta.yaml").toFile()
        FileUtils.writeStringToFile(metaYamlFile, metaYamlContent, StandardCharsets.UTF_8)
        log.info("meta yaml config has rendered|appId={}|componentType={}|componentName={}|packageVersion={}",
                appId, componentType, componentName, version)

        // 将 packageDir 打包为 zip 文件
        String zipPath = packageDir.resolve("app_package.zip").toString()
        List<File> subfiles = Files.walk(packageDir).map({ p -> p.toFile() }).collect(Collectors.toList())
        ZipUtil.zipFiles(zipPath, subfiles)
        def targetFileMd5 = StringUtil.getMd5Checksum(zipPath)
        log.info("zip file has generated|appId={}|componentType={}|componentName={}|packageVersion={}|" +
                "zipPath={}|md5={}", appId, componentType, componentName, version,
                zipPath, targetFileMd5)

        // 上传导出包到 Storage 中
        String bucketName = packageProperties.getBucketName()
        String remotePath = PackageUtil
                .buildComponentPackageRemotePath(appId, componentType, componentName, version)
        storage.putObject(bucketName, remotePath, zipPath)
        log.info("component package has uploaded to storage|bucketName={}|" +
                "remotePath={}|localPath={}", bucketName, remotePath, zipPath)

        // 删除临时数据 (正常流程下)
        try {
            FileUtils.deleteDirectory(packageDir.toFile())
        } catch (Exception ignored) {
            log.warn("cannot delete component package build directory|directory={}", packageDir.toString())
        }
        LaunchBuildComponentHandlerRes res = LaunchBuildComponentHandlerRes.builder()
                .logContent(logContent.toString())
                .storageFile(new StorageFile(bucketName, remotePath))
                .packageMetaYaml(metaYamlContent)
                .packageMd5(targetFileMd5)
                .build()
        log.info("default build microservice res|{}", JSONObject.toJSONString(res))
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
     * 根据 package options 构建需要的镜像，并填充 job 中的 image 字段 + 顶层 images 参数
     * @param packageDir 打包目录
     * @param packageOptions 构建选项
     * @return 镜像文件相对 packageDir 的路径列表
     */
    private List<ImageTar> buildImages(Path packageDir, BuildComponentHandlerReq request, StringBuilder logContent) {
        def options = request.getOptions()
        List<ImageTar> imageTarList = new ArrayList<>()
        if (options.containsKey("arch")) {
            def archObj = options.getJSONObject("arch")
            for (def arch : archObj.keySet()) {
                def buildObj = archObj.getJSONObject(arch)
                def job = buildObj.getJSONObject("job")
                imageTarList.addAll(buildContainerImage(arch, packageDir, request, job, logContent))
            }
        } else {
            def job = options.getJSONObject("job")
            imageTarList.addAll(buildContainerImage("", packageDir, request, job, logContent))
        }
        return imageTarList
    }

    /**
     * 根据 package options 构建给定的 containers 参数列表中的镜像
     * @param arch Arch
     * @param packageDir 打包目录
     * @param packageOptions 构建选项
     * @param containers 容器列表
     * @param imageTarList 镜像 tar 文件相对于 imageDir 的相对路径列表
     * @return
     */
    private List<ImageTar> buildContainerImage(
            String arch, Path packageDir, BuildComponentHandlerReq request,
            JSONObject container, StringBuilder logContent) {
        def imageTarList = new ArrayList<ImageTar>()
        def build = container.getJSONObject("build")
        def response = imageBuilderService.build(ImageBuilderCreateReq.builder()
                .arch(arch)
                .appId(request.getAppId())
                .componentName(request.getComponentName())
                .basename(container.getString("name"))
                .useExistImage(build.getString("useExistImage"))
                .imagePush(build.getBooleanValue("imagePush"))
                .imagePushRegistry(build.getString("imagePushRegistry"))
                .imagePushUseBranchAsTag(build.getBoolean("imagePushUseBranchAsTag"))
                .imageName(build.getString("imageName"))
                .repo(build.getString("repo"))
                .branch(build.getString("branch"))
                .commit(build.getString("commit"))
                .repoPath(build.getString("repoPath"))
                .ciAccount(build.getString("ciAccount"))
                .ciToken(build.getString("ciToken"))
                .dockerfileTemplate(build.getString("dockerfileTemplate"))
                .dockerfileTemplateArgs(JSON.parseObject(
                        build.getJSONObject("dockerfileTemplateArgs").toString(),
                        new TypeReference<Map<String, String>>() {}))
                .args(JSON.parseObject(
                        build.getJSONObject("args").toString(),
                        new TypeReference<Map<String, String>>() {}))
                .build())
        def result = response.get()
        def imageName = result.getImageName()
        def sha256 = result.getSha256()
        logContent.append(result.getLogContent())
        container.remove("build")
        container.put("image", imageName)

        if (!build.getBooleanValue("imagePush")) {
            // 当没有进行 imagePush 的时候，需要将镜像存储到 packageDir 中
            def imagePath = result.getImagePath()
            def targetPath = Paths.get(packageDir.toString(), FilenameUtils.getName(imagePath))
            Files.move(Paths.get(imagePath), targetPath)
            imageTarList.add(ImageTar.builder()
                    .arch(arch)
                    .name(FilenameUtils.getName(imagePath))
                    .image(imageName)
                    .sha256(sha256)
                    .build())
            log.info("move image from {} to {}|image={}|sha256={}", imagePath, targetPath.toString(), imageName, sha256)
            try {
                FileUtils.deleteDirectory(Paths.get(result.getImageDir()).toFile())
            } catch (Exception ignored) {
                log.warn("cannot delete directory after docker build|directory={}", result.getImageDir())
            }
            log.info("temp docker build directory {} has deleted", result.getImageDir())
        } else {
            // 否则仅存储 imageName 数据到 imageTarList 中
            imageTarList.add(ImageTar.builder()
                    .arch(arch)
                    .image(imageName)
                    .sha256(sha256)
                    .build())
            log.info("image has append into imageTarList|imageName={}|sha256={}", imageName, sha256)
        }
        return imageTarList
    }
}
