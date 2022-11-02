/**
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

package org.apache.atlas.storm.hook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.hive.bridge.HiveMetaStoreBridge;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.storm.model.StormDataTypes;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.storm.ILocalCluster;
import org.apache.storm.generated.StormTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
public class StormAtlasHookIT {

    public static final Logger LOG = LoggerFactory.getLogger(StormAtlasHookIT.class);

    private static final String ATLAS_URL = "http://localhost:21000/";
    private static final String TOPOLOGY_NAME = "word-count";

    private ILocalCluster stormCluster;
    private AtlasClient atlasClient;

    @BeforeClass
    public void setUp() throws Exception {
        // start a local storm cluster
        stormCluster = StormTestUtil.createLocalStormCluster();
        LOG.info("Created a storm local cluster");

        Configuration configuration = ApplicationProperties.get();
        if (!AuthenticationUtil.isKerberosAuthenticationEnabled()) {
            atlasClient = new AtlasClient(configuration.getStringArray(HiveMetaStoreBridge.ATLAS_ENDPOINT), new String[]{"admin", "admin"});
        } else {
            atlasClient = new AtlasClient(configuration.getStringArray(HiveMetaStoreBridge.ATLAS_ENDPOINT));
        }
    }


    @AfterClass
    public void tearDown() throws Exception {
        LOG.info("Shutting down storm local cluster");
        stormCluster.shutdown();

        atlasClient = null;
    }

    @Test
    public void testAddEntities() throws Exception {
        StormTopology stormTopology = StormTestUtil.createTestTopology();
        StormTestUtil.submitTopology(stormCluster, TOPOLOGY_NAME, stormTopology);
        LOG.info("Submitted topology {}", TOPOLOGY_NAME);

        // todo: test if topology metadata is registered in atlas
        String guid = getTopologyGUID();
        Assert.assertNotNull(guid);
        LOG.info("GUID is {}", guid);

        Referenceable topologyReferenceable = atlasClient.getEntity(guid);
        Assert.assertNotNull(topologyReferenceable);
    }

    private String getTopologyGUID() throws Exception {
        LOG.debug("Searching for topology {}", TOPOLOGY_NAME);
        String query = String.format("from %s where name = \"%s\"",
                StormDataTypes.STORM_TOPOLOGY.getName(), TOPOLOGY_NAME);

        JsonNode results = atlasClient.search(query, 10, 0);
        JsonNode  row    = results.get(0);

        return row.has("$id$") ? row.get("$id$").get("id").asText() : null;
    }
}
