package com.alibaba.tesla.appmanager.server.service.pack;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.ComponentPackageProvider;
import com.alibaba.tesla.appmanager.autoconfig.SystemProperties;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.PackageTaskEnum;
import com.alibaba.tesla.appmanager.common.util.VersionUtil;
import com.alibaba.tesla.appmanager.domain.req.apppackage.ComponentBinder;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageTaskCreateReq;
import com.alibaba.tesla.appmanager.meta.helm.service.HelmMetaService;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.service.K8sMicroserviceMetaService;
import com.alibaba.tesla.appmanager.server.repository.AppPackageTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.ComponentPackageTaskRepository;
import com.alibaba.tesla.appmanager.server.service.appaddon.AppAddonService;
import com.alibaba.tesla.appmanager.server.service.appmeta.AppMetaService;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageTaskService;
import com.alibaba.tesla.appmanager.server.service.pack.impl.PackServiceImpl;
import com.alibaba.tesla.appmanager.domain.container.ComponentPackageTaskMessage;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@Slf4j
public class TestServicePackDoComponentPackageCreate {

    private static final String APP_ID = "testapp";
    private static final String NAMESPACE_ID = "default";
    private static final String STAGE_ID = "pre";
    private static final ComponentTypeEnum COMPONENT_TYPE = ComponentTypeEnum.INTERNAL_ADDON;
    private static final String COMPONENT_NAME = "productops";
    private static final String OPERATOR = "SYSTEM";
    private static final String PACKAGE_VERSION = "3.5.5+20220201234212";
    private static final String PACKAGE_MD5 = "133966fbc2ea534ca32c706ab41f60ff";

    @Mock
    private ComponentPackageProvider componentPackageProvider;

    @Mock
    private ComponentPackageTaskRepository componentPackageTaskRepository;

    @Mock
    private K8sMicroserviceMetaService k8sMicroserviceMetaService;

    @Mock
    private AppPackageTaskRepository appPackageTaskRepository;

    @Mock
    private AppPackageTaskService appPackageTaskService;

    @Mock
    private AppAddonService appAddonService;

    @Mock
    private AppMetaService appMetaService;

    @Mock
    private HelmMetaService helmMetaService;

    @Mock
    private SystemProperties systemProperties;

    private PackService packService;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);

        packService = Mockito.spy(new PackServiceImpl(
                componentPackageProvider,
                componentPackageTaskRepository,
                k8sMicroserviceMetaService,
                appPackageTaskRepository,
                appPackageTaskService,
                appAddonService,
                appMetaService,
                helmMetaService,
                systemProperties
        ));
        Mockito.doReturn(new JSONObject())
                .when(packService)
                .buildOptions4InternalAddon(APP_ID, NAMESPACE_ID, STAGE_ID, COMPONENT_NAME, true);
    }

    @Test
    public void testInternalAddonAutoVersionWithComponentPackageVersion() {
        ComponentPackageTaskMessage task = ComponentPackageTaskMessage.builder()
                .appPackageTaskId(1L)
                .componentPackageTaskId(1L)
                .appId(APP_ID)
                .namespaceId(NAMESPACE_ID)
                .stageId(STAGE_ID)
                .operator(OPERATOR)
                .component(ComponentBinder.builder()
                        .componentType(COMPONENT_TYPE)
                        .componentName(COMPONENT_NAME)
                        .componentLabel(COMPONENT_NAME)
                        .version(PACKAGE_VERSION)
                        .branch("master")
                        .useRawOptions(false)
                        .isDevelop(true)
                        .build())
                .packageTaskEnum(PackageTaskEnum.CREATE)
                .build();
        packService.createComponentPackageTask(task);

        // 验证创建版本号是否符合预期
        ArgumentCaptor<ComponentPackageTaskCreateReq> argReq = ArgumentCaptor.forClass(ComponentPackageTaskCreateReq.class);
        ArgumentCaptor<String> argOperator = ArgumentCaptor.forClass(String.class);
        Mockito.verify(componentPackageProvider).createTask(argReq.capture(), argOperator.capture());
        assertThat(VersionUtil.clear(argReq.getValue().getVersion())).isEqualTo(VersionUtil.clear(PACKAGE_VERSION));
        assertThat(argOperator.getValue()).isEqualTo(OPERATOR);
    }
}
