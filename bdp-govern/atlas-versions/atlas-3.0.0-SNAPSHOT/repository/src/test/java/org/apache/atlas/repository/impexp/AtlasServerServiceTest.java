/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.repository.impexp;

import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasServer;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.atlas.repository.Constants.ATTR_NAME_REFERENCEABLE;
import static org.apache.atlas.utils.TestLoadModelUtils.loadBaseModel;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

@Guice(modules = TestModules.TestOnlyModule.class)
public class AtlasServerServiceTest {
    private final String TOP_LEVEL_ENTITY_NAME  = "db1@cl1";
    private final String SERVER_NAME            = "testCl1";
    private final String TARGET_SERVER_NAME     = "testCl2";
    private final String QUALIFIED_NAME_STOCKS  = "stocks@cl1";
    private final String TYPE_HIVE_DB           = "hive_db";

    @Inject
    private AtlasTypeDefStore typeDefStore;

    @Inject
    private AtlasTypeRegistry typeRegistry;

    @Inject
    private AtlasServerService atlasServerService;
    private String topLevelEntityGuid = "AAA-BBB-CCC";

    @BeforeClass
    public void setup() throws IOException, AtlasBaseException {
        loadBaseModel(typeDefStore, typeRegistry);
    }

    @Test
    public void saveAndRetrieveServerInfo() throws AtlasBaseException {
        AtlasServer expected = getServer(SERVER_NAME + "_1", TOP_LEVEL_ENTITY_NAME, "EXPORT", 0l, TARGET_SERVER_NAME);
        AtlasServer expected2 = getServer(TARGET_SERVER_NAME + "_1", TOP_LEVEL_ENTITY_NAME, "IMPORT", 0L, TARGET_SERVER_NAME);
        AtlasServer expected3 = getServer(TARGET_SERVER_NAME + "_3", TOP_LEVEL_ENTITY_NAME, "IMPORT", 0, TARGET_SERVER_NAME);

        AtlasServer actual = atlasServerService.save(expected);
        AtlasServer actual2 = atlasServerService.save(expected2);
        AtlasServer actual3 = atlasServerService.save(expected3);
        AtlasServer actual2x = atlasServerService.get(expected2);

        assertNotNull(actual.getGuid());
        assertNotNull(actual2.getGuid());
        assertNotEquals(actual.getGuid(), actual2.getGuid());
        assertNotEquals(actual2.getGuid(), actual3.getGuid());

        assertEquals(actual2.getGuid(), actual2x.getGuid());


        assertEquals(actual.getName(), expected.getName());
        assertEquals(actual.getFullName(), expected.getFullName());
    }

    private AtlasServer getServer(String serverName, String topLevelEntity, String operation, long nextModifiedTimestamp, String targetServerName) {
        AtlasServer cluster = new AtlasServer(serverName, serverName);

        Map<String, String> syncMap = new HashMap<>();

        syncMap.put("topLevelEntity", topLevelEntity);
        syncMap.put("operation", operation);
        syncMap.put("nextModifiedTimestamp", Long.toString(nextModifiedTimestamp));
        syncMap.put("targetCluster", targetServerName);

        cluster.setAdditionalInfo(syncMap);

        return cluster;
    }

    @Test
    public void verifyAdditionalInfo() throws AtlasBaseException {
        final long expectedLastModifiedTimestamp = 200L;

        AtlasServer expectedCluster = atlasServerService.getCreateAtlasServer(SERVER_NAME, SERVER_NAME);

        String qualifiedNameAttr = Constants.QUALIFIED_NAME.replace(ATTR_NAME_REFERENCEABLE, "");
        AtlasObjectId objectId = new AtlasObjectId(TYPE_HIVE_DB, qualifiedNameAttr, QUALIFIED_NAME_STOCKS);
        expectedCluster.setAdditionalInfoRepl(topLevelEntityGuid, expectedLastModifiedTimestamp);

        AtlasServer actualCluster = atlasServerService.save(expectedCluster);
        assertEquals(actualCluster.getName(), expectedCluster.getName());

        int actualModifiedTimestamp = (int) actualCluster.getAdditionalInfoRepl(topLevelEntityGuid);

        assertEquals(actualModifiedTimestamp, expectedLastModifiedTimestamp);
    }
}
