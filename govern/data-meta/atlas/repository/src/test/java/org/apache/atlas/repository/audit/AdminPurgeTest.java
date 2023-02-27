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
package org.apache.atlas.repository.audit;

import org.apache.atlas.RequestContext;
import org.apache.atlas.TestModules;
import org.apache.atlas.TestUtilsV2;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.audit.AtlasAuditEntry;
import org.apache.atlas.model.audit.AuditSearchParameters;
import org.apache.atlas.model.instance.*;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.store.bootstrap.AtlasTypeDefStoreInitializer;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStoreV2;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStream;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.utils.TestResourceFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

@Guice(modules = TestModules.TestOnlyModule.class)
public class AdminPurgeTest extends AtlasTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(AdminPurgeTest.class);
    private static final String CLIENT_HOST = "127.0.0.0";
    private static final String DEFAULT_USER = "Admin";
    private static final String AUDIT_PARAMETER_RESOURCE_DIR = "auditSearchParameters";

    @Inject
    AtlasTypeRegistry typeRegistry;

    @Inject
    private AtlasTypeDefStore typeDefStore;

    @Inject
    private AtlasAuditService auditService;

    @Inject
    private AtlasEntityStoreV2 entityStore;

    @BeforeClass
    public void initialize() throws Exception {
        super.initialize();
    }

    @BeforeTest
    public void setupTest() throws IOException, AtlasBaseException {
        RequestContext.clear();
        RequestContext.get().setUser(TestUtilsV2.TEST_USER, null);
        basicSetup(typeDefStore, typeRegistry);
    }

    @AfterClass
    public void clear() throws Exception {
        Thread.sleep(1000);
        AtlasGraphProvider.cleanup();

        super.cleanup();
    }

    @Test
    public void testDeleteEntitiesDoesNotLookupDeletedEntity() throws Exception {
        AtlasTypesDef sampleTypes = TestUtilsV2.defineDeptEmployeeTypes();
        AtlasTypesDef typesToCreate = AtlasTypeDefStoreInitializer.getTypesToCreate(sampleTypes, typeRegistry);

        if (!typesToCreate.isEmpty()) {
            typeDefStore.createTypesDef(typesToCreate);
        }

        AtlasEntity.AtlasEntitiesWithExtInfo  deptEg2 = TestUtilsV2.createDeptEg2();
        AtlasEntityStream entityStream = new AtlasEntityStream(deptEg2);
        EntityMutationResponse emr = entityStore.createOrUpdate(entityStream, false);
        pauseForIndexCreation();

        assertNotNull(emr);
        assertNotNull(emr.getCreatedEntities());
        assertTrue(emr.getCreatedEntities().size() > 0);

        List<String> guids = emr.getCreatedEntities().stream()
                .map(p -> new String(p.getGuid())).collect(Collectors.toList());
        EntityMutationResponse response = entityStore.deleteByIds(guids);
        pauseForIndexCreation();

        List<AtlasEntityHeader> responseDeletedEntities = response.getDeletedEntities();
        assertNotNull(responseDeletedEntities);

        responseDeletedEntities.sort((l,r) -> l.getGuid().compareTo(r.getGuid()));
        List<AtlasEntityHeader> toBeDeletedEntities = emr.getCreatedEntities();
        toBeDeletedEntities.sort((l,r) -> l.getGuid().compareTo(r.getGuid()));
        Assert.assertEquals(responseDeletedEntities.size(), emr.getCreatedEntities().size());
        for(int index = 0 ; index < responseDeletedEntities.size(); index++)
            Assert.assertEquals(responseDeletedEntities.get(index).getGuid(), emr.getCreatedEntities().get(index).getGuid());

        Date startTimestamp = new Date();
        response = entityStore.purgeByIds(new HashSet<>(guids));
        pauseForIndexCreation();

        List<AtlasEntityHeader> responsePurgedEntities = response.getPurgedEntities();
        responsePurgedEntities.sort((l,r) -> l.getGuid().compareTo(r.getGuid()));
        Assert.assertEquals(responsePurgedEntities.size(), responseDeletedEntities.size());
        for(int index = 0 ; index < responsePurgedEntities.size(); index++)
            Assert.assertEquals(responsePurgedEntities.get(index).getGuid(), responseDeletedEntities.get(index).getGuid());

        auditService.add(DEFAULT_USER, AtlasAuditEntry.AuditOperation.PURGE,
                CLIENT_HOST, startTimestamp, new Date(), guids.toString(),
                response.getPurgedEntitiesIds(), response.getPurgedEntities().size());

        AuditSearchParameters auditParameterNull = createAuditParameter("audit-search-parameter-without-filter");
        assertAuditEntry(auditService, auditParameterNull);

        AuditSearchParameters auditSearchParameters = createAuditParameter("audit-search-parameter-purge");
        assertAuditEntry(auditService, auditSearchParameters);
    }

    private AuditSearchParameters createAuditParameter(String fileName) {
        try {
            return TestResourceFileUtils.readObjectFromJson(AUDIT_PARAMETER_RESOURCE_DIR, fileName, AuditSearchParameters.class);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return null;
    }

    private void assertAuditEntry(AtlasAuditService auditService, AuditSearchParameters auditSearchParameters) throws InterruptedException {
        pauseForIndexCreation();
        List<AtlasAuditEntry> result = null;
        try {

            result = auditService.get(auditSearchParameters);
        } catch (Exception e) {
            throw new SkipException("audit entries not retrieved.");
        }

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }
}
