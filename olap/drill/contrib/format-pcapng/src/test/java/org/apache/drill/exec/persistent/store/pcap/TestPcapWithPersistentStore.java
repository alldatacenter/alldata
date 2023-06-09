/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.persistent.store.pcap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import org.apache.curator.framework.CuratorFramework;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.TestWithZookeeper;
import org.apache.drill.exec.coord.zk.PathUtils;
import org.apache.drill.exec.coord.zk.ZookeeperClient;
import org.apache.drill.exec.store.dfs.FileSystemConfig;
import org.apache.drill.exec.store.pcap.plugin.PcapFormatConfig;
import org.apache.drill.exec.store.pcap.plugin.PcapngFormatConfig;
import org.apache.drill.exec.store.sys.PersistentStore;
import org.apache.drill.exec.store.sys.PersistentStoreConfig;
import org.apache.drill.exec.store.sys.store.ZookeeperPersistentStore;
import org.apache.drill.exec.store.sys.store.provider.ZookeeperPersistentStoreProvider;
import org.apache.zookeeper.CreateMode;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertTrue;

public class TestPcapWithPersistentStore extends TestWithZookeeper {
    /**
     * DRILL-7828
     * Note: If this test breaks you are probably breaking backward and forward compatibility. Verify with the community
     * that breaking compatibility is acceptable and planned for.
     */
    @Test
    public void pcapPluginBackwardCompatabilityTest() throws Exception {
        final String oldPlugin = "oldFormatPlugin";

        try (CuratorFramework curator = createCurator()) {
            curator.start();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerSubtypes(PcapFormatConfig.class, PcapngFormatConfig.class);
            PersistentStoreConfig<FileSystemConfig> storeConfig =
                    PersistentStoreConfig.newJacksonBuilder(objectMapper, FileSystemConfig.class).name("type").build();


            try (ZookeeperClient zkClient = new ZookeeperClient(curator,
                    PathUtils.join("/", storeConfig.getName()), CreateMode.PERSISTENT)) {
                zkClient.start();
                String oldFormatPlugin = DrillFileUtils.getResourceAsString("/config/oldPcapPlugins.json");
                zkClient.put(oldPlugin, oldFormatPlugin.getBytes(), null);
            }

            try (ZookeeperPersistentStoreProvider provider =
                         new ZookeeperPersistentStoreProvider(zkHelper.getConfig(), curator)) {
                PersistentStore<FileSystemConfig> store = provider.getOrCreateStore(storeConfig);
                assertTrue(store instanceof ZookeeperPersistentStore);

                FileSystemConfig oldPluginConfig = ((ZookeeperPersistentStore<FileSystemConfig>)store).get(oldPlugin, null);
                Map<String, FormatPluginConfig> formats = oldPluginConfig.getFormats();
                Assert.assertEquals(formats.keySet(), ImmutableSet.of("pcap", "pcapng"));
                PcapFormatConfig pcap = (PcapFormatConfig) formats.get("pcap");
                PcapngFormatConfig pcapng = (PcapngFormatConfig) formats.get("pcapng");
                Assert.assertEquals(pcap.getExtensions(), ImmutableList.of("pcap"));
                assertTrue(pcapng.getStat());
            }
        }
    }
}
