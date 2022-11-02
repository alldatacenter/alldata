package com.alibaba.tesla.appmanager.deployconfig.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SpringRunner.class)
@Slf4j
public class TestServiceDeployConfigGenerateByConfiguration {

    private static final String APP_ID = "testapp";
    private static final String API_VERSION = DefaultConstant.API_VERSION_V1_ALPHA2;
    private static final String APP_INSTANCE_NAME = "testinstance";
    private static final long APP_PACKAGE_ID = 10L;
    private static final String CLUSTER_ID = "master";
    private static final String NAMESPACE_ID = "default";
    private static final String STAGE_ID = "prod";

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
     * 测试生成 K8S Microservice 的 ApplicationConfiguration
     */
    @Test
    public void testGenerateK8SMicroserviceNoInherit() throws Exception {
        // 准备数据
        String parameterValueTypeId = new DeployConfigTypeId(DeployConfigTypeId.TYPE_PARAMETER_VALUES).toString();
        String componentTypeId = new DeployConfigTypeId(ComponentTypeEnum.K8S_MICROSERVICE, "aiops-server").toString();
        String config = getConfig();
        DeployAppSchema schema = SchemaUtil.toSchema(DeployAppSchema.class, config);
        List<DeployConfigDO> mockResults = new ArrayList<>();
        mockResults.add(DeployConfigDO.builder()
                .appId(APP_ID)
                .typeId(parameterValueTypeId)
                .envId("")
                .apiVersion(API_VERSION)
                .currentRevision(0)
                .config(SchemaUtil.toYamlStr(schema.getSpec().getParameterValues(), DeployAppSchema.ParameterValue.class))
                .enabled(true)
                .inherit(false)
                .build());
        mockResults.add(DeployConfigDO.builder()
                .appId(APP_ID)
                .typeId(componentTypeId)
                .envId("")
                .apiVersion(API_VERSION)
                .currentRevision(0)
                .config(SchemaUtil.toYamlMapStr(schema.getSpec().getComponents().get(0)))
                .enabled(true)
                .inherit(false)
                .build());
        Mockito.doReturn(mockResults)
                .when(deployConfigRepository)
                .selectByCondition(DeployConfigQueryCondition.builder()
                        .apiVersion(API_VERSION)
                        .appId(APP_ID)
                        .enabled(true)
                        .build());

        // 测试调用
        DeployConfigGenerateReq req = DeployConfigGenerateReq.builder()
                .apiVersion(API_VERSION)
                .appId(APP_ID)
                .appPackageId(APP_PACKAGE_ID)
                .clusterId(CLUSTER_ID)
                .namespaceId(NAMESPACE_ID)
                .stageId(STAGE_ID)
                .appInstanceName(APP_INSTANCE_NAME)
                .build();
        DeployConfigGenerateRes res = deployConfigService.generate(req);
        log.info("generateRes: {}", JSONObject.toJSONString(res.getSchema()));
        DeployAppSchema generatedSchema = res.getSchema();

        // 验证返回结果
        Assertions.assertThat(generatedSchema.getApiVersion()).isEqualTo(schema.getApiVersion());
        Assertions.assertThat(generatedSchema.getKind()).isEqualTo(schema.getKind());
        Assertions.assertThat(generatedSchema.getMetadata().getAnnotations().getAppId()).isEqualTo(APP_ID);
        Assertions.assertThat(generatedSchema.getMetadata().getAnnotations().getAppInstanceName()).isEqualTo(APP_INSTANCE_NAME);
        Assertions.assertThat(generatedSchema.getMetadata().getAnnotations().getClusterId()).isEqualTo(CLUSTER_ID);
        Assertions.assertThat(generatedSchema.getMetadata().getAnnotations().getNamespaceId()).isEqualTo(NAMESPACE_ID);
        Assertions.assertThat(generatedSchema.getMetadata().getAnnotations().getStageId()).isEqualTo(STAGE_ID);
        Assertions.assertThat(generatedSchema.getMetadata().getAnnotations().getAppPackageId()).isEqualTo(APP_PACKAGE_ID);
        Assertions.assertThat(SchemaUtil.toYamlStr(generatedSchema.getSpec().getParameterValues(), DeployAppSchema.ParameterValue.class))
                .isEqualTo(SchemaUtil.toYamlStr(schema.getSpec().getParameterValues(), DeployAppSchema.ParameterValue.class));
        Assertions.assertThat(generatedSchema.getSpec().getComponents().get(0).getScopes().get(0).getScopeRef().getName()).isEqualTo(CLUSTER_ID);
        Assertions.assertThat(generatedSchema.getSpec().getComponents().get(0).getScopes().get(1).getScopeRef().getName()).isEqualTo(NAMESPACE_ID);
        Assertions.assertThat(generatedSchema.getSpec().getComponents().get(0).getScopes().get(2).getScopeRef().getName()).isEqualTo(STAGE_ID);
    }

    /**
     * 测试生成 K8S Microservice 的 ApplicationConfiguration (带继承关系)
     */
    @Test
    public void testGenerateK8SMicroserviceWithInherit() throws Exception {
        // 准备数据
        String parameterValueTypeId = new DeployConfigTypeId(DeployConfigTypeId.TYPE_PARAMETER_VALUES).toString();
        String componentTypeId = new DeployConfigTypeId(ComponentTypeEnum.K8S_MICROSERVICE, "aiops-server").toString();
        String config = getConfig();
        DeployAppSchema schema = SchemaUtil.toSchema(DeployAppSchema.class, config);
        Mockito.doReturn(
                        Arrays.asList(DeployConfigDO.builder()
                                        .appId(APP_ID)
                                        .typeId(parameterValueTypeId)
                                        .envId("")
                                        .apiVersion(API_VERSION)
                                        .currentRevision(0)
                                        .config(SchemaUtil.toYamlStr(schema.getSpec().getParameterValues(), DeployAppSchema.ParameterValue.class))
                                        .enabled(true)
                                        .inherit(false)
                                        .build(),
                                DeployConfigDO.builder()
                                        .appId(APP_ID)
                                        .typeId(componentTypeId)
                                        .envId("")
                                        .apiVersion(API_VERSION)
                                        .currentRevision(0)
                                        .config("")
                                        .enabled(true)
                                        .inherit(true)
                                        .build()
                        ))
                .when(deployConfigRepository)
                .selectByCondition(DeployConfigQueryCondition.builder()
                        .apiVersion(API_VERSION)
                        .appId(APP_ID)
                        .enabled(true)
                        .build());
        Mockito.doReturn(
                        Collections.singletonList(DeployConfigDO.builder()
                                .appId("")
                                .typeId(componentTypeId)
                                .envId("")
                                .apiVersion(API_VERSION)
                                .currentRevision(0)
                                .config(SchemaUtil.toYamlMapStr(schema.getSpec().getComponents().get(0)))
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
                .appPackageId(APP_PACKAGE_ID)
                .clusterId(CLUSTER_ID)
                .namespaceId(NAMESPACE_ID)
                .stageId(STAGE_ID)
                .appInstanceName(APP_INSTANCE_NAME)
                .build();
        DeployConfigGenerateRes res = deployConfigService.generate(req);
        log.info("generateRes: {}", JSONObject.toJSONString(res.getSchema()));
        DeployAppSchema generatedSchema = res.getSchema();

        // 验证返回结果
        Assertions.assertThat(generatedSchema.getApiVersion()).isEqualTo(schema.getApiVersion());
        Assertions.assertThat(generatedSchema.getKind()).isEqualTo(schema.getKind());
        Assertions.assertThat(generatedSchema.getMetadata().getAnnotations().getAppId()).isEqualTo(APP_ID);
        Assertions.assertThat(generatedSchema.getMetadata().getAnnotations().getAppInstanceName()).isEqualTo(APP_INSTANCE_NAME);
        Assertions.assertThat(generatedSchema.getMetadata().getAnnotations().getClusterId()).isEqualTo(CLUSTER_ID);
        Assertions.assertThat(generatedSchema.getMetadata().getAnnotations().getNamespaceId()).isEqualTo(NAMESPACE_ID);
        Assertions.assertThat(generatedSchema.getMetadata().getAnnotations().getStageId()).isEqualTo(STAGE_ID);
        Assertions.assertThat(generatedSchema.getMetadata().getAnnotations().getAppPackageId()).isEqualTo(APP_PACKAGE_ID);
        Assertions.assertThat(SchemaUtil.toYamlStr(generatedSchema.getSpec().getParameterValues(), DeployAppSchema.ParameterValue.class))
                .isEqualTo(SchemaUtil.toYamlStr(schema.getSpec().getParameterValues(), DeployAppSchema.ParameterValue.class));
        Assertions.assertThat(generatedSchema.getSpec().getComponents().get(0).getScopes().get(0).getScopeRef().getName()).isEqualTo(CLUSTER_ID);
        Assertions.assertThat(generatedSchema.getSpec().getComponents().get(0).getScopes().get(1).getScopeRef().getName()).isEqualTo(NAMESPACE_ID);
        Assertions.assertThat(generatedSchema.getSpec().getComponents().get(0).getScopes().get(2).getScopeRef().getName()).isEqualTo(STAGE_ID);
    }

    private String getConfig() throws Exception {
        Resource config = new ClassPathResource("fixtures/application_configuration/sreworks_k8s_microservice_aiops_api.yaml");
        String configPath = config.getFile().getAbsolutePath();
        return new String(Files.readAllBytes(Paths.get(configPath)), StandardCharsets.UTF_8);
    }
}
