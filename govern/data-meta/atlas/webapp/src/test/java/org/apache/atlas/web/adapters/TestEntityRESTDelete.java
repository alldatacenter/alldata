/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.web.adapters;

import org.apache.atlas.RequestContext;
import org.apache.atlas.TestModules;
import org.apache.atlas.TestUtilsV2;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.instance.EntityMutations;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.DeleteType;
import org.apache.atlas.web.rest.EntityREST;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.FileAssert.fail;

@Guice(modules = {TestModules.TestOnlyModule.class})
public class TestEntityRESTDelete {

    @Inject
    private AtlasTypeDefStore typeStore;

    @Inject
    private EntityREST entityREST;

    private List<AtlasEntity> dbEntities = new ArrayList<>();

    @BeforeClass
    public void setUp() throws Exception {
        AtlasTypesDef typesDef = TestUtilsV2.defineHiveTypes();
        typeStore.createTypesDef(typesDef);
    }

    @AfterMethod
    public void cleanup() {
        RequestContext.clear();
    }

    private void assertSoftDelete(String guid) throws AtlasBaseException {
        AtlasEntity.AtlasEntityWithExtInfo entity = entityREST.getById(guid, false, false);
        assertTrue(entity != null && entity.getEntity().getStatus() == AtlasEntity.Status.DELETED);
    }

    private void assertHardDelete(String guid) {
        try {
            entityREST.getById(guid, false, false);
            fail("Entity should have been deleted. Exception should have been thrown.");
        } catch (AtlasBaseException e) {
            assertTrue(true);
        }
    }

    private void createEntities() throws Exception {
        dbEntities.clear();

        for (int i = 1; i <= 2; i++) {
            AtlasEntity dbEntity = TestUtilsV2.createDBEntity();

            final EntityMutationResponse response = entityREST.createOrUpdate(new AtlasEntity.AtlasEntitiesWithExtInfo(dbEntity));

            assertNotNull(response);
            List<AtlasEntityHeader> entitiesMutated = response.getEntitiesByOperation(EntityMutations.EntityOperation.CREATE);

            assertNotNull(entitiesMutated);
            Assert.assertEquals(entitiesMutated.size(), 1);
            assertNotNull(entitiesMutated.get(0));
            dbEntity.setGuid(entitiesMutated.get(0).getGuid());

            dbEntities.add(dbEntity);
        }
    }

    @Test
    public void deleteByGuidTestSoft() throws Exception {
        RequestContext.get().setDeleteType(DeleteType.SOFT);

        createEntities();

        EntityMutationResponse response = entityREST.deleteByGuid(dbEntities.get(0).getGuid());

        assertNotNull(response);
        assertNotNull(response.getDeletedEntities());
        assertSoftDelete(dbEntities.get(0).getGuid());
    }

    @Test
    public void deleteByGuidTestHard() throws Exception {
        RequestContext.get().setDeleteType(DeleteType.HARD);

        createEntities();
        EntityMutationResponse response = entityREST.deleteByGuid(dbEntities.get(0).getGuid());

        assertNotNull(response);
        assertNotNull(response.getDeletedEntities());
        assertHardDelete(dbEntities.get(0).getGuid());
    }


    @Test
    public void deleteByGuidsSoft() throws Exception {
        RequestContext.get().setDeleteType(DeleteType.SOFT);

        createEntities();
        List<String> guids = new ArrayList<>();
        guids.add(dbEntities.get(0).getGuid());
        guids.add(dbEntities.get(1).getGuid());

        EntityMutationResponse response = entityREST.deleteByGuids(guids);

        assertNotNull(response);
        assertNotNull(response.getDeletedEntities());

        for (String guid : guids) {
            assertSoftDelete(guid);
        }
    }

    @Test
    public void deleteByGuidsHard() throws Exception {
        RequestContext.get().setDeleteType(DeleteType.HARD);

        createEntities();
        List<String> guids = new ArrayList<>();
        guids.add(dbEntities.get(0).getGuid());
        guids.add(dbEntities.get(1).getGuid());
        EntityMutationResponse response = entityREST.deleteByGuids(guids);

        assertNotNull(response);
        assertNotNull(response.getDeletedEntities());

        for (String guid : guids) {
            assertHardDelete(guid);
        }
    }

    @Test
    public void testUpdateGetDeleteEntityByUniqueAttributeSoft() throws Exception {
        RequestContext.get().setDeleteType(DeleteType.SOFT);

        createEntities();
        entityREST.deleteByUniqueAttribute(TestUtilsV2.DATABASE_TYPE, toHttpServletRequest(TestUtilsV2.NAME,
                (String) dbEntities.get(0).getAttribute(TestUtilsV2.NAME)));

        assertSoftDelete(dbEntities.get(0).getGuid());
    }

    @Test
    public void testUpdateGetDeleteEntityByUniqueAttributeHard() throws Exception {
        RequestContext.get().setDeleteType(DeleteType.HARD);

        createEntities();

        entityREST.deleteByUniqueAttribute(TestUtilsV2.DATABASE_TYPE, toHttpServletRequest(TestUtilsV2.NAME,
                (String) dbEntities.get(0).getAttribute(TestUtilsV2.NAME)));

        assertHardDelete(dbEntities.get(0).getGuid());
    }

    private HttpServletRequest toHttpServletRequest(String attrName, String attrValue) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Map<String, String[]> paramsMap = toParametersMap(EntityREST.PREFIX_ATTR + attrName, attrValue);

        Mockito.when(request.getParameterMap()).thenReturn(paramsMap);

        return request;
    }

    private Map<String, String[]> toParametersMap(final String name, final String value) {
        return new HashMap<String, String[]>() {{
            put(name, new String[]{value});
        }};
    }
}