package com.alibaba.tesla.appmanager.meta.k8smicroservice.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.K8sMicroServiceMetaProvider;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.ContainerTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.RepoTypeEnum;
import com.alibaba.tesla.appmanager.common.service.GitService;
import com.alibaba.tesla.appmanager.common.util.ClassUtil;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.deployconfig.service.DeployConfigService;
import com.alibaba.tesla.appmanager.domain.dto.*;
import com.alibaba.tesla.appmanager.domain.req.K8sMicroServiceMetaUpdateReq;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.api.K8sMicroServiceMetaProviderImpl;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.assembly.K8sMicroServiceMetaDtoConvert;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.condition.K8sMicroserviceMetaQueryCondition;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.domain.K8sMicroServiceMetaDO;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * K8S Microservice 对象转换测试 (dto to ext / options)
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@RunWith(SpringRunner.class)
@Slf4j
public class TestTransformerDtoToExt {

    private static final long ID = 1L;
    private static final String APP_ID = "testapp";
    private static final String NAMESPACE_ID = "default";
    private static final String STAGE_ID = "prod";
    private static final String MICROSERVICE_ID = "server";
    private static final String MICROSERVICE_DESCRIPTION = "description";
    private static final String ARCH = "x86";

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
     * 通用创建 microservice 测试
     */
    @Test
    public void testTransformer() {
        K8sMicroServiceMetaDTO raw = generateMetaDTO(true);
        provider.create(raw);

        // 验证在写入数据 DO 的时候，各参数是否正确
        ArgumentCaptor<K8sMicroServiceMetaDO> arg = ArgumentCaptor.forClass(K8sMicroServiceMetaDO.class);
        Mockito.verify(metaService).create(arg.capture());
        K8sMicroServiceMetaDO meta = arg.getValue();
        log.info("meta: {}", JSONObject.toJSONString(meta));
        assertThat(meta.getAppId()).isEqualTo(APP_ID);
        assertThat(meta.getArch()).isEqualTo(ARCH);
        assertThat(meta.getComponentType()).isEqualTo(ComponentTypeEnum.K8S_MICROSERVICE);
        assertThat(meta.getDescription()).isEqualTo(MICROSERVICE_DESCRIPTION);
        assertThat(meta.getId()).isEqualTo(ID);
        assertThat(meta.getMicroServiceId()).isEqualTo(MICROSERVICE_ID);
        assertThat(meta.getName()).isEqualTo(MICROSERVICE_ID);
        assertThat(meta.getNamespaceId()).isEqualTo(NAMESPACE_ID);
        assertThat(meta.getStageId()).isEqualTo(STAGE_ID);

        // 单独验证生成的 options 是否合法
        JSONObject options = SchemaUtil.toSchema(JSONObject.class, meta.getOptions()).getJSONObject("options");
        assertThat(options.getString("kind")).isEqualTo("AdvancedStatefulSet");
        assertThat(options.getJSONArray("containers").size()).isEqualTo(1);
        assertThat(options.getJSONArray("containers").getJSONObject(0).getString("name")).isEqualTo("server");
        assertThat(options.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getString("branch")).isEqualTo("master");
        assertThat(options.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getBoolean("imagePush")).isTrue();
        assertThat(options.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getString("imagePushRegistry")).isEqualTo("reg.docker.alibaba-inc.com/abm-private-x86");
        assertThat(options.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getBoolean("imagePushUseBranchAsTag")).isTrue();
        assertThat(options.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getString("repo")).isEqualTo("http://test.com/pe3/testapp.git");
        assertThat(options.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getString("dockerfileTemplate")).isEqualTo("Dockerfile");
        assertThat(options.getJSONArray("env").size()).isEqualTo(1);
        assertThat(options.getJSONArray("env").getString(0)).isEqualTo("tmp");
        assertThat(options.getJSONArray("initContainers").size()).isEqualTo(1);
        assertThat(options.getJSONArray("initContainers").getJSONObject(0).getString("name")).isEqualTo("db-migration");
        assertThat(options.getJSONArray("initContainers").getJSONObject(0).getJSONObject("build").getString("branch")).isEqualTo("paas-dev");
        assertThat(options.getJSONArray("initContainers").getJSONObject(0).getJSONObject("build").getJSONObject("args")).containsEntry("TAG", "paas");
        assertThat(options.getJSONArray("initContainers").getJSONObject(0).getJSONObject("build").getBoolean("imagePush")).isTrue();
        assertThat(options.getJSONArray("initContainers").getJSONObject(0).getJSONObject("build").getString("imagePushRegistry")).isEqualTo("reg.docker.alibaba-inc.com/abm-private-x86");
        assertThat(options.getJSONArray("initContainers").getJSONObject(0).getJSONObject("build").getBoolean("imagePushUseBranchAsTag")).isTrue();
        assertThat(options.getJSONArray("initContainers").getJSONObject(0).getJSONObject("build").getJSONObject("dockerfileTemplateArgs")).containsEntry("MIGRATE_IMAGE", "reg.docker.alibaba-inc.com/test/migrate");
        assertThat(options.getJSONArray("initContainers").getJSONObject(0).getJSONObject("build").getString("repo")).isEqualTo("http://test.com/pe3/test.git");
        assertThat(options.getJSONArray("initContainers").getJSONObject(0).getJSONObject("build").getString("dockerfileTemplate")).isEqualTo("Dockerfile_db_migration.tpl");
    }

    /**
     * 弹内更新 microservice 测试
     */
    @Test
    public void testTransformerForInternal() {
        // 准备 DTO 对象
        JSONObject ext = new JSONObject();
        ext.put("imagePush", ImagePushDTO.builder()
                .imagePushRegistry(ImagePushRegistryDTO.builder()
                        .dockerRegistry("reg.docker.alibaba-inc.com")
                        .dockerNamespace("abm-private-x86")
                        .useBranchAsTag(true)
                        .build())
                .build());
        Mockito.doReturn(K8sMicroServiceMetaDO.builder()
                        .microServiceExt(ext.toJSONString())
                        .build())
                .when(metaService)
                .get(K8sMicroserviceMetaQueryCondition.builder()
                        .appId(APP_ID)
                        .arch(ARCH)
                        .microServiceId(MICROSERVICE_ID)
                        .namespaceId(NAMESPACE_ID)
                        .stageId(STAGE_ID)
                        .withBlobs(true)
                        .build());

        // run
        K8sMicroServiceMetaDTO raw = generateMetaDTO(false);
        K8sMicroServiceMetaUpdateReq request = new K8sMicroServiceMetaUpdateReq();
        ClassUtil.copy(raw, request);
        provider.update(request);

        // 验证在写入数据 DO 的时候，各参数是否正确
        ArgumentCaptor<K8sMicroServiceMetaDO> arg = ArgumentCaptor.forClass(K8sMicroServiceMetaDO.class);
        ArgumentCaptor<K8sMicroserviceMetaQueryCondition> argCondition = ArgumentCaptor.forClass(K8sMicroserviceMetaQueryCondition.class);
        Mockito.verify(metaService).update(arg.capture(), argCondition.capture());
        K8sMicroServiceMetaDO meta = arg.getValue();
        K8sMicroserviceMetaQueryCondition condition = argCondition.getValue();

        // check
        assertThat(condition).isEqualTo(K8sMicroserviceMetaQueryCondition.builder()
                .appId(APP_ID)
                .namespaceId(NAMESPACE_ID)
                .stageId(STAGE_ID)
                .microServiceId(MICROSERVICE_ID)
                .arch(DefaultConstant.DEFAULT_ARCH)
                .build());
        JSONObject options = SchemaUtil.toSchema(JSONObject.class, meta.getOptions()).getJSONObject("options");
        log.info("metaDO: {}", JSONObject.toJSONString(meta));
        assertThat(options.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getBoolean("imagePush")).isTrue();
        assertThat(options.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getString("imagePushRegistry")).isEqualTo("reg.docker.alibaba-inc.com/abm-private-x86");
        assertThat(options.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getBoolean("imagePushUseBranchAsTag")).isTrue();
        assertThat(options.getJSONArray("initContainers").getJSONObject(0).getJSONObject("build").getBoolean("imagePush")).isTrue();
        assertThat(options.getJSONArray("initContainers").getJSONObject(0).getJSONObject("build").getString("imagePushRegistry")).isEqualTo("reg.docker.alibaba-inc.com/abm-private-x86");
        assertThat(options.getJSONArray("initContainers").getJSONObject(0).getJSONObject("build").getBoolean("imagePushUseBranchAsTag")).isTrue();
    }

    private K8sMicroServiceMetaDTO generateMetaDTO(boolean hasImagePush) {
        K8sMicroServiceMetaDTO dto = K8sMicroServiceMetaDTO.builder()
                .id(ID)
                .appId(APP_ID)
                .namespaceId(NAMESPACE_ID)
                .stageId(STAGE_ID)
                .microServiceId(MICROSERVICE_ID)
                .name(MICROSERVICE_ID)
                .description(MICROSERVICE_DESCRIPTION)
                .componentType(ComponentTypeEnum.K8S_MICROSERVICE)
                .arch(ARCH)
                .kind("AdvancedStatefulSet")
                .envList(Collections.singletonList(EnvMetaDTO.builder()
                        .comment("comment")
                        .defaultValue("default")
                        .name("tmp")
                        .build()))
                .containerObjectList(Arrays.asList(
                        ContainerObjectDTO.builder()
                                .branch("paas-dev")
                                .buildArgs(Collections.singletonList(ArgMetaDTO.builder()
                                        .name("TAG")
                                        .value("paas")
                                        .build()))
                                .containerType(ContainerTypeEnum.INIT_CONTAINER)
                                .dockerfileTemplate("Dockerfile_db_migration.tpl")
                                .dockerfileTemplateArgs(Collections.singletonList(ArgMetaDTO.builder()
                                        .name("MIGRATE_IMAGE")
                                        .value("reg.docker.alibaba-inc.com/test/migrate")
                                        .build()))
                                .name("db-migration")
                                .ports(new ArrayList<>())
                                .repo("http://test.com/pe3/test.git")
                                .build(),
                        ContainerObjectDTO.builder()
                                .appName("testapp")
                                .branch("master")
                                .containerType(ContainerTypeEnum.CONTAINER)
                                .dockerfileTemplate("Dockerfile")
                                .name("server")
                                .repo("http://test.com/pe3/testapp.git")
                                .repoDomain("http://test.com")
                                .repoGroup("pe3")
                                .repoType(RepoTypeEnum.THIRD_REPO)
                                .build()))
                .build();
        if (hasImagePush) {
            dto.setImagePushObject(ImagePushDTO.builder()
                    .imagePushRegistry(ImagePushRegistryDTO.builder()
                            .dockerRegistry("reg.docker.alibaba-inc.com")
                            .dockerNamespace("abm-private-x86")
                            .useBranchAsTag(true)
                            .build())
                    .build());
        }
        return dto;
    }
}
