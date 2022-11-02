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

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.RequestContext;
import org.apache.atlas.TestModules;
import org.apache.atlas.TestUtilsV2;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.instance.EntityMutations;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasBusinessMetadataDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.web.rest.EntityREST;
import org.apache.atlas.web.rest.TypesREST;
import org.apache.commons.lang.time.DateUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.*;

@Guice(modules = {TestModules.TestOnlyModule.class})
public class TypeDefsRESTTest {
    @Inject
    private TypesREST typesREST;

    @Inject
    EntityREST entityREST;

    @Inject
    private AtlasTypeDefStore typeStore;

    private AtlasEntity dbEntity;

    private String bmWithAllTypes;
    private String bmWithSuperType;
    private String bmWithAllTypesMV;

    @BeforeClass
    public void setUp() throws Exception {
        AtlasTypesDef typesDef = TestUtilsV2.defineHiveTypes();
        typeStore.createTypesDef(typesDef);
        AtlasTypesDef enumDef = TestUtilsV2.defineEnumTypes();
        typeStore.createTypesDef(enumDef);
        AtlasTypesDef metadataDef = TestUtilsV2.defineBusinessMetadataTypes();
        typeStore.createTypesDef(metadataDef);
        bmWithAllTypes = "bmWithAllTypes";
        bmWithSuperType = "bmWithSuperType";
        bmWithAllTypesMV = "bmWithAllTypesMV";
    }

    @AfterMethod
    public void cleanup() throws Exception {
        RequestContext.clear();
    }

    private void createTestEntity() throws AtlasBaseException {
        AtlasEntity dbEntity = TestUtilsV2.createDBEntity();

        final EntityMutationResponse response = entityREST.createOrUpdate(new AtlasEntity.AtlasEntitiesWithExtInfo(dbEntity));

        Assert.assertNotNull(response);
        List<AtlasEntityHeader> entitiesMutated = response.getEntitiesByOperation(EntityMutations.EntityOperation.CREATE);

        Assert.assertNotNull(entitiesMutated);
        Assert.assertEquals(entitiesMutated.size(), 1);
        Assert.assertNotNull(entitiesMutated.get(0));
        dbEntity.setGuid(entitiesMutated.get(0).getGuid());

        this.dbEntity = dbEntity;
    }

    private Map<String, Map<String, Object>> populateSuperBusinessMetadataAttributeMap(Map<String, Map<String, Object>> bmAttrMapReq) {
        if (bmAttrMapReq == null) {
            bmAttrMapReq = new HashMap<>();
        }
        Map<String, Object> attrValueMapReq = new HashMap<>();
        attrValueMapReq.put("attrSuperBoolean", true);
        attrValueMapReq.put("attrSuperString", "pqr");

        bmAttrMapReq.put(bmWithSuperType, attrValueMapReq);
        return bmAttrMapReq;
    }

    private Map<String, Map<String, Object>> populateBusinessMetadataAttributeMap(Map<String, Map<String, Object>> bmAttrMapReq) {
        if (bmAttrMapReq == null) {
            bmAttrMapReq = new HashMap<>();
        }
        Map<String, Object> attrValueMapReq = new HashMap<>();
        attrValueMapReq.put("attr1", true);
        attrValueMapReq.put("attr8", "abc");

        bmAttrMapReq.put(bmWithAllTypes, attrValueMapReq);
        return bmAttrMapReq;
    }

    private Map<String, Map<String, Object>> populateMultivaluedBusinessMetadataAttributeMap(Map<String, Map<String, Object>> bmAttrMapReq) {
        if (bmAttrMapReq == null) {
            bmAttrMapReq = new HashMap<>();
        }
        Map<String, Object> attrValueMapReq = new HashMap<>();

        List<Date> dateList = new ArrayList<>();
        Date date = new Date();
        dateList.add(date);
        dateList.add(DateUtils.addDays(date, 2));
        attrValueMapReq.put("attr19", dateList);

        List<String> enumList = new ArrayList<>();
        enumList.add("ROLE");
        enumList.add("GROUP");
        attrValueMapReq.put("attr20", enumList);

        bmAttrMapReq.put("bmWithAllTypesMV", attrValueMapReq);

        return bmAttrMapReq;
    }

    @Test
    public void testDeleteAtlasBusinessTypeDefs() throws AtlasBaseException {
        createTestEntity();

        Map<String, Map<String, Object>> bmAttrMapReq = populateBusinessMetadataAttributeMap(null);
        entityREST.addOrUpdateBusinessAttributes(dbEntity.getGuid(), false, bmAttrMapReq);

        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = entityREST.getById(dbEntity.getGuid(), false, false);
        AtlasEntity atlasEntity = entityWithExtInfo.getEntity();
        Map<String, Map<String, Object>> bmAttrMapRes = atlasEntity.getBusinessAttributes();

        Assert.assertEquals(bmAttrMapReq, bmAttrMapRes);

        AtlasErrorCode errorCode = null;
        try {
            typesREST.deleteAtlasTypeByName(bmWithAllTypes);
        } catch (AtlasBaseException e) {
            errorCode = e.getAtlasErrorCode();
        }
        Assert.assertEquals(errorCode, AtlasErrorCode.TYPE_HAS_REFERENCES);

        Map<String, Object> objectMap = bmAttrMapReq.get(bmWithAllTypes);
        objectMap.remove("attr1", true);
        entityREST.removeBusinessAttributes(dbEntity.getGuid(), bmWithAllTypes, objectMap);

        errorCode = null;
        try {
            typesREST.deleteAtlasTypeByName(bmWithAllTypes);
        } catch (AtlasBaseException e) {
            errorCode = e.getAtlasErrorCode();
        }
        Assert.assertEquals(errorCode, AtlasErrorCode.TYPE_HAS_REFERENCES);

        objectMap.put("attr1", true);
        entityREST.removeBusinessAttributes(dbEntity.getGuid(), bmWithAllTypes, objectMap);

        typesREST.deleteAtlasTypeByName(bmWithAllTypes);
    }

    @Test
    public void testDeleteAtlasBusinessTypeDefs_2() throws AtlasBaseException {
        createTestEntity();

        Map<String, Map<String, Object>> bmAttrMapReq = populateSuperBusinessMetadataAttributeMap(null);
        entityREST.addOrUpdateBusinessAttributes(dbEntity.getGuid(), false, bmAttrMapReq);

        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = entityREST.getById(dbEntity.getGuid(), false, false);
        AtlasEntity atlasEntity = entityWithExtInfo.getEntity();
        Map<String, Map<String, Object>> bmAttrMapRes = atlasEntity.getBusinessAttributes();

        Assert.assertEquals(bmAttrMapReq, bmAttrMapRes);

        AtlasErrorCode errorCode = null;
        try {
            typesREST.deleteAtlasTypeByName(bmWithSuperType);
        } catch (AtlasBaseException e) {
            errorCode = e.getAtlasErrorCode();
        }
        Assert.assertEquals(errorCode, AtlasErrorCode.TYPE_HAS_REFERENCES);

        Map<String, Object> objectMap = bmAttrMapReq.get(bmWithSuperType);
        objectMap.remove("attrSuperString", "pqr");
        entityREST.removeBusinessAttributes(dbEntity.getGuid(), bmWithSuperType, objectMap);

        errorCode = null;
        try {
            typesREST.deleteAtlasTypeByName(bmWithSuperType);
        } catch (AtlasBaseException e) {
            errorCode = e.getAtlasErrorCode();
        }
        Assert.assertEquals(errorCode, AtlasErrorCode.TYPE_HAS_REFERENCES);

        objectMap.put("attrSuperString", "pqr");
        entityREST.removeBusinessAttributes(dbEntity.getGuid(), bmWithSuperType, objectMap);

        typesREST.deleteAtlasTypeByName(bmWithSuperType);
    }

    @Test
    public void testDeleteAtlasBusinessTypeDefs_3() throws AtlasBaseException {
        createTestEntity();

        Map<String, Map<String, Object>> bmAttrMapReq = populateMultivaluedBusinessMetadataAttributeMap(null);
        entityREST.addOrUpdateBusinessAttributes(dbEntity.getGuid(), false, bmAttrMapReq);

        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = entityREST.getById(dbEntity.getGuid(), false, false);
        AtlasEntity atlasEntity = entityWithExtInfo.getEntity();
        Map<String, Map<String, Object>> bmAttrMapRes = atlasEntity.getBusinessAttributes();

        Assert.assertEquals(bmAttrMapReq, bmAttrMapRes);

        AtlasBaseTypeDef atlasBaseTypeDef = typesREST.getTypeDefByName(bmWithAllTypesMV);
        AtlasBusinessMetadataDef businessMetadataDef = (AtlasBusinessMetadataDef) atlasBaseTypeDef;

        AtlasTypesDef atlasTypesDef = new AtlasTypesDef();
        atlasTypesDef.setBusinessMetadataDefs(Collections.singletonList(businessMetadataDef));

        AtlasErrorCode errorCode = null;
        try {
            typesREST.deleteAtlasTypeDefs(atlasTypesDef);
        } catch (AtlasBaseException e) {
            errorCode = e.getAtlasErrorCode();
        }
        Assert.assertEquals(errorCode, AtlasErrorCode.TYPE_HAS_REFERENCES);

        Map<String, Object> objectMap = bmAttrMapReq.get(bmWithAllTypesMV);

        List<Date> dateList = (List<Date>) objectMap.get("attr19");

        objectMap.remove("attr19", dateList);
        entityREST.removeBusinessAttributes(dbEntity.getGuid(), bmWithAllTypesMV, objectMap);

        errorCode = null;
        try {
            typesREST.deleteAtlasTypeDefs(atlasTypesDef);
        } catch (AtlasBaseException e) {
            errorCode = e.getAtlasErrorCode();
        }
        Assert.assertEquals(errorCode, AtlasErrorCode.TYPE_HAS_REFERENCES);

        AtlasBusinessMetadataDef atlasBusinessMetadataDef = atlasTypesDef.getBusinessMetadataDefs().get(0);
        atlasBusinessMetadataDef.setGuid("");
        atlasTypesDef.setBusinessMetadataDefs(Collections.singletonList(atlasBusinessMetadataDef));

        errorCode = null;
        try {
            typesREST.deleteAtlasTypeDefs(atlasTypesDef);
        } catch (AtlasBaseException e) {
            errorCode = e.getAtlasErrorCode();
        }
        Assert.assertEquals(errorCode, AtlasErrorCode.TYPE_HAS_REFERENCES);

        objectMap.put("attr19", dateList);
        entityREST.removeBusinessAttributes(dbEntity.getGuid(), bmWithAllTypesMV, objectMap);

        typesREST.deleteAtlasTypeDefs(atlasTypesDef);
    }
}
