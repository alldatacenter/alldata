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
package org.apache.atlas.repository.store.graph.v2;

import org.apache.atlas.BasicTestSetup;
import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.EntityCorrelationStore;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.utils.TestResourceFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

@Guice(modules = TestModules.TestOnlyModule.class)
public class EntityCorrelationStoreTest extends BasicTestSetup {
    @Inject
    AtlasGraph graph;

    @BeforeClass
    public void setup() throws Exception {
        super.initialize();

        setupTestData();
    }

    @Test
    public void verify() throws IOException, AtlasBaseException {
        final String nonExistentQName = "db01@cm";
        final String db01QName = "db01x@cm";
        final EntityCorrelationStore entityCorrelationStore = new EntityCorrelationStore();

        String db01 = TestResourceFileUtils.getJson("entities", "db01");

        AtlasEntity.AtlasEntitiesWithExtInfo db = AtlasType.fromJson(db01, AtlasEntity.AtlasEntitiesWithExtInfo.class);
        EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(db), false);

        String dbGuid = response.getFirstEntityCreated().getGuid();
        entityStore.deleteById(dbGuid);

        entityCorrelationStore.add(dbGuid,2L);
        graph.commit();

        String guid = entityCorrelationStore.findCorrelatedGuid(nonExistentQName, 1);
        assertNull(guid);

        String fetchedGuid = entityCorrelationStore.findCorrelatedGuid(db01QName, 1L);
        assertNotNull(fetchedGuid);
        assertEquals(fetchedGuid, dbGuid);

        guid = entityCorrelationStore.findCorrelatedGuid(db01QName, 2L);
        assertNull(guid);
    }
}
