package com.alibaba.tesla.appmanager.meta.k8smicroservice.api;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.K8sMicroServiceMetaProvider;
import com.alibaba.tesla.appmanager.common.service.GitService;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.deployconfig.service.DeployConfigService;
import com.alibaba.tesla.appmanager.domain.req.K8sMicroServiceMetaUpdateByOptionReq;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.assembly.K8sMicroServiceMetaDtoConvert;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.domain.K8sMicroServiceMetaDO;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.service.K8sMicroserviceMetaService;
import com.alibaba.tesla.appmanager.spring.util.FixtureUtil;
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

/**
 * 通过 Yaml 创建 K8S Microservice 测试
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@RunWith(SpringRunner.class)
@Slf4j
public class TestProviderCreateK8sMicroServiceByYaml {

    private K8sMicroServiceMetaDtoConvert k8sMicroServiceMetaDtoConvert;

    @Mock
    private K8sMicroserviceMetaService k8SMicroserviceMetaService;

    @Mock
    private GitService gitService;

    @Mock
    private DeployConfigService deployConfigService;

    private K8sMicroServiceMetaProvider provider;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);

        k8sMicroServiceMetaDtoConvert = new K8sMicroServiceMetaDtoConvert();
        this.provider = new K8sMicroServiceMetaProviderImpl(
                k8sMicroServiceMetaDtoConvert,
                k8SMicroserviceMetaService,
                gitService, deployConfigService
        );
    }

    /**
     * 测试携带 imagePush 标识情况下的 k8s microservice meta 创建
     */
    @Test
    public void testCreateWithImagePush_1() throws Exception {
        provider.updateByOption(K8sMicroServiceMetaUpdateByOptionReq.builder()
                .appId("flycore-noarch")
                .namespaceId("default")
                .stageId("live")
                .body(SchemaUtil.toSchema(JSONObject.class, FixtureUtil.getFixture("build/create_with_image_push_1.yaml")))
                .build());

        // check
        ArgumentCaptor<K8sMicroServiceMetaDO> arg = ArgumentCaptor.forClass(K8sMicroServiceMetaDO.class);
        Mockito.verify(k8SMicroserviceMetaService).create(arg.capture());
        K8sMicroServiceMetaDO metaDO = arg.getValue();
        log.info("metaDO: {}", JSONObject.toJSONString(metaDO));
        assertThat(metaDO).isNotNull();

        // check ext
        JSONObject ext = JSONObject.parseObject(metaDO.getMicroServiceExt());
        JSONObject imagePushRegistry = ext.getJSONObject("image").getJSONObject("imagePushRegistry");
        assertThat(imagePushRegistry.getString("dockerNamespace")).isEqualTo("abm-private-x86");
        assertThat(imagePushRegistry.getString("dockerRegistry")).isEqualTo("test.com");
        assertThat(imagePushRegistry.getBoolean("useBranchAsTag")).isTrue();

        // check options
        JSONObject options = SchemaUtil.toSchema(JSONObject.class, metaDO.getOptions()).getJSONObject("options");
        assertThat(options.getString("kind")).isEqualTo("Deployment");
        assertThat(options.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getString("imagePushRegistry")).isEqualTo("test.com/abm-private-x86");
        assertThat(options.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getBooleanValue("imagePush")).isTrue();
        assertThat(options.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getBooleanValue("imagePushUseBranchAsTag")).isTrue();
    }

    /**
     * 测试携带 imagePush 标识情况下的 k8s microservice meta 创建
     */
    @Test
    public void testCreateWithImagePush_2() throws Exception {
        provider.updateByOption(K8sMicroServiceMetaUpdateByOptionReq.builder()
                .appId("flycore-noarch")
                .namespaceId("default")
                .stageId("live")
                .body(SchemaUtil.toSchema(JSONObject.class, FixtureUtil.getFixture("build/create_with_image_push_2.yaml")))
                .build());

        // check
        ArgumentCaptor<K8sMicroServiceMetaDO> arg = ArgumentCaptor.forClass(K8sMicroServiceMetaDO.class);
        Mockito.verify(k8SMicroserviceMetaService).create(arg.capture());
        K8sMicroServiceMetaDO metaDO = arg.getValue();
        log.info("metaDO: {}", JSONObject.toJSONString(metaDO));
        assertThat(metaDO).isNotNull();

        // check ext
        JSONObject ext = JSONObject.parseObject(metaDO.getMicroServiceExt());
        JSONObject imagePushRegistry = ext.getJSONObject("image").getJSONObject("imagePushRegistry");
        assertThat(imagePushRegistry.getString("dockerNamespace")).isEqualTo("abm-private-x86");
        assertThat(imagePushRegistry.getString("dockerRegistry")).isEqualTo("test.com");
        assertThat(imagePushRegistry.getBoolean("useBranchAsTag")).isFalse();

        // check options
        JSONObject options = SchemaUtil.toSchema(JSONObject.class, metaDO.getOptions()).getJSONObject("options");
        assertThat(options.getString("kind")).isEqualTo("Deployment");
        assertThat(options.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getString("imagePushRegistry")).isEqualTo("test.com/abm-private-x86");
        assertThat(options.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getBooleanValue("imagePush")).isTrue();
        assertThat(options.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getBooleanValue("imagePushUseBranchAsTag")).isFalse();
    }

    /**
     * 测试携带 imagePush 标识情况下的 k8s microservice meta 创建
     */
    @Test
    public void testCreateWithImagePush_3() throws Exception {
        provider.updateByOption(K8sMicroServiceMetaUpdateByOptionReq.builder()
                .appId("flycore-noarch")
                .namespaceId("default")
                .stageId("live")
                .body(SchemaUtil.toSchema(JSONObject.class, FixtureUtil.getFixture("build/create_with_image_push_3.yaml")))
                .build());

        // check
        ArgumentCaptor<K8sMicroServiceMetaDO> arg = ArgumentCaptor.forClass(K8sMicroServiceMetaDO.class);
        Mockito.verify(k8SMicroserviceMetaService).create(arg.capture());
        K8sMicroServiceMetaDO metaDO = arg.getValue();
        log.info("metaDO: {}", JSONObject.toJSONString(metaDO));
        assertThat(metaDO).isNotNull();

        // check ext
        JSONObject ext = JSONObject.parseObject(metaDO.getMicroServiceExt());
        assertThat(ext.getJSONObject("image")).isNull();

        // check options
        JSONObject options = SchemaUtil.toSchema(JSONObject.class, metaDO.getOptions()).getJSONObject("options");
        assertThat(options.getString("kind")).isEqualTo("Deployment");
        assertThat(options.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getString("imagePushRegistry")).isNull();
        assertThat(options.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getBoolean("imagePush")).isNull();
        assertThat(options.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getBoolean("imagePushUseBranchAsTag")).isNull();
    }
}
