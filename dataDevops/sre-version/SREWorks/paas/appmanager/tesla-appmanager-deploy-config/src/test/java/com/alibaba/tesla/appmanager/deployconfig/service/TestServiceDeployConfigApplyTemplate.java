package com.alibaba.tesla.appmanager.deployconfig.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.deployconfig.repository.DeployConfigHistoryRepository;
import com.alibaba.tesla.appmanager.deployconfig.repository.DeployConfigRepository;
import com.alibaba.tesla.appmanager.deployconfig.repository.condition.DeployConfigHistoryQueryCondition;
import com.alibaba.tesla.appmanager.deployconfig.repository.condition.DeployConfigQueryCondition;
import com.alibaba.tesla.appmanager.deployconfig.repository.domain.DeployConfigDO;
import com.alibaba.tesla.appmanager.deployconfig.repository.domain.DeployConfigHistoryDO;
import com.alibaba.tesla.appmanager.deployconfig.service.impl.DeployConfigServiceImpl;
import com.alibaba.tesla.appmanager.domain.container.DeployConfigTypeId;
import com.alibaba.tesla.appmanager.domain.req.deployconfig.DeployConfigApplyTemplateReq;
import com.alibaba.tesla.appmanager.domain.res.deployconfig.DeployConfigApplyTemplateRes;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@Slf4j
public class TestServiceDeployConfigApplyTemplate {

    private static final String APP_ID = "testapp";
    private static final String API_VERSION = DefaultConstant.API_VERSION_V1_ALPHA2;

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

    @Test
    public void testWithExistsRecords() throws Exception {
        String parameterValueTypeId = new DeployConfigTypeId(DeployConfigTypeId.TYPE_PARAMETER_VALUES).toString();
        String componentTypeId = new DeployConfigTypeId(ComponentTypeEnum.K8S_MICROSERVICE, "aiops-server").toString();
        Mockito.doReturn(Collections.singletonList(DeployConfigHistoryDO.builder().revision(10).build()))
                .when(deployConfigHistoryRepository)
                .selectByExample(DeployConfigHistoryQueryCondition.builder()
                        .appId(APP_ID)
                        .typeId(parameterValueTypeId)
                        .envId("")
                        .apiVersion(API_VERSION)
                        .page(1)
                        .pageSize(1)
                        .build());
        Mockito.doReturn(Collections.singletonList(DeployConfigHistoryDO.builder().revision(20).build()))
                .when(deployConfigHistoryRepository)
                .selectByExample(DeployConfigHistoryQueryCondition.builder()
                        .appId(APP_ID)
                        .typeId(componentTypeId)
                        .envId("")
                        .apiVersion(API_VERSION)
                        .page(1)
                        .pageSize(1)
                        .build());
        Mockito.doReturn(Collections.singletonList(DeployConfigDO.builder()
                        .appId(APP_ID)
                        .typeId(parameterValueTypeId)
                        .envId("")
                        .apiVersion(API_VERSION)
                        .currentRevision(10)
                        .config("")
                        .enabled(true)
                        .build()))
                .when(deployConfigRepository)
                .selectByCondition(DeployConfigQueryCondition.builder()
                        .appId(APP_ID)
                        .typeId(parameterValueTypeId)
                        .envId("")
                        .apiVersion(API_VERSION)
                        .build());

        String config = getConfig();
        DeployConfigApplyTemplateRes<DeployConfigDO> res = deployConfigService.applyTemplate(
                DeployConfigApplyTemplateReq.builder()
                        .appId(APP_ID)
                        .apiVersion(API_VERSION)
                        .config(config)
                        .enabled(true)
                        .build());
        log.info("testK8sMicroservice: {}", JSONObject.toJSONString(res));
        Assertions.assertThat(res.getItems().size()).isEqualTo(2);
        assertThat(res.getItems().get(0).getApiVersion()).isEqualTo(API_VERSION);
        assertThat(res.getItems().get(0).getAppId()).isEqualTo(APP_ID);
        assertThat(res.getItems().get(0).getEnvId()).isEqualTo("");
        assertThat(res.getItems().get(0).getCurrentRevision()).isEqualTo(11);
        assertThat(res.getItems().get(0).getTypeId()).isEqualTo(parameterValueTypeId);
        assertThat(res.getItems().get(0).getInherit()).isFalse();
        assertThat(res.getItems().get(1).getApiVersion()).isEqualTo(API_VERSION);
        assertThat(res.getItems().get(1).getAppId()).isEqualTo(APP_ID);
        assertThat(res.getItems().get(1).getEnvId()).isEqualTo("");
        assertThat(res.getItems().get(1).getCurrentRevision()).isEqualTo(21);
        assertThat(res.getItems().get(1).getTypeId()).isEqualTo(componentTypeId);
        assertThat(res.getItems().get(1).getInherit()).isFalse();

        DeployAppSchema schema = SchemaUtil.toSchema(DeployAppSchema.class, config);

        // 验证 DB 存储方法
        Mockito.verify(deployConfigHistoryRepository, Mockito.times(1)).insertSelective(DeployConfigHistoryDO.builder()
                .appId(APP_ID)
                .typeId(parameterValueTypeId)
                .envId("")
                .apiVersion(API_VERSION)
                .revision(11)
                .config(SchemaUtil.toYamlStr(schema.getSpec().getParameterValues(), DeployAppSchema.ParameterValue.class))
                .inherit(false)
                .deleted(false)
                .build());
        Mockito.verify(deployConfigHistoryRepository, Mockito.times(1)).insertSelective(DeployConfigHistoryDO.builder()
                .appId(APP_ID)
                .typeId(componentTypeId)
                .envId("")
                .apiVersion(API_VERSION)
                .revision(21)
                .config(SchemaUtil.toYamlMapStr(schema.getSpec().getComponents().get(0)))
                .inherit(false)
                .deleted(false)
                .build());
        Mockito.verify(deployConfigRepository, Mockito.times(1)).updateByCondition(DeployConfigDO.builder()
                        .appId(APP_ID)
                        .typeId(parameterValueTypeId)
                        .envId("")
                        .apiVersion(API_VERSION)
                        .currentRevision(11)
                        .config(SchemaUtil.toYamlStr(schema.getSpec().getParameterValues(), DeployAppSchema.ParameterValue.class))
                        .enabled(true)
                        .inherit(false)
                        .build(),
                DeployConfigQueryCondition.builder()
                        .appId(APP_ID)
                        .typeId(parameterValueTypeId)
                        .envId("")
                        .apiVersion(API_VERSION)
                        .build());
        Mockito.verify(deployConfigRepository, Mockito.times(1)).insert(DeployConfigDO.builder()
                .appId(APP_ID)
                .typeId(componentTypeId)
                .envId("")
                .apiVersion(API_VERSION)
                .currentRevision(21)
                .config(SchemaUtil.toYamlMapStr(schema.getSpec().getComponents().get(0)))
                .enabled(true)
                .inherit(false)
                .build());
    }

    @Test
    public void testWithNoRecords() throws Exception {
        String parameterValueTypeId = new DeployConfigTypeId(DeployConfigTypeId.TYPE_PARAMETER_VALUES).toString();
        String componentTypeId = new DeployConfigTypeId(ComponentTypeEnum.K8S_MICROSERVICE, "aiops-server").toString();
        String config = getConfig();
        DeployConfigApplyTemplateRes<DeployConfigDO> res = deployConfigService.applyTemplate(
                DeployConfigApplyTemplateReq.builder()
                        .appId(APP_ID)
                        .apiVersion(API_VERSION)
                        .config(config)
                        .enabled(true)
                        .build());
        log.info("testK8sMicroservice: {}", JSONObject.toJSONString(res));
        Assertions.assertThat(res.getItems().size()).isEqualTo(2);
        assertThat(res.getItems().get(0).getApiVersion()).isEqualTo(API_VERSION);
        assertThat(res.getItems().get(0).getAppId()).isEqualTo(APP_ID);
        assertThat(res.getItems().get(0).getEnvId()).isEqualTo("");
        assertThat(res.getItems().get(0).getCurrentRevision()).isEqualTo(0);
        assertThat(res.getItems().get(0).getTypeId()).isEqualTo(parameterValueTypeId);
        assertThat(res.getItems().get(1).getApiVersion()).isEqualTo(API_VERSION);
        assertThat(res.getItems().get(1).getAppId()).isEqualTo(APP_ID);
        assertThat(res.getItems().get(1).getEnvId()).isEqualTo("");
        assertThat(res.getItems().get(1).getCurrentRevision()).isEqualTo(0);
        assertThat(res.getItems().get(1).getTypeId()).isEqualTo(componentTypeId);

        DeployAppSchema schema = SchemaUtil.toSchema(DeployAppSchema.class, config);

        // 验证 DB 存储方法
        Mockito.verify(deployConfigHistoryRepository, Mockito.times(1)).insertSelective(DeployConfigHistoryDO.builder()
                .appId(APP_ID)
                .typeId(parameterValueTypeId)
                .envId("")
                .apiVersion(API_VERSION)
                .revision(0)
                .config(SchemaUtil.toYamlStr(schema.getSpec().getParameterValues(), DeployAppSchema.ParameterValue.class))
                .inherit(false)
                .deleted(false)
                .build());
        Mockito.verify(deployConfigHistoryRepository, Mockito.times(1)).insertSelective(DeployConfigHistoryDO.builder()
                .appId(APP_ID)
                .typeId(componentTypeId)
                .envId("")
                .apiVersion(API_VERSION)
                .revision(0)
                .config(SchemaUtil.toYamlMapStr(schema.getSpec().getComponents().get(0)))
                .inherit(false)
                .deleted(false)
                .build());
        Mockito.verify(deployConfigRepository, Mockito.times(1)).insert(DeployConfigDO.builder()
                .appId(APP_ID)
                .typeId(parameterValueTypeId)
                .envId("")
                .apiVersion(API_VERSION)
                .currentRevision(0)
                .config(SchemaUtil.toYamlStr(schema.getSpec().getParameterValues(), DeployAppSchema.ParameterValue.class))
                .inherit(false)
                .enabled(true)
                .build());
        Mockito.verify(deployConfigRepository, Mockito.times(1)).insert(DeployConfigDO.builder()
                .appId(APP_ID)
                .typeId(componentTypeId)
                .envId("")
                .apiVersion(API_VERSION)
                .currentRevision(0)
                .config(SchemaUtil.toYamlMapStr(schema.getSpec().getComponents().get(0)))
                .inherit(false)
                .enabled(true)
                .build());
    }

    private String getConfig() throws Exception {
        Resource config = new ClassPathResource("fixtures/application_configuration/sreworks_k8s_microservice_aiops_api.yaml");
        String configPath = config.getFile().getAbsolutePath();
        return new String(Files.readAllBytes(Paths.get(configPath)), StandardCharsets.UTF_8);
    }
}
