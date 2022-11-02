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

import org.apache.atlas.AtlasException;
import org.apache.atlas.RequestContext;
import org.apache.atlas.TestModules;
import org.apache.atlas.TestUtilsV2;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.AtlasStruct;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.instance.EntityMutations.EntityOperation;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.graph.GraphBackedSearchIndexer;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.store.bootstrap.AtlasTypeDefStoreInitializer;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.repository.store.graph.v1.DeleteHandlerDelegate;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasArrayType;
import org.apache.atlas.type.AtlasMapType;
import org.apache.atlas.type.AtlasStructType;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Guice;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.atlas.TestUtilsV2.randomString;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.fail;

@Guice(modules = TestModules.TestOnlyModule.class)
public class AtlasEntityTestBase extends AtlasTestBase {
    @Inject
    AtlasTypeRegistry typeRegistry;

    @Inject
    AtlasTypeDefStore typeDefStore;

    @Inject
    AtlasEntityStore entityStore;

    @Inject
    DeleteHandlerDelegate deleteDelegate;

    @Inject
    private EntityGraphMapper graphMapper;

    @Inject
    protected AtlasGraph graph;

    AtlasEntityChangeNotifier mockChangeNotifier = mock(AtlasEntityChangeNotifier.class);

    @BeforeClass
    public void setUp() throws Exception {
        RequestContext.clear();
        RequestContext.get().setUser(TestUtilsV2.TEST_USER, null);

        super.initialize();

        new GraphBackedSearchIndexer(typeRegistry);
    }

    @AfterClass
    public void clear() throws Exception {
        AtlasGraphProvider.cleanup();

        super.cleanup();
    }

    @BeforeTest
    public void init() throws Exception {
        entityStore = new AtlasEntityStoreV2(graph, deleteDelegate, typeRegistry, mockChangeNotifier, graphMapper);

        RequestContext.clear();
        RequestContext.get().setUser(TestUtilsV2.TEST_USER, null);
    }

    protected AtlasClassificationDef getTagWithName(AtlasTypesDef typesDef, String tagName, String attributeType) {
        AtlasClassificationDef aTag = new AtlasClassificationDef(tagName);
        AtlasAttributeDef attributeDef = new AtlasAttributeDef("testAttribute", attributeType, true,
                AtlasAttributeDef.Cardinality.SINGLE, 0, 1, false, true, false,
                Collections.emptyList());

        aTag.addAttribute(attributeDef);
        typesDef.setClassificationDefs(Arrays.asList(aTag));
        return aTag;
    }

    protected void createTag(String tagName, String attributeType) {
        try {
            AtlasTypesDef typesDef = new AtlasTypesDef();
            getTagWithName(typesDef, tagName, attributeType);
            typeDefStore.createTypesDef(typesDef);
        } catch (AtlasBaseException e) {
            fail("Tag creation should've succeeded");
        }
    }

    protected String randomStrWithReservedChars() {
        return randomString() + "\"${}%";
    }

    protected void validateMutationResponse(EntityMutationResponse response, EntityOperation op, int expectedNumCreated) {
        List<AtlasEntityHeader> entitiesCreated = response.getEntitiesByOperation(op);
        Assert.assertNotNull(entitiesCreated);
        Assert.assertEquals(entitiesCreated.size(), expectedNumCreated);
    }

    protected void validateEntity(AtlasEntityExtInfo entityExtInfo, AtlasEntity actual) throws AtlasBaseException, AtlasException {
        validateEntity(entityExtInfo, actual, entityExtInfo.getEntity(actual.getGuid()));
    }

    protected void validateEntity(AtlasEntityExtInfo entityExtInfo, AtlasStruct actual, AtlasStruct expected) throws AtlasBaseException, AtlasException {
        if (expected == null) {
            Assert.assertNull(actual, "expected null instance. Found " + actual);

            return;
        }

        Assert.assertNotNull(actual, "found null instance");

        AtlasStructType entityType = (AtlasStructType) typeRegistry.getType(actual.getTypeName());
        for (String attrName : expected.getAttributes().keySet()) {
            Object expectedVal = expected.getAttribute(attrName);
            Object actualVal   = actual.getAttribute(attrName);

            AtlasType attrType = entityType.getAttributeType(attrName);
            validateAttribute(entityExtInfo, actualVal, expectedVal, attrType, attrName);
        }
    }

    protected void validateAttribute(AtlasEntityExtInfo entityExtInfo, Object actual, Object expected, AtlasType attributeType, String attrName) throws AtlasBaseException, AtlasException {
        switch(attributeType.getTypeCategory()) {
            case OBJECT_ID_TYPE:
                Assert.assertTrue(actual instanceof AtlasObjectId);
                String guid = ((AtlasObjectId) actual).getGuid();
                Assert.assertTrue(AtlasTypeUtil.isAssignedGuid(guid), "expected assigned guid. found " + guid);
                break;

            case PRIMITIVE:
            case ENUM:
                Assert.assertEquals(actual, expected);
                break;

            case MAP:
                AtlasMapType mapType     = (AtlasMapType) attributeType;
                AtlasType    valueType   = mapType.getValueType();
                Map          actualMap   = (Map) actual;
                Map          expectedMap = (Map) expected;

                if (MapUtils.isNotEmpty(expectedMap)) {
                    Assert.assertTrue(MapUtils.isNotEmpty(actualMap));

                    // deleted entries are included in the attribute; hence use >=
                    Assert.assertTrue(actualMap.size() >= expectedMap.size());

                    for (Object key : expectedMap.keySet()) {
                        validateAttribute(entityExtInfo, actualMap.get(key), expectedMap.get(key), valueType, attrName);
                    }
                }
                break;

            case ARRAY:
                AtlasArrayType arrType      = (AtlasArrayType) attributeType;
                AtlasType      elemType     = arrType.getElementType();
                List           actualList   = (List) actual;
                List           expectedList = (List) expected;

                if (CollectionUtils.isNotEmpty(expectedList)) {
                    Assert.assertTrue(CollectionUtils.isNotEmpty(actualList));

                    //actual list could have deleted entities. Hence size may not match.
                    Assert.assertTrue(actualList.size() >= expectedList.size());

                    for (int i = 0; i < expectedList.size(); i++) {
                        Assert.assertTrue(actualList.contains(expectedList.get(i)));
                    }
                }
                break;
            case STRUCT:
                AtlasStruct expectedStruct = (AtlasStruct) expected;
                AtlasStruct actualStruct   = (AtlasStruct) actual;

                validateEntity(entityExtInfo, actualStruct, expectedStruct);
                break;
            default:
                Assert.fail("Unknown type category");
        }
    }

    protected AtlasEntity getEntityFromStore(AtlasEntityHeader header) throws AtlasBaseException {
        return header != null ? getEntityFromStore(header.getGuid()) : null;
    }

    protected AtlasEntity getEntityFromStore(String guid) throws AtlasBaseException {
        AtlasEntityWithExtInfo entity = guid != null ? entityStore.getById(guid) : null;

        return entity != null ? entity.getEntity() : null;
    }

    protected void createTypesDef(AtlasTypesDef[] testTypesDefs) throws AtlasBaseException {
        for (AtlasTypesDef typesDef : testTypesDefs) {
            AtlasTypesDef typesToCreate = AtlasTypeDefStoreInitializer.getTypesToCreate(typesDef, typeRegistry);

            if (!typesToCreate.isEmpty()) {
                typeDefStore.createTypesDef(typesToCreate);
            }
        }
    }
}
