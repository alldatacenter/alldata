package com.alibaba.tesla.appmanager.server.service.componentpackage;

import com.alibaba.tesla.appmanager.autoconfig.PackageProperties;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.util.PackageUtil;
import com.alibaba.tesla.appmanager.common.util.VersionUtil;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageNextVersionReq;
import com.alibaba.tesla.appmanager.domain.res.componentpackage.ComponentPackageNextVersionRes;
import com.alibaba.tesla.appmanager.server.repository.AppPackageComponentRelRepository;
import com.alibaba.tesla.appmanager.server.repository.ComponentPackageRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDO;
import com.alibaba.tesla.appmanager.server.service.componentpackage.impl.ComponentPackageServiceImpl;
import com.alibaba.tesla.appmanager.server.storage.Storage;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@Slf4j
public class TestServiceComponentPackageNextVersion {

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
    }

    /**
     * 测试组件包任务已经存在的情况下，获取 next version
     */
    @Test
    public void testWhenRecordExists() {
        Mockito.doReturn(Collections.singletonList(ComponentPackageDO.builder()
                        .id(1L)
                        .appId(APP_ID)
                        .componentType(COMPONENT_TYPE)
                        .componentName(COMPONENT_NAME)
                        .packageVersion(PACKAGE_VERSION)
                        .packagePath(PackageUtil.buildComponentPackagePath(
                                BUCKET_NAME, APP_ID, COMPONENT_TYPE, COMPONENT_NAME, PACKAGE_VERSION))
                        .packageCreator(PACKAGE_CREATOR)
                        .packageMd5(PACKAGE_MD5)
                        .version(0)
                        .build()))
                .when(componentPackageRepository)
                .selectByCondition(ComponentPackageQueryCondition.builder()
                        .appId(APP_ID)
                        .componentType(COMPONENT_TYPE)
                        .componentName(COMPONENT_NAME)
                        .withBlobs(false)
                        .page(1)
                        .pageSize(1)
                        .build());

        ComponentPackageNextVersionRes res = componentPackageService.nextVersion(
                ComponentPackageNextVersionReq.builder()
                        .appId(APP_ID)
                        .componentType(COMPONENT_TYPE)
                        .componentName(COMPONENT_NAME)
                        .build());
        assertThat(VersionUtil.clear(res.getCurrentVersion())).isEqualTo(VersionUtil.clear(PACKAGE_VERSION));
        assertThat(VersionUtil.clear(res.getNextVersion())).isEqualTo(VersionUtil.buildNextPatch(PACKAGE_VERSION));
    }

    /**
     * 测试组件包任务不存在的情况下，获取 next version
     */
    @Test
    public void testWhenRecordNotExists() {
        Mockito.doReturn(Collections.emptyList())
                .when(componentPackageRepository)
                .selectByCondition(ComponentPackageQueryCondition.builder()
                        .appId(APP_ID)
                        .componentType(COMPONENT_TYPE)
                        .componentName(COMPONENT_NAME)
                        .withBlobs(false)
                        .page(1)
                        .pageSize(1)
                        .build());

        ComponentPackageNextVersionRes res = componentPackageService.nextVersion(
                ComponentPackageNextVersionReq.builder()
                        .appId(APP_ID)
                        .componentType(COMPONENT_TYPE)
                        .componentName(COMPONENT_NAME)
                        .build());
        assertThat(VersionUtil.clear(res.getCurrentVersion())).isEqualTo(DefaultConstant.INIT_VERSION);
        assertThat(VersionUtil.clear(res.getNextVersion()))
                .isEqualTo(VersionUtil.buildNextPatch(DefaultConstant.INIT_VERSION));
    }
}
