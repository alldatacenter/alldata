package com.alibaba.tesla.appmanager.meta.k8smicroservice.util;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(SpringRunner.class)
public class TestK8sMicroServiceUtil {

    private static final String OPTIONS = "" +
            "options:\n" +
            "  containers:\n" +
            "  - build:\n" +
            "      args:\n" +
            "        TAG: tag\n" +
            "        OSSUTIL_URL: url\n" +
            "      dockerfileTemplateArgs:\n" +
            "        SAAS_PYTHON2_IMAGE: image\n" +
            "      repo: http://test.com/test.git\n" +
            "      dockerfileTemplate: Dockerfile.tpl\n" +
            "      branch: master\n" +
            "    name: server\n" +
            "    ports:\n" +
            "    - containerPort: '8000'\n" +
            "  env:\n" +
            "  - ON_PAAS";

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 测试 options 中的 branch / dockerRegistry / dockerNamespace 替换结果
     */
    @Test
    public void testReplaceOptionsBranch() {
        JSONObject res = K8sMicroServiceUtil.replaceOptionsBranch(
                ComponentTypeEnum.K8S_MICROSERVICE, OPTIONS, "newbranch", "docker.test.com", "testgroup");
        log.info("final options: {}", JSONObject.toJSONString(res));
        assertThat(res.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getString("branch")).isEqualTo("newbranch");
        assertThat(res.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getBoolean("imagePush")).isEqualTo(true);
        assertThat(res.getJSONArray("containers").getJSONObject(0).getJSONObject("build").getString("imagePushRegistry")).isEqualTo("docker.test.com/testgroup");
    }
}
