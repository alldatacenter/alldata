package com.alibaba.tesla.appmanager.server.provider.apppackage;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.AppPackageProvider;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.constants.UnpackAppPackageVariableKey;
import com.alibaba.tesla.appmanager.common.enums.DagTypeEnum;
import com.alibaba.tesla.appmanager.common.util.PackageUtil;
import com.alibaba.tesla.appmanager.common.util.VersionUtil;
import com.alibaba.tesla.appmanager.domain.dto.AppPackageDTO;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageImportReq;
import com.alibaba.tesla.appmanager.server.assembly.AppPackageDtoConvert;
import com.alibaba.tesla.appmanager.server.provider.impl.AppPackageProviderImpl;
import com.alibaba.tesla.appmanager.server.repository.AppPackageComponentRelRepository;
import com.alibaba.tesla.appmanager.server.repository.AppPackageRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;
import com.alibaba.tesla.appmanager.server.service.appmeta.AppMetaService;
import com.alibaba.tesla.appmanager.server.service.appoption.AppOptionService;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageService;
import com.alibaba.tesla.appmanager.server.service.pack.dag.UnpackAppPackageFromStorageDag;
import com.alibaba.tesla.appmanager.server.service.unit.UnitService;
import com.alibaba.tesla.appmanager.server.storage.Storage;
import com.alibaba.tesla.dag.repository.domain.DagInstDO;
import com.alibaba.tesla.dag.schedule.status.DagInstStatus;
import com.alibaba.tesla.dag.services.DagInstNewService;
import com.alibaba.tesla.dag.services.DagInstService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 Provider 中应用包导入函数
 */
@RunWith(SpringRunner.class)
@Slf4j
public class TestProviderAppPackageImport {

    private static final String BUCKET_NAME = "appmanager";
    private static final String APP_ID = "testapp";
    private static final String PACKAGE_CREATOR = "SYSTEM";
    private static final String PACKAGE_VERSION = "2.1.1+20220201234212";
    private static final String PACKAGE_MD5 = "433966fbc2ea534ca32c706ab41f60ff";
    private static final String APP_SCHEMA = "{\"a\": \"b\"}";
    private static final String SWAPP = "{\"c\": \"d\"}";
    private static final InputStream BODY = IOUtils.toInputStream("body", StandardCharsets.UTF_8);

    @Mock
    private AppPackageRepository appPackageRepository;

    @Mock
    private AppPackageService appPackageService;

    @Mock
    private AppMetaService appMetaService;

    @Mock
    private AppOptionService appOptionService;

    @Mock
    private AppPackageComponentRelRepository relRepository;

    @Mock
    private DagInstService dagInstService;

    @Mock
    private DagInstNewService dagInstNewService;

    @Mock
    private Storage storage;

    @Mock
    private UnitService unitService;

    private AppPackageProvider appPackageProvider;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);

        appPackageProvider = Mockito.spy(new AppPackageProviderImpl(
                appPackageRepository,
                appPackageService,
                appMetaService,
                appOptionService,
                new AppPackageDtoConvert(),
                relRepository,
                dagInstService,
                dagInstNewService,
                storage,
                unitService
        ));
    }

    @Test
    public void testImport() throws Exception {
        AppPackageDO appPackageDO = AppPackageDO.builder()
                .id(1L)
                .appId(APP_ID)
                .packageVersion(PACKAGE_VERSION)
                .packagePath(PackageUtil.buildAppPackagePath(BUCKET_NAME, APP_ID, PACKAGE_VERSION))
                .packageCreator(PACKAGE_CREATOR)
                .packageMd5(PACKAGE_MD5)
                .componentCount(1L)
                .version(0)
                .appSchema(APP_SCHEMA)
                .swapp(SWAPP)
                .build();
        Mockito.when(appPackageService.createByStream(Mockito.any())).thenReturn(appPackageDO);
        Mockito.when(appPackageService.get(AppPackageQueryCondition.builder().id(1L).build())).thenReturn(appPackageDO);

        Long dagInstId = 10L;
        Mockito.when(dagInstService.start(Mockito.eq(UnpackAppPackageFromStorageDag.name),
                        Mockito.any(JSONObject.class), Mockito.eq(true)))
                .thenReturn(dagInstId);
        Mockito.when(dagInstNewService.getDagInstById(dagInstId)).thenReturn(DagInstDO.builder()
                .status(DagInstStatus.SUCCESS.toString())
                .build());

        boolean forceFlag = false;
        boolean resetVersionFlag = true;
        AppPackageImportReq request = AppPackageImportReq.builder()
                .appId(APP_ID)
                .packageVersion(PACKAGE_VERSION)
                .packageCreator(PACKAGE_CREATOR)
                .force(forceFlag)
                .resetVersion(resetVersionFlag)
                .build();
        AppPackageDTO appPackageDTO = appPackageProvider.importPackage(request, BODY, PACKAGE_CREATOR);
        log.info("appPackageDTO: {}", JSONObject.toJSONString(appPackageDTO));

        // 验证导入后 DTO 对象内容
        assertThat(appPackageDTO.getId()).isEqualTo(appPackageDO.getId());
        assertThat(appPackageDTO.getAppId()).isEqualTo(appPackageDO.getAppId());
        assertThat(appPackageDTO.getAppName()).isEqualTo(appPackageDO.getAppId());
        assertThat(appPackageDTO.getPackageVersion()).isEqualTo(appPackageDO.getPackageVersion());
        assertThat(appPackageDTO.getPackagePath()).isEqualTo(appPackageDO.getPackagePath());
        assertThat(appPackageDTO.getPackageMd5()).isEqualTo(appPackageDO.getPackageMd5());
        assertThat(appPackageDTO.getPackageCreator()).isEqualTo(appPackageDO.getPackageCreator());
        assertThat(appPackageDTO.getAppSchema()).isEqualTo(appPackageDO.getAppSchema());
        assertThat(appPackageDTO.getComponentCount()).isEqualTo(appPackageDO.getComponentCount());
        assertThat(appPackageDTO.getSwapp()).isEqualTo(appPackageDO.getSwapp());
        assertThat(appPackageDTO.getSimplePackageVersion()).isEqualTo(VersionUtil.clear(appPackageDO.getPackageVersion()));

        // 验证 DAG 启动参数
        ArgumentCaptor<String> argName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<JSONObject> argGlobalVariables = ArgumentCaptor.forClass(JSONObject.class);
        ArgumentCaptor<Boolean> argIsStandalone = ArgumentCaptor.forClass(Boolean.class);
        Mockito.verify(dagInstService).start(argName.capture(), argGlobalVariables.capture(), argIsStandalone.capture());
        assertThat(argName.getValue()).isEqualTo(UnpackAppPackageFromStorageDag.name);
        assertThat(argGlobalVariables.getValue().getString(DefaultConstant.DAG_TYPE)).isEqualTo(DagTypeEnum.UNPACK_APP_PACKAGE.toString());
        assertThat(argGlobalVariables.getValue().getString(UnpackAppPackageVariableKey.APP_ID)).isEqualTo(APP_ID);
        assertThat(argGlobalVariables.getValue().getLong(UnpackAppPackageVariableKey.APP_PACKAGE_ID)).isEqualTo(appPackageDO.getId());
        assertThat(argGlobalVariables.getValue().getBoolean(UnpackAppPackageVariableKey.FORCE)).isEqualTo(forceFlag);
        assertThat(argGlobalVariables.getValue().getBoolean(UnpackAppPackageVariableKey.RESET_VERSION)).isEqualTo(resetVersionFlag);
        assertThat(argIsStandalone.getValue()).isEqualTo(true);
    }
}
