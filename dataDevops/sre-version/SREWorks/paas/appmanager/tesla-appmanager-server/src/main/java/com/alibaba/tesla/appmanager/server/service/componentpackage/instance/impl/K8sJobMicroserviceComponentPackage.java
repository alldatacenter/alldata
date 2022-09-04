package com.alibaba.tesla.appmanager.server.service.componentpackage.instance.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.autoconfig.ImageBuilderProperties;
import com.alibaba.tesla.appmanager.autoconfig.PackageProperties;
import com.alibaba.tesla.appmanager.autoconfig.SystemProperties;
import com.alibaba.tesla.appmanager.common.enums.AppPackageTaskStatusEnum;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.*;
import com.alibaba.tesla.appmanager.domain.core.StorageFile;
import com.alibaba.tesla.appmanager.server.event.componentpackage.*;
import com.alibaba.tesla.appmanager.server.event.loader.ComponentPackageLoadEvent;
import com.alibaba.tesla.appmanager.server.repository.ComponentPackageRepository;
import com.alibaba.tesla.appmanager.server.repository.ComponentPackageTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDO;
import com.alibaba.tesla.appmanager.server.service.componentpackage.KanikoInformerFactory;
import com.alibaba.tesla.appmanager.server.service.componentpackage.instance.ComponentPackageBase;
import com.alibaba.tesla.appmanager.server.service.componentpackage.instance.util.RetryUtil;
import com.alibaba.tesla.appmanager.server.storage.Storage;
import com.hubspot.jinjava.Jinjava;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @ClassName: K8sJobMicroserviceComponentPackage
 * @Author: dyj
 * @DATE: 2021-03-09
 * @Description:
 **/
@EnableRetry(proxyTargetClass = true)
@Service("K8sJobMicroserviceComponentPackage")
@Slf4j
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class K8sJobMicroserviceComponentPackage implements ComponentPackageBase, ApplicationListener<KanikoPodEvent> {
    private static final ComponentTypeEnum COMPONENT_TYPE = ComponentTypeEnum.K8S_JOB;
    private static final String HTTP_PREFIX = "http://";
    private static final String HTTPS_PREFIX = "https://";
    private static final String VOLUME_PATH = "/app/kaniko/";
    private static final String KANIKO_TPL_PATH = "kaniko/kaniko.yaml.tpl";
    private static final String TEMPLATE_JOB_FILENAME = "jinja/default_job.tpl";
    private static final HashMap<Long, Set<String>> taskPodMap = new HashMap<>();
    private static final HashMap<Long, Set<String>> succeedPodMap = new HashMap<>();
    private static final HashMap<Long, String> taskTargetDirMap = new HashMap<>();
    private static final String flag = new String();
    private CoreV1Api api;

    @Autowired
    private PackageProperties packageProperties;
    @Autowired
    private SystemProperties systemProperties;
    @Autowired
    private ImageBuilderProperties imageBuilderProperties;
    @Autowired
    private KanikoInformerFactory kanikoInformerFactory;
    @Autowired
    private ApplicationEventPublisher publisher;
    @Autowired
    private ComponentPackageRepository componentPackageRepository;
    @Autowired
    private ComponentPackageTaskRepository componentPackageTaskRepository;
    @Autowired
    private Storage storage;

    /**
     * 初始化，注册自身
     */
    @PostConstruct
    public void init() {
        this.api = kanikoInformerFactory.getDefaultApi();
        publisher.publishEvent(new ComponentPackageLoadEvent(
                this, COMPONENT_TYPE.name(), this.getClass().getSimpleName()));
    }

    /**
     * 导出 component package 到 zip
     *
     * @param taskDO
     */
    @Override
    public void exportComponentPackage(ComponentPackageTaskDO taskDO) throws Exception {
        long tag = System.currentTimeMillis();
        String relativePath = taskDO.getAppId() + "/" + taskDO.getComponentName() + tag + "/";
        String targetFileDir = VOLUME_PATH + relativePath;
        FileUtil.createDir(targetFileDir, true);
        JSONObject packageOptions = JSONObject.parseObject(taskDO.getPackageOptions());

        //1. generate pod name
        JSONArray containers = loadContainer(packageOptions);
        Set<String> podNameSet = new HashSet<>();
        for (int index = 0; index < containers.size(); index++) {
            JSONObject container = containers.getJSONObject(index);
            KanikoBuildCheckUtil.buildCheck(container.getJSONObject("build"));
            container.put("image", generateImageName(taskDO, container, Long.toString(tag)));
            Object nameOb = JsonUtil.recursiveGetParameter(container, Collections.singletonList("name"));
            String containerName = nameOb != null ? nameOb.toString() : "default";
            String podName = genPodName(taskDO.getAppId(), taskDO.getComponentName(), containerName, taskDO.getId(), tag);
            podNameSet.add(podName);
        }
        //2. generate meta.yaml
        generateMetaYaml(taskDO, packageOptions, targetFileDir);

        //3. insert informer
        waitBuildPodAndPackage(podNameSet, taskDO, targetFileDir);

        //4. build pod
        String podTpl = loadClassPathFile(KANIKO_TPL_PATH);
        for (int index = 0; index < containers.size(); index++) {
            JSONObject container = containers.getJSONObject(index);
            try {
                String existImage = JsonUtil.recursiveGetString(container, Arrays.asList("build", "useExistImage"));
                if (!container.getJSONObject("build").getBooleanValue("imagePush") && !StringUtils.isEmpty(existImage)) {
                    String podName = genPodName(taskDO.getAppId(), taskDO.getComponentName(), existImage, taskDO.getId(), tag);
                    publisher.publishEvent(new SucceedKanikoPodEvent(this, podName));
                } else {
                    buildImage(api, container, podTpl, taskDO, relativePath, tag);
                }
            } catch (Exception e) {
                log.error("action=buildImage|| can not build {} container image,Message:{} Exception:{}", container.getString("name"), e.getMessage(), e.getCause());
                String podName = genPodName(taskDO.getAppId(), taskDO.getComponentName(), container.getString("name"),
                        taskDO.getId(), tag);
                publisher.publishEvent(new DeleteKanikoPodEvent(this, podName));
                updateDataFailRecord(taskDO.getId(), e.getMessage());
            }

        }
    }

    private String loadClassPathFile(String filePath) throws IOException {
        Resource config = new ClassPathResource(filePath);
        InputStream inputStream = config.getInputStream();
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }

    public String fileToString(String filePath) throws IOException {
        if (filePath.endsWith(File.separator)) {
            throw new AppException(AppErrorCode.USER_CONFIG_ERROR, "action=fileToString|| can not read dir!");
        }
        File file = new File(filePath);
        FileInputStream fileInputStream = new FileInputStream(file);
        return IOUtils.toString(fileInputStream, StandardCharsets.UTF_8);
    }

    private JSONArray loadContainer(JSONObject packageOptions) {
        JSONArray containers = new JSONArray();
        containers.add(packageOptions.getJSONObject("job"));
        if (containers.isEmpty()) {
            log.warn("The build yaml have no build contains!");
            throw new AppException(AppErrorCode.USER_CONFIG_ERROR, "action=loadContainer|| The job build yaml must have job build config!");
        }
        return containers;
    }

    private String generateImageName(ComponentPackageTaskDO taskDO, JSONObject container, String tag) {
        String existImage = JsonUtil.recursiveGetString(container, Arrays.asList("build", "useExistImage"));
        if (!container.getJSONObject("build").getBooleanValue("imagePush") && !StringUtils.isEmpty(existImage)) {
            return existImage;
        }
        String imageBaseName = String.format("%s-%s-%s", taskDO.getAppId(), taskDO.getComponentName(),
                container.get("name"));
        String image = imageBaseName + ":" + tag;
        String destination = JsonUtil.recursiveGetString(container, Arrays.asList("build", "imagePushRegistry"));
        if (StringUtils.isEmpty(destination)) {
            throw new AppException(AppErrorCode.USER_CONFIG_ERROR,
                    "Can not find imagePushRegistry from build.yaml");
        }
        return destination.concat("/").concat(image);
    }

    private void buildImage(CoreV1Api api, JSONObject container, String podTpl,
                            ComponentPackageTaskDO taskDO, String relativePath, long tag)
            throws Exception {
        Object nameOb = JsonUtil.recursiveGetParameter(container,
                Collections.singletonList("name"));
        String containerName = nameOb != null ? nameOb.toString() : "default";

        // 渲染 dockerfileTpl
        // 1. 下载 repo
        RetryUtil.getRetryClient().execute(retryContext -> gitClone(VOLUME_PATH + relativePath + containerName, container));

        Object repoPathPara = JsonUtil.recursiveGetParameter(container, Arrays.asList("build", "repoPath"));
        String repoPath = repoPathPara == null ? null : repoPathPara.toString();
        String buildAbsolutePath = VOLUME_PATH + relativePath + containerName + "/";
        if (!StringUtils.isEmpty(repoPath)) {
            if (repoPath.endsWith("/")) {
                buildAbsolutePath = buildAbsolutePath + repoPath;
            } else {
                buildAbsolutePath = buildAbsolutePath + repoPath + "/";
            }
        }
        // 2. 读取dockertemplateFile
        String tplName = JsonUtil.recursiveGetString(container, Arrays.asList("build", "dockerfileTemplate"));
        assert !StringUtils.isEmpty(tplName);
        String dockerFileName = tplName;
        if (tplName.endsWith(".tpl")) {
            dockerFileName = tplName.substring(0, tplName.length() - 4);
        }
        File dockerTemplateFile = new File(buildAbsolutePath + tplName);
        if (!dockerTemplateFile.exists()) {
            throw new AppException(AppErrorCode.USER_CONFIG_ERROR,
                    "Can not find dockerfileTemplateFile:" + dockerTemplateFile.getAbsolutePath());
        }
        JSONObject dockerTemplateArgs = JSONObject.parseObject(JSONObject.toJSONString(
                JsonUtil.recursiveGetParameter(container, Arrays.asList("build", "dockerfileTemplateArgs"))));
        String dockerTemplateRaw = IOUtils.toString(new FileInputStream(dockerTemplateFile), StandardCharsets.UTF_8);
        String dockerFileStr = jinjaRender(dockerTemplateRaw, dockerTemplateArgs);
        // 3. 生成 dockerfile
        boolean b = FileUtil.writeStringToFile(buildAbsolutePath + dockerFileName, dockerFileStr, true);
        log.info("action=JinjaRender||dockerFileStr:{}", dockerFileStr);
        // 4. 打包成tar.gz （不打包 markdown 文件）
        String compressTarName = dockerFileName + ".tar.gz";
        String tarCommand = String.format("cd %s; tar -zcvf %s -C %s *", buildAbsolutePath, buildAbsolutePath + compressTarName, buildAbsolutePath);
        CommandUtil.runLocalCommand(tarCommand);

        String bucketName = packageProperties.getBucketName();
        String remotePath = PackageUtil
                .buildKanikoBuildRemotePath(taskDO.getAppId(), taskDO.getComponentType(), taskDO.getComponentName(), containerName,
                        taskDO.getPackageVersion());
        storage.putObject(bucketName, remotePath, buildAbsolutePath + compressTarName);
        StorageFile storageFile = new StorageFile(bucketName, remotePath);
        log.info("kaniko build package has uploaded to storage||componentPackageTaskId={}||bucketName={}||" +
                "remotePath={}||localPath={}", taskDO.getId(), bucketName, remotePath, buildAbsolutePath + compressTarName);

        // 5. 渲染 kaniko pod
        JSONObject parameters = new JSONObject();
        parameters.put("CONTEXT", storageFile.toPath());
        parameters.put("DOCKERFILE", dockerFileName);
        String podName = genPodName(taskDO.getAppId(), taskDO.getComponentName(), containerName, taskDO.getId(), tag);
        parameters.put("POD_NAME", podName);
        JSONObject buildArgs = JSONObject.parseObject(JSONObject.toJSONString(
                JsonUtil.recursiveGetParameter(container, Arrays.asList("build", "args"))));
        if (buildArgs != null) {
            JSONArray args = new JSONArray();
            for (String key : buildArgs.keySet()) {
                String arg = String.format("--build-arg=%s=%s", key, buildArgs.getString(key));
                args.add(arg);
            }
            parameters.put("BUILD_ARGS", args);
        }
        JSONArray imageTags = JSONArray.parseArray(JSONObject.toJSONString(
                JsonUtil.recursiveGetParameter(container, Arrays.asList("build", "imagePushTags"))));
        JSONArray destinations = new JSONArray();
        destinations.add(container.getString("image"));
        if (imageTags != null) {
            for (Object imageTag : imageTags) {
                destinations.add(generateImageName(taskDO, container, imageTag.toString()));
            }
        }
        parameters.put("DESTINATIONS", destinations);
        parameters.put("STORAGE_ACCESS_KEY", packageProperties.getAccessKey());
        parameters.put("STORAGE_SECRET_KEY", packageProperties.getSecretKey());
        parameters.put("STORAGE_ENDPOINT", packageProperties.getEndpointProtocol() + packageProperties.getEndpoint());
        parameters.put("KANIKO_IMAGE", packageProperties.getKanikoImage());
        String snapshotMode = JsonUtil.recursiveGetString(container, Arrays.asList("build", "snapshotMode"));
        if (StringUtils.isEmpty(snapshotMode)) {
            parameters.put("SNAPSHOTMODE", snapshotMode);
        }
        // set docker secret name
        if (StringUtils.isEmpty(JsonUtil.recursiveGetString(container, Arrays.asList("build", "dockerSecretName")))) {
            parameters.put("K8S_DOCKER_SECRET", systemProperties.getK8sDockerSecret());
        } else {
            parameters.put("K8S_DOCKER_SECRET", JsonUtil.recursiveGetString(container, Arrays.asList("build", "dockerSecretName")));
        }
        // 携带flag设定
        JSONArray configKanikoFlags = JSONArray.parseArray(JSONObject.toJSONString(
                JsonUtil.recursiveGetParameter(container, Arrays.asList("build", "configKanikoFlags"))));
        if (configKanikoFlags != null) {
            parameters.put("CONFIG_KANIKO_FLAGS", configKanikoFlags);
        }

        String render = jinjaRender(podTpl, parameters);
        log.info("actionName=buildImage-kaniko||parameters={}\nkaniko_pod.yaml:{}", parameters, render);
        V1Pod v1Pod = SchemaUtil.toSchema(V1Pod.class, render);
        try {
            api.deleteNamespacedPod(podName.toString(), systemProperties.getK8sNamespace(), null,
                    null, null, null, null, null);
        } catch (ApiException e) {
            log.info("clean pod");
        }
        api.createNamespacedPod(systemProperties.getK8sNamespace(), v1Pod, null, null, null);
    }

    private boolean gitClone(String localDir, JSONObject container) {
        FileUtil.createDir(localDir, true);

        Object ciAccountPara = JsonUtil.recursiveGetParameter(container, Arrays.asList("build", "ciAccount"));
        Object ciTokenPara = JsonUtil.recursiveGetParameter(container, Arrays.asList("build", "ciToken"));
        String ciAccount = ciAccountPara == null ? imageBuilderProperties.getDefaultCiAccount() : ciAccountPara.toString();
        String ciToken = ciTokenPara == null ? imageBuilderProperties.getDefaultCiToken() : ciTokenPara.toString();

        Object repoPara = JsonUtil.recursiveGetParameter(container, Arrays.asList("build", "repo"));
        String gitHttpRep;
        if (repoPara == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    "can not found git repo from the container yaml path: build.repo!");
        } else {
            String repo = repoPara.toString();
            if (repo.startsWith(HTTP_PREFIX)) {
                String rest = StringUtil.trimStringByString(repo, HTTP_PREFIX);
                gitHttpRep = String.format("%s%s:%s@%s", HTTP_PREFIX, ciAccount, ciToken, rest);
            } else if (repo.startsWith(HTTPS_PREFIX)) {
                String rest = StringUtil.trimStringByString(repo, HTTPS_PREFIX);
                gitHttpRep = String.format("%s%s:%s@%s", HTTPS_PREFIX, ciAccount, ciToken, rest);
            } else {
                throw new AppException(AppErrorCode.INVALID_USER_ARGS, "cannot generate authorized repo by string " + repo);
            }
        }
        String branch = String.valueOf(JsonUtil.recursiveGetParameter(container, Arrays.asList("build", "branch")));
        String gitCloneComannd = String.format("git clone -b %s %s %s", branch, gitHttpRep, localDir);
        CommandUtil.runLocalCommand(gitCloneComannd);
        Object commit = JsonUtil.recursiveGetParameter(container, Arrays.asList("build", "commit"));
        if (commit != null) {
            String resetCommit = String.format("cd %s; git reset --hard %s", localDir, commit);
            CommandUtil.runLocalCommand(resetCommit);
        }
        return true;
    }

    private String jinjaRender(String raw, JSONObject parameters) {
        Jinjava jinjava = new Jinjava();
        return jinjava.render(raw, parameters);
    }

    private void generateMetaYaml(ComponentPackageTaskDO taskDO,
                                  JSONObject packageOptions, String targetFileDir) throws IOException {

        packageOptions.put("appId", taskDO.getAppId());
        packageOptions.put("version", taskDO.getPackageVersion());
        packageOptions.put("componentType", taskDO.getComponentType());
        packageOptions.put("componentName", taskDO.getComponentName());

        String metaTpl = loadClassPathFile(TEMPLATE_JOB_FILENAME);

        String metaYamlContent = jinjaRender(metaTpl, packageOptions);
        FileUtil.writeStringToFile(targetFileDir + "meta.yaml", metaYamlContent, true);
    }

    private void waitBuildPodAndPackage(Set<String> podNameSet, ComponentPackageTaskDO taskDO, String targetFileDir) {
        log.info("actionName=waitBuildImage|| Pod: {}", podNameSet);
        taskPodMap.put(taskDO.getId(), podNameSet);
        taskTargetDirMap.put(taskDO.getId(), targetFileDir);
    }


    private void packageComponentZip(Long taskId, String taskLog, String targetFileDir) throws IOException {
        ComponentPackageTaskDO taskDO = componentPackageTaskRepository.getByCondition(
                ComponentPackageTaskQueryCondition.builder().id(taskId).build());
        String zipFilePath = targetFileDir + "component_package_task.zip";
        ZipFile zip = new ZipFile(zipFilePath);
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setIncludeRootFolder(false);
        zipParameters.setExcludeFileFilter(file -> {
            if (file.getName().endsWith(".tar")) {
                return false;
            }
            if (file.getName().equalsIgnoreCase("meta.yaml")) {
                return false;
            }
            return true;
        });
        try {
            zip.addFolder(new File(targetFileDir), zipParameters);
        } catch (ZipException e) {
            log.error("action=packageComponentZip|| fail to addFolder to zip!||message={}, Exception={}", e.getMessage(), e.getCause());
            throw new AppException(AppErrorCode.USER_CONFIG_ERROR, "actionName=SuccessComponentPackageTaskStateAction|| Can not create zip file:" + targetFileDir, e.getMessage(), e.getCause());
        }

        String targetFileMd5 = StringUtil.getMd5Checksum(zipFilePath);

        String bucketName = packageProperties.getBucketName();
        String remotePath = PackageUtil
                .buildComponentPackageRemotePath(taskDO.getAppId(), taskDO.getComponentType(), taskDO.getComponentName(),
                        taskDO.getPackageVersion());
        storage.putObject(bucketName, remotePath, zipFilePath);
        StorageFile storageFile = new StorageFile(bucketName, remotePath);
        log.info("component package has uploaded to storage||componentPackageTaskId={}||bucketName={}||" +
                "remotePath={}||localPath={}", taskDO.getId(), bucketName, remotePath, zipFilePath);

        // 增加 Component Package 包记录
        ComponentPackageDO componentPackageDO = ComponentPackageDO.builder()
                .appId(taskDO.getAppId())
                .componentType(taskDO.getComponentType())
                .componentName(taskDO.getComponentName())
                .packageVersion(taskDO.getPackageVersion())
                .packageCreator(taskDO.getPackageCreator())
                .packageMd5(targetFileMd5)
                .packagePath(storageFile.toPath())
                .packageOptions(taskDO.getPackageOptions())
                .componentSchema(fileToString(targetFileDir + "meta.yaml"))
                .build();
        taskDO.setPackagePath(storageFile.toPath());
        taskDO.setPackageMd5(targetFileMd5);
        updateDataSuccessRecord(componentPackageDO, taskDO, taskLog);
        log.info("component package task has inserted to db||componentPackageTaskId={}||" +
                        "componentPackageId={}||appId={}||componentType={}||componentName={}||version={}||md5={}",
                taskDO.getAppPackageTaskId(), componentPackageDO.getId(), taskDO.getAppId(), taskDO.getComponentType(),
                taskDO.getComponentName(), taskDO.getPackageVersion(), targetFileMd5);
    }

    /**
     * 事务内更新 Database 记录
     *
     * @param componentPackageDO Component Package 记录
     * @param taskDO             Component Package Task 记录
     * @param taskLog
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateDataSuccessRecord(ComponentPackageDO componentPackageDO, ComponentPackageTaskDO taskDO,
                                        String taskLog) {
        componentPackageRepository.insert(componentPackageDO);
        taskDO.setComponentPackageId(componentPackageDO.getId());
        taskDO.setTaskLog(taskLog);
        componentPackageTaskRepository.updateByCondition(taskDO, ComponentPackageTaskQueryCondition.builder().id(taskDO.getId()).build());
    }

    private void deleteDir(String targetFileDir) {
        if (StringUtils.isEmpty(targetFileDir)) {
            log.error("action=deleteDir|| dir is empty!");
            return;
        }
        String deleteCommand = String.format("rm -rf %s", targetFileDir);
        CommandUtil.runLocalCommand(deleteCommand);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDataFailRecord(Long taskId, String taskLog) {
        ComponentPackageTaskDO taskDO = componentPackageTaskRepository.getByCondition(
                ComponentPackageTaskQueryCondition.builder().id(taskId).build());
        if (taskDO.getTaskStatus().equalsIgnoreCase(AppPackageTaskStatusEnum.FAILURE.name())) {
            return;
        }
        taskDO.setTaskLog(taskLog);
        componentPackageTaskRepository.updateByCondition(taskDO, ComponentPackageTaskQueryCondition.builder().id(taskDO.getId()).build());
    }

    private String genPodName(String appId, String componentName, String containerName, Long taskId, long tag) {
        StringBuilder podName = new StringBuilder();
        podName.append("appmanager-build-").append(appId).append("-")
                .append(componentName).append("-")
                .append(containerName).append("-")
                .append(taskId).append("-")
                .append(tag);
        return podName.toString();
    }

    @Async
    @Override
    public void onApplicationEvent(KanikoPodEvent event) {
        String podName = event.getPodName();
        String[] split = podName.split("-");
        Long taskId = Long.valueOf(split[split.length - 2]);
        if (taskPodMap.containsKey(taskId)) {
            synchronized (flag) {
                switch (event.getCurrentStatus()) {
                    case SUCCEED: {
                        if (taskPodMap.containsKey(taskId)) {
                            if (!succeedPodMap.containsKey(taskId)) {
                                succeedPodMap.put(taskId, new HashSet<>());
                            }
                            succeedPodMap.get(taskId).add(podName);
                            if (taskPodMap.get(taskId).size() == succeedPodMap.get(taskId).size()) {
                                StringBuilder taskLog = new StringBuilder();
                                for (String runPod : taskPodMap.get(taskId)) {
                                    try {
                                        String podLog = api.readNamespacedPodLog(runPod, systemProperties.getK8sNamespace(),
                                                null, null, null, null, null, null, null, null, null);
                                        taskLog.append(podName).append("-log:\n").append(podLog).append("\n");
                                    } catch (ApiException e) {
                                        log.warn("action=KanikoSucceedOp||can not read pod log:{}", runPod);
                                    }
                                    try {
                                        api.deleteNamespacedPod(runPod, systemProperties.getK8sNamespace(), null,
                                                null, null, null, null, null);
                                    } catch (ApiException e) {
                                        log.warn("action=KanikoSucceedOp||clean pod error:{}", e.getResponseBody());
                                    }
                                }
                                try {
                                    taskPodMap.remove(taskId);
                                    log.info("action=clean build task informer:{}", taskId);
                                    succeedPodMap.remove(taskId);
                                    String targetFileDir = taskTargetDirMap.remove(taskId);
                                    packageComponentZip(taskId, taskLog.toString(), targetFileDir);
                                    deleteDir(targetFileDir);
                                    publisher.publishEvent(new SucceedComponentPackageTaskEvent(this, taskId));
                                } catch (Exception e) {
                                    log.error("action=waitBuildPodAndPackage|| something wrong when operate package zip!message={}||exception={}", e.getMessage(), e.getCause());
                                    publisher.publishEvent(new FailedComponentPackageTaskEvent(this, taskId));
                                }
                            }
                        }
                        break;
                    }
                    case FAILED: {
                        if (taskPodMap.containsKey(taskId)) {
                            StringBuilder taskLog = new StringBuilder();
                            try {
                                String podLog = api.readNamespacedPodLog(podName, systemProperties.getK8sNamespace(),
                                        null, null, null, null, null, null, null, null, null);
                                taskLog.append(podName).append("-log:\n").append(podLog).append("\n");
                            } catch (ApiException e) {
                                log.warn("action=KanikoFailedOp||can not read pod log:{}", podName);
                            }
                            for (String runPod : taskPodMap.get(taskId)) {
                                try {
                                    api.deleteNamespacedPod(runPod, systemProperties.getK8sNamespace(), null,
                                            null, null, null, null, null);
                                } catch (ApiException e) {
                                    log.warn("action=KanikoFailedOp||clean pod error:{}", e.getResponseBody());
                                }
                            }
                            try {
                                taskPodMap.remove(taskId);
                                log.info("action=clean build task informer:{}", taskId);
                                succeedPodMap.remove(taskId);
                                updateDataFailRecord(taskId, taskLog.toString());
                                String targetFileDir = taskTargetDirMap.remove(taskId);
                                deleteDir(targetFileDir);
                            } finally {
                                publisher.publishEvent(new FailedComponentPackageTaskEvent(this, taskId));
                            }
                        }
                        break;
                    }
                    case DELETE: {
                        if (taskPodMap.containsKey(taskId)) {
                            for (String runPod : taskPodMap.get(taskId)) {
                                try {
                                    api.deleteNamespacedPod(runPod, systemProperties.getK8sNamespace(), null,
                                            null, null, null, null, null);
                                } catch (ApiException e) {
                                    log.warn("action=KanikoFailedOp||clean pod error:{}", e.getResponseBody());
                                }
                            }
                            try {
                                taskPodMap.remove(taskId);
                                log.info("action=clean build task informer:{}", taskId);
                                succeedPodMap.remove(taskId);
                                String targetFileDir = taskTargetDirMap.remove(taskId);
                                deleteDir(targetFileDir);
                            } finally {
                                publisher.publishEvent(new FailedComponentPackageTaskEvent(this, taskId));
                            }
                        }
                        break;
                    }
                    default:
                }
            }
        }
    }
}
