package com.alibaba.tesla.appmanager.server.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.tesla.appmanager.common.util.DeploymentUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 Deployment Util
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class TestUtilDeployment {

    @Test
    public void testChopIntoParts() throws Exception {
        List<String> targets = new ArrayList<>();
        targets.add("11.159.187.45");
        targets.add("11.165.113.131");
        targets.add("11.159.187.42");
        targets.add("11.165.113.130");
        List<List<String>> result = DeploymentUtil.chopIntoParts(targets, 3);
        assertThat(result.size()).isEqualTo(3);
        assertThat(JSON.toJSONString(result.get(0))).isEqualTo("[\"11.159.187.45\",\"11.165.113.131\"]");
        assertThat(JSON.toJSONString(result.get(1))).isEqualTo("[\"11.159.187.42\"]");
        assertThat(JSON.toJSONString(result.get(2))).isEqualTo("[\"11.165.113.130\"]");
    }
}
