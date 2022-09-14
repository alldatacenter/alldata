package com.alibaba.tesla.appmanager.server.service.componentpackage;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.autoconfig.PackageProperties;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.PackageUtil;
import com.alibaba.tesla.appmanager.common.util.VersionUtil;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageCreateByStreamReq;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageCreateByLocalFileReq;
import com.alibaba.tesla.appmanager.domain.schema.AppPackageSchema;
import com.alibaba.tesla.appmanager.server.repository.*;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDO;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageService;
import com.alibaba.tesla.appmanager.server.service.apppackage.impl.AppPackageServiceImpl;
import com.alibaba.tesla.appmanager.server.service.componentpackage.impl.ComponentPackageServiceImpl;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployAppService;
import com.alibaba.tesla.appmanager.server.storage.Storage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(SpringRunner.class)
@Slf4j
public class TestServiceComponentPackageCreateByLocalFile {

    private static final String BUCKET_NAME = "appmanager";
    private static final String APP_ID = "testapp";
    private static final String COMPONENT_TYPE = "K8S_MICROSERVICE";
    private static final String COMPONENT_NAME = "testserver";
    private static final String PACKAGE_CREATOR = "SYSTEM";
    private static final String PACKAGE_VERSION = "3.5.5+20220201234212";
    private static final String PACKAGE_MD5 = "133966fbc2ea534ca32c706ab41f60ff";

    @Mock
    private ComponentPackageRepository componentPackageRepository;

    @Mock
    private AppPackageComponentRelRepository relRepository;

    @Mock
    private Storage storage;

    @Mock
    private PackageProperties packageProperties;

    private ComponentPackageService componentPackageService;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);

        componentPackageService = Mockito.spy(new ComponentPackageServiceImpl(
                componentPackageRepository,
                relRepository,
                storage,
                packageProperties
        ));
        Mockito.doReturn(BUCKET_NAME).when(packageProperties).getBucketName();
    }

    /**
     * 当目标 ComponentPackage 不存在时的 Mock 返回
     */
    private void prepareComponentPackageForNotExistsScene() {
        ComponentPackageQueryCondition condition = ComponentPackageQueryCondition.builder()
                .appId(APP_ID)
                .componentType(COMPONENT_TYPE)
                .componentName(COMPONENT_NAME)
                .withBlobs(false)
                .page(1)
                .pageSize(1)
                .build();
        Mockito.doReturn(new ArrayList<ComponentPackageDO>())
                .when(componentPackageRepository)
                .selectByCondition(condition);
    }

    /**
     * 当目标 ComponentPackage 存在时的 Mock 返回
     */
    private void prepareComponentPackageForExistsScene() {
        Mockito.doReturn(Collections.singletonList(getComponentPackage()))
                .when(componentPackageRepository)
                .selectByCondition(ComponentPackageQueryCondition.builder()
                        .appId(APP_ID)
                        .componentType(COMPONENT_TYPE)
                        .componentName(COMPONENT_NAME)
                        .withBlobs(false)
                        .page(1)
                        .pageSize(1)
                        .build());
        Mockito.doReturn(Collections.singletonList(getComponentPackage()))
                .when(componentPackageRepository)
                .selectByCondition(ComponentPackageQueryCondition.builder()
                        .appId(APP_ID)
                        .componentType(COMPONENT_TYPE)
                        .componentName(COMPONENT_NAME)
                        .packageVersion(PACKAGE_VERSION)
                        .withBlobs(true)
                        .build());
    }

    /**
     * 测试带 force == false && resetVersion == true 情况下的创建动作
     */
    @Test
    public void testCreateWithResetVersion() throws Exception {
        // 目标版本不存在 => 1.0.1
        prepareComponentPackageForNotExistsScene();
        ComponentPackageDO res = componentPackageService
                .createByLocalFile(ComponentPackageCreateByLocalFileReq.builder()
                        .appId(APP_ID)
                        .componentPackageItem(getComponentPackageItem())
                        .localFilePath("fakepath")
                        .force(false)
                        .resetVersion(true)
                        .build());
        verifyCreateRes(res, getComponentPackage(), true, false);
        Mockito.verify(componentPackageRepository, Mockito.times(1)).insert(Mockito.any());
        Mockito.verify(componentPackageRepository, Mockito.times(0)).updateByPrimaryKeySelective(Mockito.any());
        Mockito.verify(storage, Mockito.times(1))
                .putObject(Mockito.eq(BUCKET_NAME), Mockito.isA(String.class), Mockito.isA(String.class));

        // 目标版本存在 => 3.5.6
        prepareComponentPackageForExistsScene();
        res = componentPackageService
                .createByLocalFile(ComponentPackageCreateByLocalFileReq.builder()
                        .appId(APP_ID)
                        .componentPackageItem(getComponentPackageItem())
                        .localFilePath("fakepath")
                        .force(false)
                        .resetVersion(true)
                        .build());
        verifyCreateRes(res, getComponentPackage(), true, true);
        Mockito.verify(componentPackageRepository, Mockito.times(2)).insert(Mockito.any());
        Mockito.verify(componentPackageRepository, Mockito.times(0)).updateByPrimaryKeySelective(Mockito.any());
        Mockito.verify(storage, Mockito.times(2))
                .putObject(Mockito.eq(BUCKET_NAME), Mockito.isA(String.class), Mockito.isA(String.class));
    }

    /**
     * 测试带 force == false && resetVersion == true 情况下的创建动作
     */
    @Test
    public void testCreateWithForce() throws Exception {
        // 目标版本不存在 => 3.5.5
        prepareComponentPackageForNotExistsScene();
        ComponentPackageDO res = componentPackageService
                .createByLocalFile(ComponentPackageCreateByLocalFileReq.builder()
                        .appId(APP_ID)
                        .componentPackageItem(getComponentPackageItem())
                        .localFilePath("fakepath")
                        .force(true)
                        .resetVersion(false)
                        .build());
        verifyCreateRes(res, getComponentPackage(), false, false);
        Mockito.verify(componentPackageRepository, Mockito.times(1)).insert(Mockito.any());
        Mockito.verify(componentPackageRepository, Mockito.times(0)).updateByPrimaryKeySelective(Mockito.any());
        Mockito.verify(storage, Mockito.times(1))
                .putObject(Mockito.eq(BUCKET_NAME), Mockito.isA(String.class), Mockito.isA(String.class));

        // 目标版本存在 => 3.5.5
        prepareComponentPackageForExistsScene();
        res = componentPackageService
                .createByLocalFile(ComponentPackageCreateByLocalFileReq.builder()
                        .appId(APP_ID)
                        .componentPackageItem(getComponentPackageItem())
                        .localFilePath("fakepath")
                        .force(true)
                        .resetVersion(false)
                        .build());
        verifyCreateRes(res, getComponentPackage(), false, true);
        Mockito.verify(componentPackageRepository, Mockito.times(1)).insert(Mockito.any());
        Mockito.verify(componentPackageRepository, Mockito.times(1)).updateByPrimaryKeySelective(Mockito.any());
        Mockito.verify(storage, Mockito.times(2))
                .putObject(Mockito.eq(BUCKET_NAME), Mockito.isA(String.class), Mockito.isA(String.class));
    }

    /**
     * 测试带 force == false && resetVersion == false 情况下的创建动作
     */
    @Test
    public void testCreateWithNothing() throws Exception {
        // 目标版本不存在 => 3.5.5
        prepareComponentPackageForNotExistsScene();
        ComponentPackageDO res = componentPackageService
                .createByLocalFile(ComponentPackageCreateByLocalFileReq.builder()
                        .appId(APP_ID)
                        .componentPackageItem(getComponentPackageItem())
                        .localFilePath("fakepath")
                        .force(false)
                        .resetVersion(false)
                        .build());
        verifyCreateRes(res, getComponentPackage(), false, false);
        Mockito.verify(componentPackageRepository, Mockito.times(1)).insert(Mockito.any());
        Mockito.verify(componentPackageRepository, Mockito.times(0)).updateByPrimaryKeySelective(Mockito.any());
        Mockito.verify(storage, Mockito.times(1))
                .putObject(Mockito.eq(BUCKET_NAME), Mockito.isA(String.class), Mockito.isA(String.class));

        // 目标版本存在 => 抛出异常
        prepareComponentPackageForExistsScene();
        assertThatThrownBy(() -> componentPackageService
                .createByLocalFile(ComponentPackageCreateByLocalFileReq.builder()
                        .appId(APP_ID)
                        .componentPackageItem(getComponentPackageItem())
                        .localFilePath("fakepath")
                        .force(false)
                        .resetVersion(false)
                        .build()))
                .isInstanceOf(AppException.class);
    }

    /**
     * 验证创建应用包的结果对象
     *
     * @param res              结果对象
     * @param req              请求对象
     * @param resetVersion     是否重置版本
     * @param reqPackageExists 原始包是否已经在目标系统中存在
     */
    private void verifyCreateRes(
            ComponentPackageDO res, ComponentPackageDO req, boolean resetVersion, boolean reqPackageExists) {
        assertThat(res).isNotNull();
        log.info("createRes={}", JSONObject.toJSONString(res));
        assertThat(res.getAppId()).isEqualTo(req.getAppId());
        assertThat(res.getPackageCreator()).isEqualTo(req.getPackageCreator());
        verifyPackageVersion(res.getPackageVersion(), req, resetVersion, reqPackageExists);
        verifyPackagePath(res.getPackagePath(), req, resetVersion, reqPackageExists);
    }

    /**
     * 验证 packageVersion 合法性
     *
     * @param packageVersion   PackageVersion
     * @param req              创建请求
     * @param resetVersion     是否重置版本
     * @param reqPackageExists 原始包是否已经在目标系统中存在
     */
    private void verifyPackageVersion(
            String packageVersion, ComponentPackageDO req, boolean resetVersion, boolean reqPackageExists) {
        String clearVersion = VersionUtil.clear(packageVersion);
        if (resetVersion) {
            if (reqPackageExists) {
                assertThat(clearVersion)
                        .isEqualTo(VersionUtil.clear(VersionUtil.buildNextPatch(req.getPackageVersion())));
            } else {
                assertThat(clearVersion).isEqualTo(VersionUtil.clear(VersionUtil.buildNextPatch()));
            }
        } else {
            assertThat(clearVersion).isEqualTo(VersionUtil.clear(req.getPackageVersion()));
        }
    }

    /**
     * 验证 packagePath 合法性
     *
     * @param packagePath      PackagePath
     * @param req              创建请求
     * @param resetVersion     是否重置版本
     * @param reqPackageExists 原始包是否已经在目标系统中存在
     */
    private void verifyPackagePath(
            String packagePath, ComponentPackageDO req, boolean resetVersion, boolean reqPackageExists) {
        String[] arr = packagePath.split("/");
        assertThat(arr.length).isEqualTo(6);
        assertThat(arr[0]).isEqualTo(BUCKET_NAME);
        assertThat(arr[1]).isEqualTo("components");
        assertThat(arr[2]).isEqualTo(req.getAppId());
        assertThat(arr[3]).isEqualTo(req.getComponentType());
        assertThat(arr[4]).isEqualTo(req.getComponentName());

        // 判定 packagePath 中的 filename 合法性
        String filename = arr[5];
        assertThat(filename.endsWith(".zip")).isTrue();
        String packageVersion = filename.substring(0, filename.length() - 4);
        verifyPackageVersion(packageVersion, req, resetVersion, reqPackageExists);
    }

    private ComponentPackageDO getComponentPackage() {
        return ComponentPackageDO.builder()
                .id(1L)
                .appId(APP_ID)
                .packageVersion(PACKAGE_VERSION)
                .packagePath(PackageUtil.buildComponentPackagePath(
                        BUCKET_NAME, APP_ID, COMPONENT_TYPE, COMPONENT_NAME, PACKAGE_VERSION))
                .packageCreator(PACKAGE_CREATOR)
                .componentType(COMPONENT_TYPE)
                .componentName(COMPONENT_NAME)
                .packageMd5(PACKAGE_MD5)
                .version(0)
                .build();
    }

    private AppPackageSchema.ComponentPackageItem getComponentPackageItem() {
        return AppPackageSchema.ComponentPackageItem.builder()
                .packageVersion(PACKAGE_VERSION)
                .packageCreator(PACKAGE_CREATOR)
                .componentType(COMPONENT_TYPE)
                .componentName(COMPONENT_NAME)
                .packageMd5(PACKAGE_MD5)
                .build();
    }
}
