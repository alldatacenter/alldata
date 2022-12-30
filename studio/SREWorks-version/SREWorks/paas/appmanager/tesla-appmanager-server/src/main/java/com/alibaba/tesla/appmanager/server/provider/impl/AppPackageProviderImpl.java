package com.alibaba.tesla.appmanager.server.provider.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.AppPackageProvider;
import com.alibaba.tesla.appmanager.common.constants.CheckNullObject;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.constants.PackAppPackageVariableKey;
import com.alibaba.tesla.appmanager.common.constants.UnpackAppPackageVariableKey;
import com.alibaba.tesla.appmanager.common.enums.DagTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.NetworkUtil;
import com.alibaba.tesla.appmanager.common.util.ObjectUtil;
import com.alibaba.tesla.appmanager.common.util.VersionUtil;
import com.alibaba.tesla.appmanager.domain.core.StorageFile;
import com.alibaba.tesla.appmanager.domain.dto.AppPackageDTO;
import com.alibaba.tesla.appmanager.domain.req.AppPackageTagUpdateReq;
import com.alibaba.tesla.appmanager.domain.req.apppackage.*;
import com.alibaba.tesla.appmanager.domain.res.apppackage.*;
import com.alibaba.tesla.appmanager.server.assembly.AppPackageDtoConvert;
import com.alibaba.tesla.appmanager.server.repository.AppPackageComponentRelRepository;
import com.alibaba.tesla.appmanager.server.repository.AppPackageRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppMetaQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.UnitQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppMetaDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageComponentRelDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;
import com.alibaba.tesla.appmanager.server.repository.domain.UnitDO;
import com.alibaba.tesla.appmanager.server.service.appmeta.AppMetaService;
import com.alibaba.tesla.appmanager.server.service.appoption.AppOptionService;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageService;
import com.alibaba.tesla.appmanager.server.service.pack.dag.PackAppPackageToStorageDag;
import com.alibaba.tesla.appmanager.server.service.pack.dag.UnpackAppPackageFromStorageDag;
import com.alibaba.tesla.appmanager.server.service.unit.UnitService;
import com.alibaba.tesla.appmanager.server.service.unit.helper.UnitHttpHelper;
import com.alibaba.tesla.appmanager.server.storage.Storage;
import com.alibaba.tesla.dag.repository.domain.DagInstDO;
import com.alibaba.tesla.dag.schedule.status.DagInstStatus;
import com.alibaba.tesla.dag.services.DagInstNewService;
import com.alibaba.tesla.dag.services.DagInstService;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * AppPackage 服务实现
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class AppPackageProviderImpl implements AppPackageProvider {

    private static final int MAX_UNPACK_WAIT_SECS = 1800;

    private final AppPackageRepository appPackageRepository;
    private final AppPackageService appPackageService;
    private final AppMetaService appMetaService;
    private final AppOptionService appOptionService;
    private final AppPackageDtoConvert appPackageDtoConvert;
    private final AppPackageComponentRelRepository relRepository;
    private final DagInstService dagInstService;
    private final DagInstNewService dagInstNewService;
    private final Storage storage;
    private final UnitService unitService;

    public AppPackageProviderImpl(
            AppPackageRepository appPackageRepository, AppPackageService appPackageService,
            AppMetaService appMetaService, AppOptionService appOptionService, AppPackageDtoConvert appPackageDtoConvert,
            AppPackageComponentRelRepository relRepository, DagInstService dagInstService,
            DagInstNewService dagInstNewService, Storage storage, UnitService unitService) {
        this.appPackageRepository = appPackageRepository;
        this.appPackageService = appPackageService;
        this.appMetaService = appMetaService;
        this.appOptionService = appOptionService;
        this.appPackageDtoConvert = appPackageDtoConvert;
        this.relRepository = relRepository;
        this.dagInstService = dagInstService;
        this.dagInstNewService = dagInstNewService;
        this.storage = storage;
        this.unitService = unitService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppPackageDTO create(AppPackageCreateReq request, String operator) {
        String appId = request.getAppId();
        String version = normalizeVersion(appId, request.getVersion());

        // 插入数据库
        List<Long> componentPackageIdList = request.getComponentPackageIdList();
        AppPackageDO appPackageDO = AppPackageDO.builder()
                .appId(appId)
                .packageVersion(version)
                .componentCount((long) componentPackageIdList.size())
                .packageCreator(operator)
                .build();
        appPackageRepository.insert(appPackageDO);
        componentPackageIdList.forEach(componentPackageId -> relRepository.insert(AppPackageComponentRelDO.builder()
                .appId(appId)
                .appPackageId(appPackageDO.getId())
                .componentPackageId(componentPackageId)
                .build()));

        // 触发生成应用包的流程
        List<String> componentPackageIdStringList = new ArrayList<>(componentPackageIdList.size());
        for (Long componentPackageId : componentPackageIdList) {
            componentPackageIdStringList.add(String.valueOf(componentPackageId));
        }
        JSONObject variables = new JSONObject();
        variables.put(DefaultConstant.DAG_TYPE, DagTypeEnum.PACK_APP_PACKAGE.toString());
        variables.put(PackAppPackageVariableKey.APP_PACKAGE_ID, appPackageDO.getId());
        variables.put(PackAppPackageVariableKey.COMPONENT_PACKAGE_ID_LIST,
                String.join(",", componentPackageIdStringList));
        long dagInstId = 0L;
        try {
            dagInstId = dagInstService.start(PackAppPackageToStorageDag.name, variables, true);
        } catch (Exception e) {
            log.error("cannot start pack app package dag|appPackageId={}|variables={}|exception={}",
                    appPackageDO.getId(), variables.toJSONString(), ExceptionUtils.getStackTrace(e));
            appPackageDO.setPackagePath("error: " + e.getMessage());
            appPackageRepository.updateByPrimaryKeySelective(appPackageDO);
        }
        log.info("app package has created|appId={}|version={}|componentPackageIdList={}|dagInstId={}",
                appId, version, JSONArray.toJSONString(componentPackageIdList), dagInstId);
        return appPackageDtoConvert.to(appPackageDO);
    }

    @Override
    public Pagination<AppPackageDTO> list(AppPackageQueryReq req, String operator) {
        AppPackageQueryCondition condition = AppPackageQueryCondition.builder()
                .appId(req.getAppId())
                .packageVersion(req.getPackageVersion())
                .packageVersionGreaterThan(req.getPackageVersionGreaterThan())
                .packageVersionLessThan(req.getPackageVersionLessThan())
                .tags(req.getTagList())
                .pagination(req.isPagination())
                .page(req.getPage())
                .pageSize(req.getPageSize())
                .withBlobs(req.isWithBlobs())
                .build();
        return Pagination.transform(appPackageService.list(condition), item -> appPackageDtoConvert.to(item));
    }

    /**
     * 查询应用包（单个）
     *
     * @param req      查询请求
     * @param operator 操作者
     */
    @Override
    public AppPackageDTO get(AppPackageQueryReq req, String operator) {
        AppPackageQueryCondition condition = AppPackageQueryCondition.builder()
                .id(req.getId())
                .withBlobs(req.isWithBlobs())
                .build();
        AppPackageDO appPackageDO = appPackageService.get(condition);
        AppPackageDTO appPackageDTO = appPackageDtoConvert.to(appPackageDO);
        if (appPackageDTO == null) {
            return null;
        }

        // 补充 app 相关元数据
        AppMetaDO appMetaDO = appMetaService.get(AppMetaQueryCondition.builder()
                .appId(appPackageDO.getAppId())
                .build());
        if (appMetaDO == null) {
            return appPackageDTO;
        }
        JSONObject optionMap = appOptionService.getOptionMap(appMetaDO.getAppId());
        String name = appMetaDO.getAppId();
        if (StringUtils.isNotEmpty(optionMap.getString("name"))) {
            name = optionMap.getString("name");
        }
        appPackageDTO.setAppName(name);
        appPackageDTO.setAppOptions(optionMap);
        return appPackageDTO;
    }

    /**
     * 删除应用包
     *
     * @param appPackageId 应用包ID
     * @param operator     操作人
     * @return AppPackageDTO
     */
    @Override
    public void delete(Long appPackageId, String operator) {
        appPackageRepository.deleteByPrimaryKey(appPackageId);
    }

    /**
     * 为指定的应用包增加 Tag
     *
     * @param request
     */
    @Override
    public void addTag(AppPackageTagUpdateReq request) {

    }

    /**
     * 导入 App Package
     *
     * @param req      请求参数
     * @param body     包体 Stream
     * @param operator 操作人
     * @return AppPackageDTO
     */
    @Override
    public AppPackageDTO importPackage(AppPackageImportReq req, InputStream body, String operator) {
        // 新增 App Package 记录
        String appId = req.getAppId();
        AppPackageDO item = appPackageService.createByStream(AppPackageCreateByStreamReq.builder()
                .appId(req.getAppId())
                .body(body)
                .packageVersion(req.getPackageVersion())
                .force(req.getForce())
                .resetVersion(req.getResetVersion())
                .packageCreator(req.getPackageCreator())
                .build());

        // 启动 DAG 进行解包
        JSONObject variables = new JSONObject();
        variables.put(DefaultConstant.DAG_TYPE, DagTypeEnum.UNPACK_APP_PACKAGE);
        variables.put(UnpackAppPackageVariableKey.APP_ID, appId);
        variables.put(UnpackAppPackageVariableKey.APP_PACKAGE_ID, item.getId());
        variables.put(UnpackAppPackageVariableKey.FORCE, req.getForce());
        variables.put(UnpackAppPackageVariableKey.RESET_VERSION, req.getResetVersion());
        log.info("prepare to start unpack app package dag|appId={}|appPackageId={}|variables={}|appPackage={}",
                appId, item.getId(), variables.toJSONString(), JSONObject.toJSONString(item));
        long dagInstId;
        try {
            dagInstId = dagInstService.start(UnpackAppPackageFromStorageDag.name, variables, true);
        } catch (Exception e) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                    String.format("cannot start unpack app package dag|appId=%s|appPackageId=%s|variables=%s",
                            appId, item.getId(), variables.toJSONString()), e);
        }
        for (int i = 0; i < MAX_UNPACK_WAIT_SECS; i++) {
            DagInstDO dagInst = dagInstNewService.getDagInstById(dagInstId);
            DagInstStatus dagInstStatus = DagInstStatus.valueOf(dagInst.getStatus());
            if (!dagInstStatus.isEnd()) {
                try {
                    Thread.sleep(1000);
                } catch (Exception ignored) {
                }
                continue;
            }
            if (dagInstStatus.equals(DagInstStatus.EXCEPTION) || dagInstStatus.equals(DagInstStatus.STOPPED)) {
                throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                        String.format("unpack failed, dagInst=%s", JSONObject.toJSONString(dagInst)));
            }
            return get(AppPackageQueryReq.builder().id(item.getId()).build(), operator);
        }
        throw new AppException(AppErrorCode.UNKNOWN_ERROR, "unpack failed, wait timeout");
    }

    /**
     * 根据应用包自动生成对应的 ApplicationConfiguration 配置
     *
     * @param req 生成请求参数
     * @return ApplicationConfiguration
     */
    @Override
    public ApplicationConfigurationGenerateRes generate(ApplicationConfigurationGenerateReq req) {
        return appPackageService.generate(req);
    }

    /**
     * 生成 URL
     *
     * @param appPackageId 应用包 ID
     * @param operator     操作人
     * @return
     */
    @Override
    public AppPackageUrlRes generateUrl(Long appPackageId, String operator) {
        AppPackageQueryCondition condition = AppPackageQueryCondition.builder()
                .id(appPackageId)
                .withBlobs(false)
                .build();
        AppPackageDO appPackageDO = appPackageRepository.getByCondition(condition);
        if (appPackageDO == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("invalid app package id %d", appPackageId));
        }
        String packagePath = appPackageDO.getPackagePath();
        if (StringUtils.isEmpty(packagePath)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "the app package url is not exists");
        }
        StorageFile storageFile = new StorageFile(packagePath);
        String bucketName = storageFile.getBucketName();
        String objectName = storageFile.getObjectName();
        String url = storage.getObjectUrl(bucketName, objectName, DefaultConstant.DEFAULT_FILE_EXPIRATION);
        String filename = packagePath.substring(packagePath.lastIndexOf("/") + 1);
        return AppPackageUrlRes.builder().url(url).filename(filename).build();
    }

    /**
     * 同步当前系统中的应用包到指定的外部环境中
     *
     * @param req      请求
     * @param operator 操作人
     * @return AppPackageSyncExternalRes
     */
    @Override
    public AppPackageSyncExternalRes syncExternal(AppPackageSyncExternalReq req, String operator)
            throws IOException, URISyntaxException {
        Long appPackageId = req.getAppPackageId();
        AppPackageQueryCondition condition = AppPackageQueryCondition.builder()
                .id(appPackageId)
                .withBlobs(false)
                .build();
        AppPackageDO appPackageDO = appPackageRepository.getByCondition(condition);
        if (appPackageDO == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find app package by id %d", appPackageId));
        }
        String packagePath = appPackageDO.getPackagePath();
        if (StringUtils.isEmpty(packagePath)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    "the storage file has not generated yet, please try again later");
        }

        // 获取目标单元信息
        UnitDO unit = unitService.get(UnitQueryCondition.builder().unitId(req.getUnitId()).build());
        if (unit == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    "the unit you provided does not exists");
        }

        // 获取目标环境的 access token
        OkHttpClient httpClient = UnitHttpHelper.getHttpClient(unit);
        String authToken = UnitHttpHelper.getAuthToken(unit, httpClient);

        // 转发文件
        StorageFile storageFile = new StorageFile(packagePath);
        String bucketName = storageFile.getBucketName();
        String objectName = storageFile.getObjectName();
        String endpoint = unit.getEndpoint();
        Path tempFile = Files.createTempFile(null, null);
        try {
            String url = storage.getObjectUrl(bucketName, objectName, DefaultConstant.DEFAULT_FILE_EXPIRATION);
            FileUtils.copyURLToFile(new URL(url), tempFile.toFile());
            log.info("currnet app package has download to local|appPackageId={}|packagePath={}|url={}|" +
                    "tempFilePath={}", appPackageId, packagePath, url, tempFile.toString());

            // 发送应用包到对应代理环境
            RequestBody requestBody = NetworkUtil.createRequestBodyByStream(
                    MediaType.parse("application/octet-stream"), new FileInputStream(tempFile.toFile()));
            String urlPrefix = NetworkUtil.concatenate(new URL(endpoint),
                    String.format("apps/%s/app-packages/import", appPackageDO.getAppId())).toString();
            HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(urlPrefix)).newBuilder();
            urlBuilder.addQueryParameter("appId", appPackageDO.getAppId());
            urlBuilder.addQueryParameter("packageVersion", appPackageDO.getPackageVersion());
            urlBuilder.addQueryParameter("packageCreator", appPackageDO.getPackageCreator());
            urlBuilder.addQueryParameter("force", "true");
            Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build()).post(requestBody);
            JSONObject response = NetworkUtil.sendRequest(httpClient, requestBuilder, authToken);
            long targetAppPackageId = response.getJSONObject("data").getLongValue("id");
            return AppPackageSyncExternalRes.builder()
                    .appPackageId(targetAppPackageId)
                    .build();
        } catch (Exception e) {
            Files.deleteIfExists(tempFile);
            if (e instanceof AppException) {
                throw e;
            }
            throw new AppException(AppErrorCode.DEPLOY_ERROR, String.format("cannot sync to external environment, " +
                    "exception=%s", ExceptionUtils.getStackTrace(e)));
        }
    }

    @Override
    public AppPackageReleaseRes releaseAsCustomAddon(AppPackageReleaseReq req) {
        ObjectUtil.checkNull(CheckNullObject.builder()
                .checkObject(req)
                .objectName("AppPackageReleaseReq")
                .actionName("releaseAppPackageAsCustomAddon")
                .fields(ImmutableList.of("appPackageId", "addonId", "addonVersion"))
                .build());

        Long appPackageRelaseId = appPackageService.releaseCustomAddonMeta(req);
        return AppPackageReleaseRes.builder()
                .appPackageRelaseId(appPackageRelaseId)
                .build();
    }

    /**
     * 应用包版本号处理，允许传入 null 进行自动增加
     *
     * @param appId   应用 ID
     * @param version 版本号
     * @return 处理后的版本号
     */
    private String normalizeVersion(String appId, String version) {
        if (!StringUtils.isEmpty(version)) {
            return version;
        }

        Pagination<AppPackageDO> existAppPackages = appPackageService.list(AppPackageQueryCondition
                .builder()
                .appId(appId)
                .build());
        if (!existAppPackages.isEmpty()) {
            version = existAppPackages.getItems().get(0).getPackageVersion();
        } else {
            version = DefaultConstant.INIT_VERSION;
        }
        return VersionUtil.buildVersion(VersionUtil.buildNextPatch(version));
    }
}
