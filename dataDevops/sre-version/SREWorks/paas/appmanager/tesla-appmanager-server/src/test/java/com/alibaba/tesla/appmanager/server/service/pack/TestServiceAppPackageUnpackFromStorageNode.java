package com.alibaba.tesla.appmanager.server.service.pack;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.autoconfig.PackageProperties;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.constants.UnpackAppPackageVariableKey;
import com.alibaba.tesla.appmanager.common.util.PackageUtil;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageCreateByLocalFileReq;
import com.alibaba.tesla.appmanager.domain.schema.AppPackageSchema;
import com.alibaba.tesla.appmanager.server.repository.AppPackageRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDO;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageTagService;
import com.alibaba.tesla.appmanager.server.service.componentpackage.ComponentPackageService;
import com.alibaba.tesla.appmanager.server.service.pack.dag.UnpackAppPackageFromStorageNode;
import com.alibaba.tesla.appmanager.server.storage.Storage;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.file.Path;
import java.util.Collections;

/**
 * 测试单个应用包从 storage 解压到 component package 插入的功能
 */
@RunWith(SpringRunner.class)
@Slf4j
public class TestServiceAppPackageUnpackFromStorageNode {

    private static final String BUCKET_NAME = "appmanager";
    private static final String APP_ID = "testapp";
    private static final String PACKAGE_CREATOR = "SYSTEM";
    private static final String PACKAGE_VERSION = "2.1.1+20220201234212";
    private static final String PACKAGE_MD5 = "433966fbc2ea534ca32c706ab41f60ff";
    private static final String APP_SCHEMA = "{\"a\": \"b\"}";
    private static final String SWAPP = "{\"c\": \"d\"}";
    private static final boolean FORCE = false;
    private static final boolean RESET_VERSION = true;
    private static final String OBJECT_URL = "http://fake_url";
    private static final String META_YAML = "" +
            "appId: tianji\n" +
            "packageCreator: SYSTEM\n" +
            "packageVersion: 5.0.0+20220118040208783257\n" +
            "tags:\n" +
            "- build=v3.16xR-x86\n" +
            "componentPackages:\n" +
            "- componentName: productops\n" +
            "  componentType: INTERNAL_ADDON\n" +
            "  packageCreator: SYSTEM\n" +
            "  packageExt: |\n" +
            "    apiVersion: core.oam.dev/v1alpha2\n" +
            "    kind: Component\n" +
            "    metadata:\n" +
            "      name: internal-addon-tianji-productops\n" +
            "      annotations:\n" +
            "        annotations.appmanager.oam.dev/version: \"7.5.5+20220118040208550904\"\n" +
            "      labels:\n" +
            "        labels.appmanager.oam.dev/appId: \"tianji\"\n" +
            "        labels.appmanager.oam.dev/componentName: \"productops\"\n" +
            "        labels.appmanager.oam.dev/stageId: \"PLACEHOLDER_STAGE_ID\"\n" +
            "        labels.appmanager.oam.dev/clusterId: \"PLACEHOLDER_CLUSTER_ID\"\n" +
            "        appId: \"tianji\"\n" +
            "        componentName: \"productops\"\n" +
            "        stageId: \"PLACEHOLDER_STAGE_ID\"\n" +
            "    spec:\n" +
            "      workload:\n" +
            "        apiVersion: apps.abm.io/v1\n" +
            "        kind: ProductopsConfig\n" +
            "        metadata:\n" +
            "          namespace: \"PLACEHOLDER_NAMESPACE_ID\"\n" +
            "          name: \"PLACEHOLDER_NAME\"\n" +
            "          labels:\n" +
            "            labels.appmanager.oam.dev/stageId: \"PLACEHOLDER_STAGE_ID\"\n" +
            "            labels.appmanager.oam.dev/appId: \"tianji\"\n" +
            "            labels.appmanager.oam.dev/componentName: \"productops\"\n" +
            "            labels.appmanager.oam.dev/clusterId: \"PLACEHOLDER_CLUSTER_ID\"\n" +
            "            stageId: \"PLACEHOLDER_STAGE_ID\"\n" +
            "            appId: \"tianji\"\n" +
            "            componentName: \"productops\"\n" +
            "          annotations:\n" +
            "            annotations.appmanager.oam.dev/deployAppId: \"PLACEHOLDER_DEPLOY_APP_ID\"\n" +
            "            annotations.appmanager.oam.dev/deployComponentId: \"PLACEHOLDER_DEPLOY_COMPONENT_ID\"\n" +
            "            annotations.appmanager.oam.dev/version: \"7.5.5+20220118040208550904\"\n" +
            "            annotations.appmanager.oam.dev/appInstanceId: \"PLACEHOLDER_APP_INSTANCE_ID\"\n" +
            "            annotations.appmanager.oam.dev/componentInstanceId: \"PLACEHOLDER_COMPONENT_INSTANCE_ID\"\n" +
            "        spec:\n" +
            "          stageId: ''\n" +
            "  packageMd5: 04cc7b23f458a96526ccc1edb0a56be3\n" +
            "  packageOptions: '{\"endpoint\":\"http://fake_url\",\"envIds\":\"prod\"}'\n" +
            "  packageVersion: 7.5.5+20220118040208550904\n" +
            "  relativePath: INTERNAL_ADDON_productops.zip";

    @Mock
    private AppPackageRepository appPackageRepository;

    @Mock
    private AppPackageTagService appPackageTagService;

    @Mock
    private ComponentPackageService componentPackageService;

    @Mock
    private Storage storage;

    @Mock
    private PackageProperties packageProperties;

    private UnpackAppPackageFromStorageNode unpackAppPackageFromStorageNode;

    @Test
    public void testRun() throws Exception {
        unpackAppPackageFromStorageNode = Mockito.spy(new UnpackAppPackageFromStorageNode());
        Mockito.doReturn(appPackageRepository).when(unpackAppPackageFromStorageNode).getAppPackageRepository();
        Mockito.doReturn(appPackageTagService).when(unpackAppPackageFromStorageNode).getAppPackageTagService();
        Mockito.doReturn(componentPackageService).when(unpackAppPackageFromStorageNode).getComponentPackageService();
        Mockito.doReturn(storage).when(unpackAppPackageFromStorageNode).getStorage();
        Mockito.doReturn(packageProperties).when(unpackAppPackageFromStorageNode).getPackageProperties();

        // 准备数据
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
        Long componentPackageId = 10L;
        Mockito.doReturn(appPackageDO)
                .when(appPackageRepository)
                .getByCondition(AppPackageQueryCondition.builder()
                        .id(appPackageDO.getId())
                        .withBlobs(true)
                        .build());
        Mockito.doReturn(OBJECT_URL)
                .when(storage)
                .getObjectUrl(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doNothing()
                .when(unpackAppPackageFromStorageNode)
                .copyURLToFile(Mockito.isA(String.class), Mockito.isA(String.class));
        Mockito.doReturn(META_YAML)
                .when(unpackAppPackageFromStorageNode)
                .readMetaYaml(Mockito.isA(String.class), Mockito.isA(Path.class));
        Mockito.doReturn(PACKAGE_MD5)
                .when(unpackAppPackageFromStorageNode)
                .readFileMd5(Mockito.isA(String.class));
        // 针对 component package service 中的 createByLocalFile，返回对象携带 id 即可测试
        Mockito.doReturn(ComponentPackageDO.builder().id(componentPackageId).build())
                .when(componentPackageService)
                .createByLocalFile(Mockito.isA(ComponentPackageCreateByLocalFileReq.class));
        JSONObject globalVariable = new JSONObject();
        globalVariable.put(UnpackAppPackageVariableKey.APP_PACKAGE_ID, appPackageDO.getId());
        globalVariable.put(UnpackAppPackageVariableKey.FORCE, FORCE);
        globalVariable.put(UnpackAppPackageVariableKey.RESET_VERSION, RESET_VERSION);
        unpackAppPackageFromStorageNode.setGlobalVariable(globalVariable);

        // Run
        unpackAppPackageFromStorageNode.run();

        // 测试函数调用情况
        Mockito.verify(appPackageRepository, Mockito.times(1)).getByCondition(AppPackageQueryCondition.builder()
                .id(appPackageDO.getId())
                .withBlobs(true)
                .build());
        Mockito.verify(storage, Mockito.times(1))
                .getObjectUrl(BUCKET_NAME, PackageUtil.buildAppPackageRemotePath(APP_ID, PACKAGE_VERSION),
                        DefaultConstant.DEFAULT_FILE_EXPIRATION);
        Mockito.verify(unpackAppPackageFromStorageNode, Mockito.times(1))
                .copyURLToFile(Mockito.eq(OBJECT_URL), Mockito.isA(String.class));
        Mockito.verify(unpackAppPackageFromStorageNode, Mockito.times(1))
                .readFileMd5(Mockito.isA(String.class));
        Mockito.verify(unpackAppPackageFromStorageNode, Mockito.times(1))
                .readMetaYaml(Mockito.isA(String.class), Mockito.isA(Path.class));
        Mockito.verify(componentPackageService, Mockito.times(1))
                .createByLocalFile(Mockito.isA(ComponentPackageCreateByLocalFileReq.class));
        Mockito.verify(componentPackageService, Mockito.times(1))
                .addComponentPackageRelation(APP_ID, appPackageDO.getId(),
                        Collections.singletonList(componentPackageId));
        appPackageDO.setAppSchema(SchemaUtil.toYamlMapStr(SchemaUtil.toSchema(AppPackageSchema.class, META_YAML)));
        Mockito.verify(appPackageRepository, Mockito.times(1))
                .updateByPrimaryKeySelective(appPackageDO);
        Mockito.verify(appPackageTagService, Mockito.times(1))
                .batchUpdate(appPackageDO.getId(), Collections.singletonList("build=v3.16xR-x86"));
    }
}
