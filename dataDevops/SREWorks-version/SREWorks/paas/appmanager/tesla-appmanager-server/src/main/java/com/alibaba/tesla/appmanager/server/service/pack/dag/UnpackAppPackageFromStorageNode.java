package com.alibaba.tesla.appmanager.server.service.pack.dag;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.autoconfig.PackageProperties;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.constants.UnpackAppPackageVariableKey;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.common.util.StringUtil;
import com.alibaba.tesla.appmanager.common.util.ZipUtil;
import com.alibaba.tesla.appmanager.domain.core.StorageFile;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageCreateByLocalFileReq;
import com.alibaba.tesla.appmanager.domain.schema.AppPackageSchema;
import com.alibaba.tesla.appmanager.server.repository.AppPackageRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDO;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageTagService;
import com.alibaba.tesla.appmanager.server.service.componentpackage.ComponentPackageService;
import com.alibaba.tesla.appmanager.server.storage.Storage;
import com.alibaba.tesla.dag.common.BeanUtil;
import com.alibaba.tesla.dag.local.AbstractLocalNodeBase;
import com.alibaba.tesla.dag.model.domain.dagnode.DagInstNodeRunRet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 将 AppPackage 从 Storage 存储中获取并解包为各个 Component Package，存储到 DB 中并再次上传 Component Package 到 Storage
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class UnpackAppPackageFromStorageNode extends AbstractLocalNodeBase {

    public static Integer runTimeout = 3600;

    @Override
    public DagInstNodeRunRet run() throws Exception {
        Path folder = Files.createTempDirectory(null);
        try {
            doPack(folder);
        } finally {
            FileUtils.deleteDirectory(folder.toFile());
        }
        return DagInstNodeRunRet.builder().build();
    }

    private void doPack(Path folder) throws Exception {
        AppPackageRepository appPackageRepository = getAppPackageRepository();
        AppPackageTagService appPackageTagService = getAppPackageTagService();
        ComponentPackageService componentPackageService = getComponentPackageService();
        Storage storage = getStorage();
        PackageProperties packageProperties = getPackageProperties();
        assert appPackageRepository != null
                && appPackageTagService != null
                && storage != null
                && componentPackageService != null
                && packageProperties != null;

        long appPackageId = globalVariable.getLongValue(UnpackAppPackageVariableKey.APP_PACKAGE_ID);
        if (appPackageId <= 0) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "empty appPackageId parameter");
        }
        AppPackageDO appPackageDO = appPackageRepository.getByCondition(AppPackageQueryCondition.builder()
                .id(appPackageId)
                .withBlobs(true)
                .build());
        if (appPackageDO == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find app package by id %d", appPackageId));
        }
        boolean force = globalVariable.getBooleanValue(UnpackAppPackageVariableKey.FORCE);
        boolean resetVersion = globalVariable.getBooleanValue(UnpackAppPackageVariableKey.RESET_VERSION);

        // 应用包下载
        String appId = appPackageDO.getAppId();
        String packageVersion = appPackageDO.getPackageVersion();
        StorageFile storageFile = new StorageFile(appPackageDO.getPackagePath());
        String packageLocalPath = folder.resolve("app_package.zip").toString();
        String url = storage.getObjectUrl(storageFile.getBucketName(), storageFile.getObjectName(),
                DefaultConstant.DEFAULT_FILE_EXPIRATION);
        copyURLToFile(url, packageLocalPath);
        String md5 = readFileMd5(packageLocalPath);
        log.info("app package has download from storage|appId={}|packageVersion={}|packageLocalPath={}|url={}|" +
                "md5={}", appId, packageVersion, packageLocalPath, url, md5);

        // 解析 meta 信息
        String metaStr = readMetaYaml(packageLocalPath, folder);
        AppPackageSchema appPackageSchema = SchemaUtil.toSchema(AppPackageSchema.class, metaStr);
        appPackageSchema.setAppId(appId);
        // 检查版本号与请求中携带的参数是否一致
        if (!resetVersion && !packageVersion.equals(appPackageSchema.getPackageVersion())) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("invalid package version provided in http request|request=%s|actual=%s",
                            packageVersion, appPackageSchema.getPackageVersion()));
        }
        List<Long> componentPackageIdList = new ArrayList<>();
        for (AppPackageSchema.ComponentPackageItem item : appPackageSchema.getComponentPackages()) {
            // 插入 component package 记录
            String zipFilePath = folder.resolve(item.getRelativePath()).toString();
            ComponentPackageDO componentPackageDO = componentPackageService.createByLocalFile(
                    ComponentPackageCreateByLocalFileReq.builder()
                            .appId(appId)
                            .componentPackageItem(item)
                            .localFilePath(zipFilePath)
                            .force(force)
                            .resetVersion(resetVersion)
                            .build());
            if (componentPackageDO.getId() == null) {
                throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                        String.format("cannot find component package id after update component package|" +
                                "componentPackage=%s", JSONObject.toJSONString(componentPackageDO)));
            }
            componentPackageIdList.add(componentPackageDO.getId());
        }

        // 增加应用包与组件包的关联数据
        componentPackageService.addComponentPackageRelation(appPackageDO.getAppId(), appPackageId, componentPackageIdList);

        // 更新 app package 数据
        long componentCount = appPackageSchema.getComponentPackages().size();
        appPackageDO.setComponentCount(componentCount);
        appPackageDO.setPackageMd5(md5);
        appPackageDO.setAppSchema(SchemaUtil.toYamlMapStr(appPackageSchema));
        appPackageRepository.updateByPrimaryKeySelective(appPackageDO);
        log.info("app package has updated|appPackageId={}|appId={}|packageVersion={}|componentCount={}",
                appPackageId, appId, packageVersion, componentCount);

        // 更新 app package tags
        List<String> tags = appPackageSchema.getTags();
        appPackageTagService.batchUpdate(appPackageId, tags);
        log.info("app package tags has updated|appPackageId={}|appId={}|tags={}",
                appPackageId, appId, JSONObject.toJSONString(tags));
    }

    public AppPackageRepository getAppPackageRepository() {
        return BeanUtil.getBean(AppPackageRepository.class);
    }

    public AppPackageTagService getAppPackageTagService() {
        return BeanUtil.getBean(AppPackageTagService.class);
    }

    public ComponentPackageService getComponentPackageService() {
        return BeanUtil.getBean(ComponentPackageService.class);
    }

    public Storage getStorage() {
        return BeanUtil.getBean(Storage.class);
    }

    public PackageProperties getPackageProperties() {
        return BeanUtil.getBean(PackageProperties.class);
    }

    public void copyURLToFile(String url, String packageLocalPath) throws IOException {
        FileUtils.copyURLToFile(new URL(url), new File(packageLocalPath));
    }

    public String readMetaYaml(String packageLocalPath, Path folder) throws IOException {
        // 应用包解压
        ZipUtil.unzip(packageLocalPath, folder.toString());
        log.info("app package has unzipped to directory|packageLocalPath={}|directory={}",
                packageLocalPath, folder);

        // 读取 meta 信息
        return new String(Files.readAllBytes(folder.resolve("meta.yaml")), StandardCharsets.UTF_8);
    }

    public String readFileMd5(String packageLocalPath) {
        return StringUtil.getMd5Checksum(packageLocalPath);
    }
}
