package com.alibaba.tesla.appmanager.server.util;

import com.alibaba.tesla.appmanager.common.util.InstanceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class TestUtilInstance {

    @Test
    public void testGenerateAppInstanceId() {
        String appId = "de36d23dbbd140";
        String clusterId = "";
        String namespaceId = "vvp-deploy";
        String stageId = "";
        String appInstanceId = InstanceIdUtil.genAppInstanceId(appId, clusterId, namespaceId, stageId);
        assertThat(appInstanceId).isEqualTo("app-2d794040c72cdb6ac6d517c4231cf784");
    }
}
