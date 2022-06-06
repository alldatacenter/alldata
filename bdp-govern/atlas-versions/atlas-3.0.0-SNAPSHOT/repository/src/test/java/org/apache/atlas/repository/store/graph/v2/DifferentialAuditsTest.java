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

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasException;
import org.apache.atlas.BasicTestSetup;
import org.apache.atlas.RequestContext;
import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.query.DSLQueriesTest;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.utils.TestResourceFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;

import static org.apache.atlas.AtlasConfiguration.STORE_DIFFERENTIAL_AUDITS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

@Guice(modules = TestModules.TestOnlyModule.class)
public class DifferentialAuditsTest extends BasicTestSetup {
    private static final Logger LOG = LoggerFactory.getLogger(DifferentialAuditsTest.class);


    @BeforeClass
    public void setup() throws Exception {
        super.initialize();

        setupTestData();
    }

    @Test
    public void t1() throws IOException, AtlasException, AtlasBaseException {
        String db01 = TestResourceFileUtils.getJson("entities", "db01");
        String tbl01 = TestResourceFileUtils.getJson("entities", "tbl01");
        String tbl012Col = TestResourceFileUtils.getJson("entities", "tbl01-2cols");

        AtlasEntity.AtlasEntitiesWithExtInfo db = AtlasType.fromJson(db01, AtlasEntity.AtlasEntitiesWithExtInfo.class);
        AtlasEntity.AtlasEntitiesWithExtInfo tbl = AtlasType.fromJson(tbl01, AtlasEntity.AtlasEntitiesWithExtInfo.class);
        AtlasEntity.AtlasEntitiesWithExtInfo tbl2Cols = AtlasType.fromJson(tbl012Col, AtlasEntity.AtlasEntitiesWithExtInfo.class);

        ((AtlasEntityStoreV2) entityStore).setStoreDifferentialAudits(true);
        EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(db), false);

        assertNotNull(response);

        response = entityStore.createOrUpdate(new AtlasEntityStream(tbl2Cols), false);
        Collection<AtlasEntity> diffEntities = RequestContext.get().getDifferentialEntities();
        assertNotNull(response);
        assertEquals(diffEntities.size(), 0);

        RequestContext.get().clearCache();
        response = entityStore.createOrUpdate(new AtlasEntityStream(tbl), false);
        assertNotNull(response);
        diffEntities = RequestContext.get().getDifferentialEntities();
        assertEquals(diffEntities.size(), 1);

        RequestContext.get().clearCache();
        response = entityStore.createOrUpdate(new AtlasEntityStream(tbl), false);
        assertNotNull(response);
        diffEntities = RequestContext.get().getDifferentialEntities();
        assertEquals(diffEntities.size(), 0);
    }
}

