package com.alibaba.tesla.appmanager.meta.k8smicroservice.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.K8sMicroServiceMetaProvider;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.ContainerTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.RepoTypeEnum;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.service.GitService;
import com.alibaba.tesla.appmanager.deployconfig.service.DeployConfigService;
import com.alibaba.tesla.appmanager.domain.dto.K8sMicroServiceMetaDTO;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.api.K8sMicroServiceMetaProviderImpl;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.assembly.K8sMicroServiceMetaDtoConvert;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.condition.K8sMicroserviceMetaQueryCondition;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.domain.K8sMicroServiceMetaDO;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * K8S Microservice 对象转换测试 (ext to dto)
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@RunWith(SpringRunner.class)
@Slf4j
public class TestTransformerExtToDto {

    private static final long ID = 1L;
    private static final String APP_ID = "testapp";
    private static final String NAMESPACE_ID = "default";
    private static final String STAGE_ID = "prod";
    private static final String MICROSERVICE_ID = "server";
    private static final String MICROSERVICE_DESCRIPTION = "description";
    private static final String MICROSERVICE_EXT = "{\n" +
            "    \"kind\": \"AdvancedStatefulSet\",\n" +
            "    \"envList\": [\n" +
            "        {\n" +
            "            \"comment\": \"comment\",\n" +
            "            \"defaultValue\": \"default\",\n" +
            "            \"name\": \"tmp\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"containerObjectList\": [\n" +
            "        {\n" +
            "            \"branch\": \"paas-dev\",\n" +
            "            \"buildArgs\": [\n" +
            "                {\n" +
            "                    \"name\": \"TAG\",\n" +
            "                    \"value\": \"paas\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"containerType\": \"INIT_CONTAINER\",\n" +
            "            \"dockerfileTemplate\": \"Dockerfile_db_migration.tpl\",\n" +
            "            \"dockerfileTemplateArgs\": [\n" +
            "                {\n" +
            "                    \"name\": \"MIGRATE_IMAGE\",\n" +
            "                    \"value\": \"reg.docker.alibaba-inc.com/test/migrate\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"name\": \"db-migration\",\n" +
            "            \"ports\": [],\n" +
            "            \"repo\": \"http://test.com/pe3/test.git\"\n" +
            "        },        \n" +
            "        {\n" +
            "            \"appName\": \"testapp\",\n" +
            "            \"branch\": \"master\",\n" +
            "            \"containerType\": \"CONTAINER\",\n" +
            "            \"dockerfileTemplate\": \"Dockerfile\",\n" +
            "            \"name\": \"server\",\n" +
            "            \"repo\": \"http://test.com/pe3/testapp.git\",\n" +
            "            \"repoDomain\": \"http://test.com\",\n" +
            "            \"repoGroup\": \"pe3\",\n" +
            "            \"repoType\": \"THIRD_REPO\"\n" +
            "        }\n" +
            "    ]   \n" +
            "}";
    private static final String MICROSERVICE_OPTIONS = "options:\n" +
            "  kind: AdvancedStatefulSet\n" +
            "  containers:\n" +
            "  - build:\n" +
            "      args: {}\n" +
            "      imagePushRegistry: reg.docker.alibaba-inc.com/sw\n" +
            "      imagePush: true\n" +
            "      dockerfileTemplateArgs: {}\n" +
            "      repo: http://test.com/test.git\n" +
            "      dockerfileTemplate: Dockerfile\n" +
            "      ciAccount: null\n" +
            "      ciToken: null\n" +
            "      branch: master\n" +
            "    name: server\n" +
            "  env:\n" +
            "  - tmp";

    @Mock
    private K8sMicroserviceMetaService metaService;

    @Mock
    private GitService gitService;

    @Mock
    private DeployConfigService deployConfigService;

    private K8sMicroServiceMetaDtoConvert converter;
    private K8sMicroServiceMetaProvider provider;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);

        this.converter = new K8sMicroServiceMetaDtoConvert();
        this.provider = Mockito.spy(new K8sMicroServiceMetaProviderImpl(
                converter, metaService, gitService, deployConfigService));
    }

    /**
     * 普通测试
     */
    @Test
    public void testTransformer() {
        Mockito.doReturn(Pagination.valueOf(Collections.singletonList(K8sMicroServiceMetaDO.builder()
                        .id(ID)
                        .appId(APP_ID)
                        .namespaceId(NAMESPACE_ID)
                        .stageId(STAGE_ID)
                        .microServiceId(MICROSERVICE_ID)
                        .name(MICROSERVICE_ID)
                        .description(MICROSERVICE_DESCRIPTION)
                        .componentType(ComponentTypeEnum.K8S_MICROSERVICE)
                        .microServiceExt(MICROSERVICE_EXT)
                        .options("")
                        .build()), Function.identity()))
                .when(metaService)
                .list(K8sMicroserviceMetaQueryCondition.builder()
                        .id(ID)
                        .withBlobs(true)
                        .build());
        K8sMicroServiceMetaDTO meta = provider.get(ID);
        log.info("meta: \n{}", JSONObject.toJSONString(meta));
        // 和 DO 相同部分
        assertThat(meta.getId()).isEqualTo(ID);
        assertThat(meta.getAppId()).isEqualTo(APP_ID);
        assertThat(meta.getNamespaceId()).isEqualTo(NAMESPACE_ID);
        assertThat(meta.getStageId()).isEqualTo(STAGE_ID);
        assertThat(meta.getMicroServiceId()).isEqualTo(MICROSERVICE_ID);
        assertThat(meta.getName()).isEqualTo(MICROSERVICE_ID);
        assertThat(meta.getDescription()).isEqualTo(MICROSERVICE_DESCRIPTION);
        assertThat(meta.getComponentType()).isEqualTo(ComponentTypeEnum.K8S_MICROSERVICE);
        // 解析部分
        assertThat(meta.getKind()).isEqualTo("AdvancedStatefulSet");

        assertThat(meta.getEnvList().get(0).getName()).isEqualTo("tmp");
        assertThat(meta.getEnvList().get(0).getDefaultValue()).isEqualTo("default");
        assertThat(meta.getEnvList().get(0).getComment()).isEqualTo("comment");

        assertThat(meta.getContainerObjectList().get(0).getContainerType()).isEqualTo(ContainerTypeEnum.INIT_CONTAINER);
        assertThat(meta.getContainerObjectList().get(0).getName()).isEqualTo("db-migration");
        assertThat(meta.getContainerObjectList().get(0).getRepoType()).isNull();
        assertThat(meta.getContainerObjectList().get(0).getRepo()).isEqualTo("http://test.com/pe3/test.git");
        assertThat(meta.getContainerObjectList().get(0).getCiToken()).isNull();
        assertThat(meta.getContainerObjectList().get(0).getCiAccount()).isNull();
        assertThat(meta.getContainerObjectList().get(0).getRepoDomain()).isEqualTo("http://test.com");
        assertThat(meta.getContainerObjectList().get(0).getRepoGroup()).isEqualTo("pe3");
        assertThat(meta.getContainerObjectList().get(0).getRepoPath()).isNullOrEmpty();
        assertThat(meta.getContainerObjectList().get(0).getAppName()).isEqualTo("test");
        assertThat(meta.getContainerObjectList().get(0).getOpenCreateRepo()).isFalse();
        assertThat(meta.getContainerObjectList().get(0).getBranch()).isEqualTo("paas-dev");
        assertThat(meta.getContainerObjectList().get(0).getDockerfileTemplate()).isEqualTo("Dockerfile_db_migration.tpl");
        assertThat(meta.getContainerObjectList().get(0).getDockerfileTemplateArgs().get(0).getName()).isEqualTo("MIGRATE_IMAGE");
        assertThat(meta.getContainerObjectList().get(0).getDockerfileTemplateArgs().get(0).getValue()).isEqualTo("reg.docker.alibaba-inc.com/test/migrate");
        assertThat(meta.getContainerObjectList().get(0).getBuildArgs().get(0).getName()).isEqualTo("TAG");
        assertThat(meta.getContainerObjectList().get(0).getBuildArgs().get(0).getValue()).isEqualTo("paas");
        assertThat(meta.getContainerObjectList().get(0).getPorts().size()).isEqualTo(0);
        assertThat(meta.getContainerObjectList().get(0).getCommand()).isNullOrEmpty();
        assertThat(meta.getContainerObjectList().get(0).getLanguage()).isNullOrEmpty();
        assertThat(meta.getContainerObjectList().get(0).getServiceType()).isNullOrEmpty();

        assertThat(meta.getContainerObjectList().get(1).getContainerType()).isEqualTo(ContainerTypeEnum.CONTAINER);
        assertThat(meta.getContainerObjectList().get(1).getName()).isEqualTo("server");
        assertThat(meta.getContainerObjectList().get(1).getRepoType()).isEqualTo(RepoTypeEnum.THIRD_REPO);
        assertThat(meta.getContainerObjectList().get(1).getRepo()).isEqualTo("http://test.com/pe3/testapp.git");
        assertThat(meta.getContainerObjectList().get(1).getCiToken()).isNull();
        assertThat(meta.getContainerObjectList().get(1).getCiAccount()).isNull();
        assertThat(meta.getContainerObjectList().get(1).getRepoDomain()).isEqualTo("http://test.com");
        assertThat(meta.getContainerObjectList().get(1).getRepoGroup()).isEqualTo("pe3");
        assertThat(meta.getContainerObjectList().get(1).getRepoPath()).isNullOrEmpty();
        assertThat(meta.getContainerObjectList().get(1).getAppName()).isEqualTo("testapp");
        assertThat(meta.getContainerObjectList().get(1).getOpenCreateRepo()).isFalse();
        assertThat(meta.getContainerObjectList().get(1).getBranch()).isEqualTo("master");
        assertThat(meta.getContainerObjectList().get(1).getDockerfileTemplate()).isEqualTo("Dockerfile");
        assertThat(meta.getContainerObjectList().get(1).getDockerfileTemplateArgs().size()).isEqualTo(0);
        assertThat(meta.getContainerObjectList().get(1).getBuildArgs().size()).isEqualTo(0);
        assertThat(meta.getContainerObjectList().get(1).getPorts().size()).isEqualTo(0);
        assertThat(meta.getContainerObjectList().get(1).getCommand()).isNullOrEmpty();
        assertThat(meta.getContainerObjectList().get(1).getLanguage()).isNullOrEmpty();
        assertThat(meta.getContainerObjectList().get(1).getServiceType()).isNullOrEmpty();
    }
}
