/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.dataproxy.config.holder;

import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.config.pojo.MQClusterConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Test for {@link MQClusterConfigHolder}
 */
public class TestMQClusterConfigLoader {

    @Test
    public void testUpdateUrl() {
        Map<String, String> url2token = ConfigManager.getInstance().getMqClusterUrl2Token();
        Assert.assertEquals(url2token.size(), 3);
        Map<String, String> newUrl = new HashMap<>();
        newUrl.put("127.0.0.1:8088", "test");
        ConfigManager.getInstance().getMqClusterHolder().setUrl2token(newUrl);
        url2token = ConfigManager.getInstance().getMqClusterUrl2Token();
        Assert.assertEquals(newUrl, url2token);
    }

    @Test
    public void testCommonConfig() {
        MQClusterConfig config = ConfigManager.getInstance().getMqClusterConfig();
        Assert.assertEquals(config.getAuthType(), "token");
        Assert.assertEquals(config.getThreadNum(), 5);
        Assert.assertTrue(config.getEnableBatch());

        Map<String, String> newConfig = new HashMap<>();
        newConfig.put("thread_num", "10");
        newConfig.put("disk_io_rate_per_sec", "60000");
        ConfigManager.getInstance().getMqClusterConfig().putAll(newConfig);
        config = ConfigManager.getInstance().getMqClusterConfig();
        Assert.assertEquals(config.getAuthType(), "token");
        Assert.assertEquals(config.getThreadNum(), 10);
        Assert.assertEquals(config.getDiskIoRatePerSec(), 60000);
    }

    @Test
    public void testTubeUrl() {
        Map<String, String> url2token = ConfigManager.getInstance().getMqClusterUrl2Token();
        Assert.assertEquals(url2token.size(), 3);
        Assert.assertEquals("", url2token.get("127.0.0.1:8080,127.0.0.1:8088"));
    }

    @Test
    public void testPulsarUrl() {
        Map<String, String> url2token = ConfigManager.getInstance().getMqClusterUrl2Token();
        Assert.assertEquals("pulsartoken1", url2token.get("pulsar1://127.0.0.1:6650,pulsar2://127.0.0.1:6600"));
        Assert.assertEquals("pulsartoken2", url2token.get("pulsar2://127.0.0.1:6680"));
    }

}
