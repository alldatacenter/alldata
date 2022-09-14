package com.alibaba.tesla.appmanager.server.service.pack.dag;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.autoconfig.PackageProperties;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.constants.PackAppPackageVariableKey;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.PackageUtil;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.common.util.StringUtil;
import com.alibaba.tesla.appmanager.common.util.ZipUtil;
import com.alibaba.tesla.appmanager.domain.core.StorageFile;
import com.alibaba.tesla.appmanager.domain.schema.AppPackageSchema;
import com.alibaba.tesla.appmanager.server.repository.AppPackageRepository;
import com.alibaba.tesla.appmanager.server.repository.AppPackageTagRepository;
import com.alibaba.tesla.appmanager.server.repository.ComponentPackageRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTagDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDO;
import com.alibaba.tesla.appmanager.server.storage.Storage;
import com.alibaba.tesla.dag.common.BeanUtil;
import com.alibaba.tesla.dag.local.AbstractLocalNodeBase;
import com.alibaba.tesla.dag.model.domain.dagnode.DagInstNodeRunRet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 打包实体的 AppPackage 并放置到 Storage 存储中
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class PackAppPackageToStorageNode extends AbstractLocalNodeBase {

    public static Integer runTimeout = 3600;

    @Override
    public DagInstNodeRunRet run() throws Exception {
        PackageProperties packageProperties = BeanUtil.getBean(PackageProperties.class);
        Path folder = Files.createTempDirectory(null);
        try {
            doPack(folder);
        } finally {
            FileUtils.deleteDirectory(folder.toFile());
        }
        return DagInstNodeRunRet.builder().build();
    }

    private void doPack(Path folder) throws Exception {
        AppPackageRepository appPackageRepository = BeanUtil.getBean(AppPackageRepository.class);
        AppPackageTagRepository appPackageTagRepository = BeanUtil.getBean(AppPackageTagRepository.class);
        ComponentPackageRepository componentPackageRepository = BeanUtil.getBean(ComponentPackageRepository.class);
        Storage storage = BeanUtil.getBean(Storage.class);
        PackageProperties packageProperties = BeanUtil.getBean(PackageProperties.class);
        assert appPackageRepository != null
                && componentPackageRepository != null
                && storage != null
                && packageProperties != null;

        long appPackageId = globalVariable.getLongValue(PackAppPackageVariableKey.APP_PACKAGE_ID);
        String componentPackageIdList = globalVariable.getString(PackAppPackageVariableKey.COMPONENT_PACKAGE_ID_LIST);
        if (appPackageId <= 0 || StringUtils.isEmpty(componentPackageIdList)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    "empty appPackageId/componentPackageIdList parameter");
        }

        AppPackageDO appPackageDO = appPackageRepository.getByCondition(AppPackageQueryCondition.builder()
                .id(appPackageId)
                .withBlobs(true)
                .build());
        if (appPackageDO == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find app package by id %d", appPackageId));
        }
        List<AppPackageTagDO> tags = appPackageTagRepository.query(Collections.singletonList(appPackageId));
        String appId = appPackageDO.getAppId();
        String appPackageVersion = appPackageDO.getPackageVersion();
        AppPackageSchema appPackageSchema = AppPackageSchema.builder()
                .appId(appId)
                .packageVersion(appPackageVersion)
                .packageCreator(appPackageDO.getPackageCreator())
                .componentPackages(new ArrayList<>())
                .tags(tags.stream().map(AppPackageTagDO::getTag).collect(Collectors.toList()))
                .build();
        for (String componentPackageIdStr : componentPackageIdList.split(",")) {
            long componentPackageId = Long.parseLong(componentPackageIdStr);
            ComponentPackageQueryCondition condition = ComponentPackageQueryCondition.builder()
                    .id(componentPackageId)
                    .withBlobs(true)
                    .build();
            ComponentPackageDO componentPackageDO = componentPackageRepository.getByCondition(condition);
            if (componentPackageDO == null) {
                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        String.format("cannot find component package by id %d", componentPackageId));
            }
            StorageFile storageFile = new StorageFile(componentPackageDO.getPackagePath());
            String bucketName = storageFile.getBucketName();
            String objectName = storageFile.getObjectName();
            String componentUrl = storage.getObjectUrl(bucketName, objectName, DefaultConstant.DEFAULT_FILE_EXPIRATION);
            String basename = String.format("%s_%s.zip",
                    componentPackageDO.getComponentType(), componentPackageDO.getComponentName());
            String filename = folder.resolve(basename).toString();

            // Component Package 包下载到本地目录中
            FileUtils.copyURLToFile(new URL(componentUrl), new File(filename));
            appPackageSchema.getComponentPackages().add(AppPackageSchema.ComponentPackageItem.builder()
                    .componentType(componentPackageDO.getComponentType())
                    .componentName(componentPackageDO.getComponentName())
                    .packageVersion(componentPackageDO.getPackageVersion())
                    .relativePath(basename)
                    .packageCreator(componentPackageDO.getPackageCreator())
                    .packageMd5(componentPackageDO.getPackageMd5())
                    .packageAddon(componentPackageDO.getPackageAddon())
                    .packageOptions(componentPackageDO.getPackageOptions())
                    .packageExt(componentPackageDO.getComponentSchema())
                    .build());
            log.info("component package {} has downloaded into folder {}|appPackageId={}|componentPackageId={}",
                    filename, folder, appPackageId, componentPackageId);
        }

        // 将 app package schema 一同放入目录中作为 meta.yaml 进行打包
        String schemaContent = SchemaUtil.toYamlMapStr(appPackageSchema);
        FileOutputStream outputStream = new FileOutputStream(folder.resolve("meta.yaml").toString());
        outputStream.write(schemaContent.getBytes());
        outputStream.close();
        log.info("app package schema has generated|appPackageId={}|content={}",
                appPackageDO, JSONObject.toJSONString(appPackageSchema));

        // 生成压缩文件并存储到 Storage 中
        String zipPath = folder.resolve("app_package.zip").toString();
        List<File> subfiles = Files.walk(folder).map(Path::toFile).collect(Collectors.toList());
        ZipUtil.zipFiles(zipPath, subfiles);
        String bucketName = packageProperties.getBucketName();
        String remotePath = PackageUtil.buildAppPackageRemotePath(appId, appPackageVersion);
        storage.putObject(bucketName, remotePath, zipPath);
        log.info("app package has uploaded to storage|appPackageId={}|bucketName={}|" +
                "remotePath={}|localPath={}", appPackageId, bucketName, remotePath, zipPath);

        // 填充 app package 的 DB 数据
        StorageFile appPackageStorageFile = new StorageFile();
        appPackageStorageFile.setBucketName(bucketName);
        appPackageStorageFile.setObjectName(remotePath);
        String packagePath = appPackageStorageFile.toPath();
        String md5 = StringUtil.getMd5Checksum(zipPath);
        appPackageDO.setPackagePath(packagePath);
        appPackageDO.setPackageMd5(md5);
        appPackageDO.setAppSchema(SchemaUtil.toYamlMapStr(appPackageSchema));
        appPackageRepository.updateByPrimaryKeySelective(appPackageDO);
        log.info("app package has updated package info in db|appPackageId={}|packagePath={}|md5={}",
                appPackageId, packagePath, md5);
    }
}
