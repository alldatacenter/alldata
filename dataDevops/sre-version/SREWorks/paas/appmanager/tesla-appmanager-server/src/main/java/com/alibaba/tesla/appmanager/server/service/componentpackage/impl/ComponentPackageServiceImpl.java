package com.alibaba.tesla.appmanager.server.service.componentpackage.impl;

import com.alibaba.tesla.appmanager.autoconfig.PackageProperties;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.PackageUtil;
import com.alibaba.tesla.appmanager.common.util.VersionUtil;
import com.alibaba.tesla.appmanager.domain.core.StorageFile;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageCreateByLocalFileReq;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageNextVersionReq;
import com.alibaba.tesla.appmanager.domain.res.componentpackage.ComponentPackageNextVersionRes;
import com.alibaba.tesla.appmanager.server.repository.AppPackageComponentRelRepository;
import com.alibaba.tesla.appmanager.server.repository.ComponentPackageRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageComponentRelQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageComponentRelDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDO;
import com.alibaba.tesla.appmanager.server.service.componentpackage.ComponentPackageService;
import com.alibaba.tesla.appmanager.server.storage.Storage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

/**
 * 内部 - 组件包服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class ComponentPackageServiceImpl implements ComponentPackageService {

    private final ComponentPackageRepository componentPackageRepository;
    private final AppPackageComponentRelRepository relRepository;
    private final Storage storage;
    private final PackageProperties packageProperties;

    public ComponentPackageServiceImpl(
            ComponentPackageRepository componentPackageRepository, AppPackageComponentRelRepository relRepository,
            Storage storage, PackageProperties packageProperties) {
        this.componentPackageRepository = componentPackageRepository;
        this.relRepository = relRepository;
        this.storage = storage;
        this.packageProperties = packageProperties;
    }

    /**
     * 根据条件过滤组件包列表
     *
     * @param condition 过滤条件
     * @return List
     */
    @Override
    public Pagination<ComponentPackageDO> list(ComponentPackageQueryCondition condition) {
        List<ComponentPackageDO> result = componentPackageRepository.selectByCondition(condition);
        return Pagination.valueOf(result, Function.identity());
    }

    /**
     * 根据条件获取单个组件包列表
     *
     * @param condition 过滤条件
     * @return ComponentPackageDO 对象
     */
    @Override
    public ComponentPackageDO get(ComponentPackageQueryCondition condition) {
        return componentPackageRepository.getByCondition(condition);
    }

    /**
     * 添加一个 component package 到当前已经存在的 app package 中
     *
     * @param appPackageId           app package id
     * @param componentPackageIdList component package ID 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addComponentPackageRelation(String appId, long appPackageId, List<Long> componentPackageIdList) {
        relRepository.deleteByCondition(AppPackageComponentRelQueryCondition.builder()
                .appPackageId(appPackageId).build());
        componentPackageIdList.forEach(componentPackageId -> relRepository.insert(AppPackageComponentRelDO.builder()
                .appId(appId)
                .appPackageId(appPackageId)
                .componentPackageId(componentPackageId)
                .build()));
    }

    /**
     * 创建一个 Component Package
     *
     * @param req 创建请求
     * @return 创建后的 ComponentPackageDO 对象
     */
    @Override
    public ComponentPackageDO createByLocalFile(ComponentPackageCreateByLocalFileReq req) {
        String appId = req.getAppId();
        String localFilePath = req.getLocalFilePath();
        boolean force = req.isForce();
        boolean resetVersion = req.isResetVersion();
        String componentType = req.getComponentPackageItem().getComponentType();
        String componentName = req.getComponentPackageItem().getComponentName();
        ComponentPackageDO item = ComponentPackageDO.builder()
                .appId(appId)
                .componentType(componentType)
                .componentName(componentName)
                .packageVersion(req.getComponentPackageItem().getPackageVersion())
                .packageCreator(req.getComponentPackageItem().getPackageCreator())
                .packageMd5(req.getComponentPackageItem().getPackageMd5())
                .packageAddon(req.getComponentPackageItem().getPackageAddon())
                .packageOptions(req.getComponentPackageItem().getPackageOptions())
                .componentSchema(req.getComponentPackageItem().getPackageExt())
                .build();

        if (force && resetVersion) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    "cannot use force and resetVersion parameters at the same time");
        }

        // 当重置版本号时，将 packageVersion 设置为当前系统中的下一个版本
        String packageVersion;
        if (resetVersion) {
            List<ComponentPackageDO> latestComponentPackageList = componentPackageRepository.selectByCondition(
                    ComponentPackageQueryCondition.builder()
                            .appId(item.getAppId())
                            .componentType(item.getComponentType())
                            .componentName(item.getComponentName())
                            .withBlobs(false)
                            .page(1)
                            .pageSize(1)
                            .build());
            if (latestComponentPackageList.size() > 0) {
                packageVersion = VersionUtil.buildVersion(
                        VersionUtil.buildNextPatch((latestComponentPackageList.get(0).getPackageVersion())));
            } else {
                packageVersion = VersionUtil.buildVersion(VersionUtil.buildNextPatch());
            }
            item.setPackageVersion(packageVersion);
        } else {
            packageVersion = item.getPackageVersion();
        }

        // 上传文件到 storage 中并获取 packagePath
        String bucketName = packageProperties.getBucketName();
        String objectName = PackageUtil.buildComponentPackageRemotePath(
                item.getAppId(), item.getComponentType(), item.getComponentName(), packageVersion
        );
        storage.putObject(bucketName, objectName, localFilePath);
        log.info("component package has put into storage|componentType={}|componentName={}|bucketName={}|" +
                        "localFilePath={}|packageVersion={}", item.getComponentType(), item.getComponentName(),
                bucketName, localFilePath, packageVersion);
        StorageFile storageFile = new StorageFile();
        storageFile.setBucketName(bucketName);
        storageFile.setObjectName(objectName);
        String packagePath = storageFile.toPath();
        item.setPackagePath(packagePath);

        // 当重置版本号时，直接插入
        if (resetVersion) {
            componentPackageRepository.insert(item);
            log.info("insert component package into database|appId={}|componentType={}|componentName={}|" +
                            "packageVersion={}", item.getAppId(), item.getComponentType(), item.getComponentName(),
                    packageVersion);
            return item;
        }

        // 不重置版本号的时候，需要看当前系统是否已经导入过相同版本
        ComponentPackageQueryCondition condition = ComponentPackageQueryCondition.builder()
                .appId(item.getAppId())
                .componentType(item.getComponentType())
                .componentName(item.getComponentName())
                .packageVersion(packageVersion)
                .withBlobs(true)
                .build();
        List<ComponentPackageDO> records = componentPackageRepository.selectByCondition(condition);
        if (records.size() == 0) {
            componentPackageRepository.insert(item);
            log.info("insert component package into database|appId={}|componentType={}|componentName={}|" +
                            "packageVersion={}", item.getAppId(), item.getComponentType(), item.getComponentName(),
                    packageVersion);
            return item;
        }
        if (!force) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    "cannot update component package, the same record already exists in system");
        }

        // 已经存在又需要强制覆盖的时候，进行更新
        ComponentPackageDO componentPackageDO = records.get(0);
        assert componentPackageDO.getId() != null && componentPackageDO.getId() > 0;
        componentPackageDO.setPackagePath(packagePath);
        componentPackageDO.setPackageCreator(item.getPackageCreator());
        componentPackageDO.setPackageMd5(item.getPackageMd5());
        componentPackageDO.setPackageAddon(item.getPackageAddon());
        componentPackageDO.setPackageOptions(item.getPackageOptions());
        componentPackageDO.setComponentSchema(item.getComponentSchema());
        componentPackageRepository.updateByPrimaryKeySelective(componentPackageDO);
        log.info("update component package in database|appId={}|componentType={}|componentName={}|" +
                        "packageVersion={}", item.getAppId(), item.getComponentType(), item.getComponentName(),
                packageVersion);
        assert componentPackageDO.getId() != null && componentPackageDO.getId() > 0;
        return componentPackageDO;
    }

    @Override
    public int delete(ComponentPackageQueryCondition condition) {
        relRepository.deleteByCondition(
                AppPackageComponentRelQueryCondition.builder().appId(condition.getAppId()).build());
        return componentPackageRepository.deleteByCondition(condition);
    }

    /**
     * 获取指定组件包的下一个可用版本 (with build number)
     *
     * @param req 查询请求
     * @return 下一个可用版本 (含当前)
     */
    @Override
    public ComponentPackageNextVersionRes nextVersion(ComponentPackageNextVersionReq req) {
        ComponentPackageQueryCondition condition = ComponentPackageQueryCondition.builder()
                .appId(req.getAppId())
                .componentType(req.getComponentType())
                .componentName(req.getComponentName())
                .withBlobs(false)
                .page(1)
                .pageSize(1)
                .build();
        List<ComponentPackageDO> packages = componentPackageRepository.selectByCondition(condition);
        String currentVersion = DefaultConstant.INIT_VERSION;
        if (CollectionUtils.isNotEmpty(packages)) {
            currentVersion = packages.get(0).getPackageVersion();
        }
        String nextVersion = VersionUtil.buildVersion(VersionUtil.buildNextPatch(currentVersion));
        return ComponentPackageNextVersionRes.builder()
                .currentVersion(currentVersion)
                .nextVersion(nextVersion)
                .build();
    }
}
