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
import org.apache.inlong.dataproxy.config.pojo.CacheClusterConfig;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Test for {@link MetaConfigHolder}
 */
public class TestMetaConfigHolder {

    @Test
    public void test() {
        ConfigManager.handshakeManagerOk.set(true);
        MetaConfigHolder metaConfigHolder = new MetaConfigHolder();
        boolean result = metaConfigHolder.loadFromFileToHolder();
        Assert.assertTrue(result);
        Assert.assertEquals(metaConfigHolder.getConfigMd5(), "5a3f5939bb7368f493bf41c1d785b8f3");
        Assert.assertEquals("test_group",
                metaConfigHolder.getTopicName("test_group", "stream1"));
        Assert.assertNull(metaConfigHolder.getTopicName("aaa", "stream1"));
        List<CacheClusterConfig> clusterConfigs = metaConfigHolder.forkCachedCLusterConfig();
        Assert.assertEquals(1, clusterConfigs.size());
        Assert.assertEquals("test_tubemq", clusterConfigs.get(0).getClusterName());

    }

}
