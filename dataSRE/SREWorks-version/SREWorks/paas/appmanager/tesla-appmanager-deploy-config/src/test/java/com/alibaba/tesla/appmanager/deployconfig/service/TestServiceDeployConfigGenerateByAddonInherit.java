package com.alibaba.tesla.appmanager.deployconfig.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.deployconfig.repository.DeployConfigHistoryRepository;
import com.alibaba.tesla.appmanager.deployconfig.repository.DeployConfigRepository;
import com.alibaba.tesla.appmanager.deployconfig.repository.condition.DeployConfigQueryCondition;
import com.alibaba.tesla.appmanager.deployconfig.repository.domain.DeployConfigDO;
import com.alibaba.tesla.appmanager.deployconfig.service.impl.DeployConfigServiceImpl;
import com.alibaba.tesla.appmanager.domain.container.DeployConfigTypeId;
import com.alibaba.tesla.appmanager.domain.req.deployconfig.DeployConfigGenerateReq;
import com.alibaba.tesla.appmanager.domain.res.deployconfig.DeployConfigGenerateRes;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@Slf4j
public class TestServiceDeployConfigGenerateByAddonInherit {

    private static final String APP_ID = "testapp";
    private static final String API_VERSION = DefaultConstant.API_VERSION_V1_ALPHA2;
    private static final String CONFIG = "revisionName: INTERNAL_ADDON|productopsv2|_\n" +
            "scopes:\n" +
            "  - scopeRef:\n" +
            "      apiVersion: flyadmin.alibaba.com/v1alpha1\n" +
            "      kind: Cluster\n" +
            "      name: \"\"\n" +
            "  - scopeRef:\n" +
            "      apiVersion: flyadmin.alibaba.com/v1alpha1\n" +
            "      kind: Namespace\n" +
            "      name: \"\"\n" +
            "  - scopeRef:\n" +
            "      apiVersion: flyadmin.alibaba.com/v1alpha1\n" +
            "      kind: Stage\n" +
            "      name: \"\"\n" +
            "parameterValues: []";

    @Mock
    private DeployConfigRepository deployConfigRepository;

    @Mock
    private DeployConfigHistoryRepository deployConfigHistoryRepository;

    private DeployConfigService deployConfigService;

    @Before
    public void before() {
        deployConfigService = Mockito.spy(new DeployConfigServiceImpl(
                deployConfigRepository,
                deployConfigHistoryRepository
        ));
    }

    /**
     * 测试生成 Internal Addon 的 ApplicationConfiguration (带继承关系)
     */
    @Test
    public void testGenerateInternalAddonWithInherit() throws Exception {
        // 准备数据
        String typeId = new DeployConfigTypeId(
                ComponentTypeEnum.INTERNAL_ADDON, "productopsv2@productopsv2").toString();
        Mockito.doReturn(
                        Collections.singletonList(DeployConfigDO.builder()
                                .appId(APP_ID)
                                .typeId(typeId)
                                .envId("")
                                .apiVersion(API_VERSION)
                                .currentRevision(0)
                                .config("")
                                .enabled(true)
                                .inherit(true)
                                .build()))
                .when(deployConfigRepository)
                .selectByCondition(DeployConfigQueryCondition.builder()
                        .apiVersion(API_VERSION)
                        .appId(APP_ID)
                        .enabled(true)
                        .build());
        Mockito.doReturn(
                        Collections.singletonList(DeployConfigDO.builder()
                                .appId("")
                                .typeId(typeId)
                                .envId("")
                                .apiVersion(API_VERSION)
                                .currentRevision(0)
                                .config(CONFIG)
                                .enabled(true)
                                .inherit(false)
                                .build()))
                .when(deployConfigRepository)
                .selectByCondition(DeployConfigQueryCondition.builder()
                        .apiVersion(API_VERSION)
                        .appId("")
                        .enabled(true)
                        .build());

        // 测试调用
        DeployConfigGenerateReq req = DeployConfigGenerateReq.builder()
                .apiVersion(API_VERSION)
                .appId(APP_ID)
                .appPackageId(0L)
                .build();
        DeployConfigGenerateRes res = deployConfigService.generate(req);
        log.info("generateRes: {}", JSONObject.toJSONString(res.getSchema()));
        DeployAppSchema generatedSchema = res.getSchema();
        assertThat(generatedSchema.getApiVersion()).isEqualTo(API_VERSION);
        assertThat(generatedSchema.getKind()).isEqualTo("ApplicationConfiguration");
        assertThat(generatedSchema.getMetadata().getAnnotations().getAppId()).isEqualTo(APP_ID);
        assertThat(generatedSchema.getSpec().getComponents().size()).isEqualTo(1);
    }
}
