package com.alibaba.tesla.appmanager.server.dag;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.AppFlowParamKey;
import com.alibaba.tesla.appmanager.common.constants.AppFlowVariableKey;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.definition.repository.condition.DefinitionSchemaQueryCondition;
import com.alibaba.tesla.appmanager.definition.repository.domain.DefinitionSchemaDO;
import com.alibaba.tesla.appmanager.definition.service.DefinitionSchemaService;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import com.alibaba.tesla.appmanager.dynamicscript.core.GroovyHandlerFactory;
import com.alibaba.tesla.appmanager.server.dag.nodes.DeployAppTraitNode;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentDO;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployAppService;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployComponentService;
import com.alibaba.tesla.appmanager.spring.util.FixtureUtil;
import com.alibaba.tesla.appmanager.trait.TraitFactory;
import com.alibaba.tesla.appmanager.trait.repository.condition.TraitQueryCondition;
import com.alibaba.tesla.appmanager.trait.repository.domain.TraitDO;
import com.alibaba.tesla.appmanager.trait.service.TraitService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(SpringRunner.class)
@Slf4j
public class TestDeployAppTraitNode {

    private static final Long DEPLOY_APP_ID = 11L;
    private static final String DEFINITION_REF = "resourceLimit.k8s.schema.abm.io";

    @Mock
    private DeployAppService deployAppService;

    @Mock
    private DeployComponentService deployComponentService;

    @Mock
    private GroovyHandlerFactory groovyHandlerFactory;

    @Mock
    private TraitService traitService;

    @Mock
    private DefinitionSchemaService definitionSchemaService;

    private TraitFactory traitFactory;

    private DeployAppTraitNode traitNode;

    @Before
    public void init() throws Exception {
        MockitoAnnotations.openMocks(this);

        traitFactory = new TraitFactory(traitService, definitionSchemaService);
        traitNode = Mockito.spy(new DeployAppTraitNode());
        Mockito.doReturn(deployAppService).when(traitNode).getDeployAppService();
        Mockito.doReturn(deployComponentService).when(traitNode).getDeployComponentService();
        Mockito.doReturn(traitFactory).when(traitNode).getTraitFactory();
        Mockito.doReturn(groovyHandlerFactory).when(traitNode).getGroovyHandlerFactory();
        Mockito.doReturn(FixtureUtil.getFixture("test_trait_resource_limit/component_schema.yaml"))
                .when(deployAppService)
                .getAttr(DEPLOY_APP_ID, "componentSchemaMap_K8S_MICROSERVICE|server|3.4.7+20220429032058586762");
        Mockito.doReturn(DefinitionSchemaDO.builder().build())
                .when(definitionSchemaService)
                .get(DefinitionSchemaQueryCondition.builder().name(DEFINITION_REF).build(), DefaultConstant.SYSTEM_OPERATOR);
        Mockito.doReturn(TraitDO.builder()
                        .className("com.alibaba.tesla.appmanager.trait.plugin.ResourceLimitTrait")
                        .definitionRef(DEFINITION_REF)
                        .traitDefinition(FixtureUtil.getFixture("test_trait_resource_limit/trait_definition.yaml"))
                        .build())
                .when(traitService)
                .get(TraitQueryCondition.builder().name("resourceLimit.trait.abm.io").withBlobs(true).build(),
                        DefaultConstant.SYSTEM_OPERATOR);

        String acStr = FixtureUtil.getFixture("test_trait_resource_limit/deploy_app_schema.yaml");
        DeployAppSchema deployAppSchema = SchemaUtil.toSchema(DeployAppSchema.class, acStr);
        JSONObject globalVariable = new JSONObject();
        globalVariable.put(AppFlowVariableKey.DEPLOY_ID, String.valueOf(DEPLOY_APP_ID));
        globalVariable.put(AppFlowVariableKey.CONFIGURATION, acStr);
        JSONObject globalParams = new JSONObject();
        JSONObject overwriteParams = new JSONObject();
        overwriteParams.put("Global", new JSONObject());
        for (DeployAppSchema.ParameterValue parameterValue : deployAppSchema.getSpec().getParameterValues()) {
            overwriteParams.getJSONObject("Global").put(parameterValue.getName(), parameterValue.getValue());
        }
        globalParams.put(AppFlowParamKey.OVERWRITE_PARAMETER_VALUES, overwriteParams);
        traitNode.setGlobalVariable(globalVariable);
        traitNode.setGlobalParams(globalParams);
        traitNode.setDagInstId(1L);
        traitNode.setNodeId("test_node_id");
        traitNode.setFatherNodeId("TRAIT_ADDON|K8S_MICROSERVICE~server~resourceLimit.trait.abm.io|_");
    }

    @Test
    public void testRun() throws Exception {
        traitNode.run();

        // 测试 Global 中的全局环境变量是否已经正确渲染到了 Scope 层面
        ArgumentCaptor<DeployComponentDO> deployComponentArg = ArgumentCaptor.forClass(DeployComponentDO.class);
        Mockito.verify(deployComponentService).create(deployComponentArg.capture(), Mockito.any());
        assertThat(deployComponentArg.getValue().getClusterId()).isEqualTo("oxs-pre-na61");
        assertThat(deployComponentArg.getValue().getNamespaceId()).isEqualTo("abm-pre");
        assertThat(deployComponentArg.getValue().getStageId()).isEqualTo("pre-na61");
    }
}
