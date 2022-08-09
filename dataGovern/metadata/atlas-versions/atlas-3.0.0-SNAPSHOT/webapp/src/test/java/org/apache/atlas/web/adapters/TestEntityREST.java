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
package org.apache.atlas.web.adapters;

import org.apache.atlas.RequestContext;
import org.apache.atlas.TestModules;
import org.apache.atlas.TestUtilsV2;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasClassification.AtlasClassifications;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.instance.EntityMutations;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.atlas.web.rest.EntityREST;
import org.apache.commons.lang.time.DateUtils;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Guice(modules = {TestModules.TestOnlyModule.class})
public class TestEntityREST {

    @Inject
    private AtlasTypeDefStore typeStore;

    @Inject
    private EntityREST entityREST;

    private AtlasEntity dbEntity;

    private AtlasClassification testClassification;

    private AtlasClassification phiClassification;

    @BeforeClass
    public void setUp() throws Exception {
        AtlasTypesDef typesDef = TestUtilsV2.defineHiveTypes();
        typeStore.createTypesDef(typesDef);
        AtlasTypesDef enumDef =  TestUtilsV2.defineEnumTypes();
        typeStore.createTypesDef(enumDef);
        AtlasTypesDef metadataDef = TestUtilsV2.defineBusinessMetadataTypes();
        typeStore.createTypesDef(metadataDef);
    }

    @AfterClass
    public void tearDown() throws Exception {
//        AtlasGraphProvider.cleanup();
    }

    @AfterMethod
    public void cleanup() throws Exception {
        RequestContext.clear();
    }

    private void createTestEntity() throws Exception {
        AtlasEntity dbEntity = TestUtilsV2.createDBEntity();

        final EntityMutationResponse response = entityREST.createOrUpdate(new AtlasEntitiesWithExtInfo(dbEntity));

        Assert.assertNotNull(response);
        List<AtlasEntityHeader> entitiesMutated = response.getEntitiesByOperation(EntityMutations.EntityOperation.CREATE);

        Assert.assertNotNull(entitiesMutated);
        Assert.assertEquals(entitiesMutated.size(), 1);
        Assert.assertNotNull(entitiesMutated.get(0));
        dbEntity.setGuid(entitiesMutated.get(0).getGuid());

        this.dbEntity = dbEntity;
    }

    @Test
    public void testGetEntityById() throws Exception {
        createTestEntity();
        AtlasEntityWithExtInfo response = entityREST.getById(dbEntity.getGuid(), false, false);

        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getEntity());
        TestEntitiesREST.verifyAttributes(response.getEntity().getAttributes(), dbEntity.getAttributes());
    }

    @Test
    public void testGetEntityHeaderByUniqueAttributes() throws Exception {
        createTestEntity();

        String[] attrVal = {String.valueOf(dbEntity.getAttribute("name"))};

        Map<String, String[]> paramMap = new HashMap<>();
        paramMap.put("attr:name", attrVal);

        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getParameterMap()).thenReturn(paramMap);

        AtlasEntityHeader response = entityREST.getEntityHeaderByUniqueAttributes(dbEntity.getTypeName(), mockRequest);

        Assert.assertNotNull(response);
        Assert.assertEquals(dbEntity.getAttribute("name"), response.getAttribute("name"));
        Assert.assertEquals(dbEntity.getAttribute("description"), response.getAttribute("description"));
    }

    @Test(expectedExceptions = AtlasBaseException.class)
    public void testGetEntityHeaderByUniqueAttributes_2() throws Exception {
        createTestEntity();

        String[] attrVal = {String.valueOf(dbEntity.getAttribute("name") + "_2")};

        Map<String, String[]> paramMap = new HashMap<>();
        paramMap.put("attr:name", attrVal);

        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getParameterMap()).thenReturn(paramMap);

        entityREST.getEntityHeaderByUniqueAttributes(dbEntity.getTypeName(), mockRequest);
    }

    @Test(expectedExceptions = AtlasBaseException.class)
    public void testGetEntityHeaderByUniqueAttributes_3() throws Exception {
        createTestEntity();

        String[] attrVal = {String.valueOf(dbEntity.getAttribute("description"))};

        Map<String, String[]> paramMap = new HashMap<>();
        paramMap.put("attr:description", attrVal);

        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getParameterMap()).thenReturn(paramMap);

        entityREST.getEntityHeaderByUniqueAttributes(dbEntity.getTypeName(), mockRequest);
    }

    @Test(dependsOnMethods = "testGetEntityById")
    public void testAddAndGetClassification() throws Exception {

        List<AtlasClassification> classifications = new ArrayList<>();
        testClassification = new AtlasClassification(TestUtilsV2.CLASSIFICATION, new HashMap<String, Object>() {{ put("tag", "tagName"); }});
        classifications.add(testClassification);
        entityREST.addClassifications(dbEntity.getGuid(), classifications);

        final AtlasClassification.AtlasClassifications retrievedClassifications = entityREST.getClassifications(dbEntity.getGuid());
        Assert.assertNotNull(retrievedClassifications);
        final List<AtlasClassification> retrievedClassificationsList = retrievedClassifications.getList();
        Assert.assertNotNull(retrievedClassificationsList);

        Assert.assertEquals(classifications, retrievedClassificationsList);

        final AtlasClassification retrievedClassification = entityREST.getClassification(dbEntity.getGuid(), TestUtilsV2.CLASSIFICATION);

        Assert.assertNotNull(retrievedClassification);
        Assert.assertEquals(retrievedClassification, testClassification);

        // For ATLAS-3327 to test internal properties are added properly.
        AtlasVertex vertex         = AtlasGraphUtilsV2.findByGuid(dbEntity.getGuid());
        String expectedClsName     = Constants.CLASSIFICATION_NAME_DELIMITER + TestUtilsV2.CLASSIFICATION + Constants.CLASSIFICATION_NAME_DELIMITER ;
        String classificationNames = vertex.getProperty(Constants.CLASSIFICATION_NAMES_KEY, String.class);

        Assert.assertNotNull(classificationNames);
        Assert.assertEquals(classificationNames, expectedClsName);
    }

    @Test(dependsOnMethods = "testGetEntityById")
    public void testAddAndUpdateClassificationWithAttributes() throws Exception {
        phiClassification = new AtlasClassification(TestUtilsV2.PHI, new HashMap<String, Object>() {{
            put("stringAttr", "sample_string");
            put("booleanAttr", true);
            put("integerAttr", 100);
        }});

        testClassification = new AtlasClassification(TestUtilsV2.CLASSIFICATION, new HashMap<String, Object>() {{
            put("tag", "tagName");
        }});

        entityREST.addClassifications(dbEntity.getGuid(), new ArrayList<>(Arrays.asList(phiClassification)));

        final AtlasClassifications retrievedClassifications = entityREST.getClassifications(dbEntity.getGuid());
        Assert.assertNotNull(retrievedClassifications);

        final List<AtlasClassification> retrievedClassificationsList = retrievedClassifications.getList();
        Assert.assertNotNull(retrievedClassificationsList);

        final AtlasClassification retrievedClassification = entityREST.getClassification(dbEntity.getGuid(), TestUtilsV2.PHI);
        Assert.assertNotNull(retrievedClassification);
        Assert.assertEquals(retrievedClassification, phiClassification);

        for (String attrName : retrievedClassification.getAttributes().keySet()) {
            Assert.assertEquals(retrievedClassification.getAttribute(attrName), phiClassification.getAttribute(attrName));
        }

        // For ATLAS-3327 to test internal properties are added properly.
        String expectedClsNames    = Constants.CLASSIFICATION_NAME_DELIMITER + TestUtilsV2.CLASSIFICATION +
                                     Constants.CLASSIFICATION_NAME_DELIMITER + TestUtilsV2.PHI + Constants.CLASSIFICATION_NAME_DELIMITER;
        AtlasVertex vertex         = AtlasGraphUtilsV2.findByGuid(dbEntity.getGuid());
        String classificationNames = vertex.getProperty(Constants.CLASSIFICATION_NAMES_KEY, String.class);

        Assert.assertNotNull(classificationNames);
        Assert.assertEquals(classificationNames, expectedClsNames);

        // update multiple tags attributes
        phiClassification = new AtlasClassification(TestUtilsV2.PHI, new HashMap<String, Object>() {{
            put("stringAttr", "sample_string_v2");
            put("integerAttr", 200);
        }});

        testClassification = new AtlasClassification(TestUtilsV2.CLASSIFICATION, new HashMap<String, Object>() {{
            put("tag", "tagName_updated");
        }});

        entityREST.updateClassifications(dbEntity.getGuid(), new ArrayList<>(Arrays.asList(phiClassification, testClassification)));

        AtlasClassification updatedClassification = entityREST.getClassification(dbEntity.getGuid(), TestUtilsV2.PHI);
        Assert.assertNotNull(updatedClassification);
        Assert.assertEquals(updatedClassification.getAttribute("stringAttr"), "sample_string_v2");
        Assert.assertEquals(updatedClassification.getAttribute("integerAttr"), 200);
        Assert.assertEquals(updatedClassification.getAttribute("booleanAttr"), true);

        updatedClassification = entityREST.getClassification(dbEntity.getGuid(), TestUtilsV2.CLASSIFICATION);
        Assert.assertNotNull(updatedClassification);
        Assert.assertEquals(updatedClassification.getAttribute("tag"), testClassification.getAttribute("tag"));

        vertex = AtlasGraphUtilsV2.findByGuid(dbEntity.getGuid());
        classificationNames = vertex.getProperty(Constants.CLASSIFICATION_NAMES_KEY, String.class);

        Assert.assertNotNull(classificationNames);
        Assert.assertEquals(classificationNames, expectedClsNames);

        deleteClassification(dbEntity.getGuid(), TestUtilsV2.PHI);

        expectedClsNames    = Constants.CLASSIFICATION_NAME_DELIMITER + TestUtilsV2.CLASSIFICATION + Constants.CLASSIFICATION_NAME_DELIMITER;
        vertex              = AtlasGraphUtilsV2.findByGuid(dbEntity.getGuid());
        classificationNames = vertex.getProperty(Constants.CLASSIFICATION_NAMES_KEY, String.class);

        Assert.assertNotNull(classificationNames);
        Assert.assertEquals(classificationNames, expectedClsNames);
    }

    @Test(dependsOnMethods = "testAddAndGetClassification")
    public void testGetEntityWithAssociations() throws Exception {

        AtlasEntityWithExtInfo entity = entityREST.getById(dbEntity.getGuid(), false, false);
        final List<AtlasClassification> retrievedClassifications = entity.getEntity().getClassifications();

        Assert.assertNotNull(retrievedClassifications);
        Assert.assertEquals(new ArrayList<AtlasClassification>() {{ add(testClassification); }}, retrievedClassifications);
    }

    @Test(dependsOnMethods = "testGetEntityWithAssociations")
    public void testDeleteClassification() throws Exception {

        deleteClassification(dbEntity.getGuid(), TestUtilsV2.CLASSIFICATION);
        final AtlasClassification.AtlasClassifications retrievedClassifications = entityREST.getClassifications(dbEntity.getGuid());

        String expectedClsNames    = "";
        AtlasVertex vertex         = AtlasGraphUtilsV2.findByGuid(dbEntity.getGuid());
        String classificationNames = vertex.getProperty(Constants.CLASSIFICATION_NAMES_KEY, String.class);

        Assert.assertNotNull(classificationNames);
        Assert.assertEquals(classificationNames, expectedClsNames);

        Assert.assertNotNull(retrievedClassifications);
        Assert.assertEquals(retrievedClassifications.getList().size(), 0);
    }

    @Test(dependsOnMethods = "testDeleteClassification")
    public void testAddClassificationByUniqueAttribute() throws Exception {
        testClassification = new AtlasClassification(TestUtilsV2.CLASSIFICATION, Collections.singletonMap("tag", "tagName"));

        List<AtlasClassification> classifications = Collections.singletonList(testClassification);

        entityREST.addClassificationsByUniqueAttribute(TestUtilsV2.DATABASE_TYPE, toHttpServletRequest(TestUtilsV2.NAME, (String) dbEntity.getAttribute(TestUtilsV2.NAME)), classifications);

        final AtlasClassification.AtlasClassifications retrievedClassifications = entityREST.getClassifications(dbEntity.getGuid());

        Assert.assertNotNull(retrievedClassifications);

        final List<AtlasClassification> retrievedClassificationsList = retrievedClassifications.getList();

        Assert.assertNotNull(retrievedClassificationsList);

        Assert.assertEquals(classifications, retrievedClassificationsList);

        String expectedClsNames    = Constants.CLASSIFICATION_NAME_DELIMITER + TestUtilsV2.CLASSIFICATION + Constants.CLASSIFICATION_NAME_DELIMITER;
        AtlasVertex vertex         = AtlasGraphUtilsV2.findByGuid(dbEntity.getGuid());
        String classificationNames = vertex.getProperty(Constants.CLASSIFICATION_NAMES_KEY, String.class);

        Assert.assertNotNull(classificationNames);
        Assert.assertEquals(classificationNames, expectedClsNames);

        final AtlasClassification retrievedClassification = entityREST.getClassification(dbEntity.getGuid(), TestUtilsV2.CLASSIFICATION);

        Assert.assertNotNull(retrievedClassification);
        Assert.assertEquals(retrievedClassification, testClassification);
    }

    @Test(dependsOnMethods = "testAddClassificationByUniqueAttribute" )
    public void testUpdateClassificationByUniqueAttribute() throws Exception{
        testClassification = new AtlasClassification(TestUtilsV2.CLASSIFICATION, Collections.singletonMap("tag", "tagName"));
        phiClassification  = new AtlasClassification(TestUtilsV2.PHI, new HashMap<String, Object>() {{
            put("stringAttr", "sample_string");
            put("booleanAttr", true);
            put("integerAttr", 100);
        }});

        entityREST.addClassificationsByUniqueAttribute(TestUtilsV2.DATABASE_TYPE, toHttpServletRequest(TestUtilsV2.NAME, (String) dbEntity.getAttribute(TestUtilsV2.NAME)), Arrays.asList(testClassification, phiClassification));

        final AtlasClassifications retrievedClassifications = entityREST.getClassifications(dbEntity.getGuid());

        Assert.assertNotNull(retrievedClassifications);

        final List<AtlasClassification> retrievedClassificationsList = retrievedClassifications.getList();

        Assert.assertNotNull(retrievedClassificationsList);

        final AtlasClassification retrievedClassification = entityREST.getClassification(dbEntity.getGuid(), TestUtilsV2.PHI);

        Assert.assertNotNull(retrievedClassification);
        Assert.assertEquals(retrievedClassification, phiClassification);

        for (String attrName : retrievedClassification.getAttributes().keySet()) {
            Assert.assertEquals(retrievedClassification.getAttribute(attrName), phiClassification.getAttribute(attrName));
        }

        // update multiple tags attributes
        phiClassification = new AtlasClassification(TestUtilsV2.PHI, new HashMap<String, Object>() {{
            put("stringAttr", "sample_string_v2");
            put("integerAttr", 200);
        }});

        entityREST.updateClassificationsByUniqueAttribute(TestUtilsV2.DATABASE_TYPE, toHttpServletRequest(TestUtilsV2.NAME, (String) dbEntity.getAttribute(TestUtilsV2.NAME)), Collections.singletonList(phiClassification));

        AtlasClassification updatedClassification = entityREST.getClassification(dbEntity.getGuid(), TestUtilsV2.PHI);
        Assert.assertNotNull(updatedClassification);
        Assert.assertEquals(updatedClassification.getAttribute("stringAttr"), "sample_string_v2");
        Assert.assertEquals(updatedClassification.getAttribute("integerAttr"), 200);
        Assert.assertEquals(updatedClassification.getAttribute("booleanAttr"), true);

        updatedClassification = entityREST.getClassification(dbEntity.getGuid(), TestUtilsV2.CLASSIFICATION);
        Assert.assertNotNull(updatedClassification);
        Assert.assertEquals(updatedClassification.getAttribute("tag"), testClassification.getAttribute("tag"));
    }

    @Test(dependsOnMethods = "testUpdateClassificationByUniqueAttribute" )
    public void testDeleteClassificationByUniqueAttribute() throws Exception {
        entityREST.deleteClassificationByUniqueAttribute(TestUtilsV2.DATABASE_TYPE, toHttpServletRequest(TestUtilsV2.NAME, (String) dbEntity.getAttribute(TestUtilsV2.NAME)), TestUtilsV2.CLASSIFICATION);
        entityREST.deleteClassificationByUniqueAttribute(TestUtilsV2.DATABASE_TYPE, toHttpServletRequest(TestUtilsV2.NAME, (String) dbEntity.getAttribute(TestUtilsV2.NAME)), TestUtilsV2.PHI);

        final AtlasClassification.AtlasClassifications retrievedClassifications = entityREST.getClassifications(dbEntity.getGuid());

        Assert.assertNotNull(retrievedClassifications);
        Assert.assertEquals(retrievedClassifications.getList().size(), 0);
    }

    @Test(dependsOnMethods = "testDeleteClassificationByUniqueAttribute")
    public void  testDeleteEntityById() throws Exception {

        EntityMutationResponse response = entityREST.deleteByGuid(dbEntity.getGuid());
        List<AtlasEntityHeader> entitiesMutated = response.getEntitiesByOperation(EntityMutations.EntityOperation.DELETE);
        Assert.assertNotNull(entitiesMutated);
        Assert.assertEquals(entitiesMutated.get(0).getGuid(), dbEntity.getGuid());
    }

    @Test
    public void  testPartialUpdateByUniqueAttribute() throws Exception {
        AtlasEntity            dbEntity = TestUtilsV2.createDBEntity();
        EntityMutationResponse response = entityREST.createOrUpdate(new AtlasEntitiesWithExtInfo(dbEntity));
        String                 dbGuid   = response.getEntitiesByOperation(EntityMutations.EntityOperation.CREATE).get(0).getGuid();

        Assert.assertTrue(AtlasTypeUtil.isAssignedGuid(dbGuid));

        final String prevDBName    = (String) dbEntity.getAttribute(TestUtilsV2.NAME);
        final String updatedDBName = prevDBName + ":updated";
        Map<String, Object> dbAttrs = dbEntity.getAttributes();

        // partial update only db name
        dbEntity = new AtlasEntity(TestUtilsV2.DATABASE_TYPE);
        dbEntity.setGuid(dbGuid);
        dbEntity.setAttribute(TestUtilsV2.NAME, updatedDBName);

        dbAttrs.putAll(dbEntity.getAttributes());

        response = entityREST.partialUpdateEntityByUniqueAttrs(TestUtilsV2.DATABASE_TYPE, toHttpServletRequest(TestUtilsV2.NAME, prevDBName), new AtlasEntityWithExtInfo(dbEntity));

        Assert.assertEquals(response.getEntitiesByOperation(EntityMutations.EntityOperation.PARTIAL_UPDATE).get(0).getGuid(), dbGuid);

        //Get By unique attribute
        AtlasEntityWithExtInfo entity = entityREST.getByUniqueAttributes(TestUtilsV2.DATABASE_TYPE, false, false, toHttpServletRequest(TestUtilsV2.NAME, updatedDBName));
        Assert.assertNotNull(entity);
        Assert.assertNotNull(entity.getEntity().getGuid());
        Assert.assertEquals(entity.getEntity().getGuid(), dbGuid);
        TestEntitiesREST.verifyAttributes(entity.getEntity().getAttributes(), dbAttrs);
    }

    @Test
    public void  testUpdateGetDeleteEntityByUniqueAttribute() throws Exception {
        AtlasEntity            dbEntity = TestUtilsV2.createDBEntity();
        EntityMutationResponse response = entityREST.createOrUpdate(new AtlasEntitiesWithExtInfo(dbEntity));
        String                 dbGuid   = response.getEntitiesByOperation(EntityMutations.EntityOperation.CREATE).get(0).getGuid();

        Assert.assertTrue(AtlasTypeUtil.isAssignedGuid(dbGuid));

        final String prevDBName    = (String) dbEntity.getAttribute(TestUtilsV2.NAME);
        final String updatedDBName = prevDBName + ":updated";

        dbEntity.setAttribute(TestUtilsV2.NAME, updatedDBName);

        response = entityREST.partialUpdateEntityByUniqueAttrs(TestUtilsV2.DATABASE_TYPE, toHttpServletRequest(TestUtilsV2.NAME, prevDBName), new AtlasEntityWithExtInfo(dbEntity));

        Assert.assertEquals(response.getEntitiesByOperation(EntityMutations.EntityOperation.PARTIAL_UPDATE).get(0).getGuid(), dbGuid);

        //Get By unique attribute
        AtlasEntityWithExtInfo entity = entityREST.getByUniqueAttributes(TestUtilsV2.DATABASE_TYPE, false, false, toHttpServletRequest(TestUtilsV2.NAME, updatedDBName));
        Assert.assertNotNull(entity);
        Assert.assertNotNull(entity.getEntity().getGuid());
        Assert.assertEquals(entity.getEntity().getGuid(), dbGuid);
        TestEntitiesREST.verifyAttributes(entity.getEntity().getAttributes(), dbEntity.getAttributes());

        final EntityMutationResponse deleteResponse = entityREST.deleteByUniqueAttribute(TestUtilsV2.DATABASE_TYPE, toHttpServletRequest(TestUtilsV2.NAME, (String) dbEntity.getAttribute(TestUtilsV2.NAME)));

        Assert.assertNotNull(deleteResponse.getEntitiesByOperation(EntityMutations.EntityOperation.DELETE));
        Assert.assertEquals(deleteResponse.getEntitiesByOperation(EntityMutations.EntityOperation.DELETE).size(), 1);
        Assert.assertEquals(deleteResponse.getEntitiesByOperation(EntityMutations.EntityOperation.DELETE).get(0).getGuid(), dbGuid);
    }

    private HttpServletRequest toHttpServletRequest(String attrName, String attrValue) {
        HttpServletRequest    request   = Mockito.mock(HttpServletRequest.class);
        Map<String, String[]> paramsMap = toParametersMap(EntityREST.PREFIX_ATTR + attrName, attrValue);

        Mockito.when(request.getParameterMap()).thenReturn(paramsMap);

        return request;
    }

    private Map<String, String[]> toParametersMap(final String name, final String value) {
        return new HashMap<String, String[]>() {{
            put(name, new String[] { value });
        }};
    }

    private void deleteClassification(String guid, String classificationName) throws AtlasBaseException {
        entityREST.deleteClassification(guid, classificationName, null);
    }

    private Map<String, Map<String, Object>> populateBusinessMetadataAttributeMap(Map<String, Map<String, Object>> bmAttrMapReq) {
        if (bmAttrMapReq == null) {
            bmAttrMapReq = new HashMap<>();
        }
        Map<String, Object> attrValueMapReq = new HashMap<>();
        attrValueMapReq.put("attr1", true);
        attrValueMapReq.put("attr2", Byte.MAX_VALUE);
        attrValueMapReq.put("attr3", Short.MAX_VALUE);
        attrValueMapReq.put("attr4", Integer.MAX_VALUE);
        attrValueMapReq.put("attr5", Long.MAX_VALUE);
        attrValueMapReq.put("attr6", Float.MAX_VALUE);
        attrValueMapReq.put("attr7", Double.MAX_VALUE);
        attrValueMapReq.put("attr8", "value8");
        attrValueMapReq.put("attr9", new Date());
        attrValueMapReq.put("attr10", "USER");

        bmAttrMapReq.put("bmWithAllTypes", attrValueMapReq);
        return bmAttrMapReq;
    }

    private Map<String, Map<String, Object>> populateMultivaluedBusinessMetadataAttributeMap(Map<String, Map<String, Object>> bmAttrMapReq) {
        if (bmAttrMapReq == null) {
            bmAttrMapReq = new HashMap<>();
        }
        Map<String, Object> attrValueMapReq = new HashMap<>();

        List<Boolean> booleanList = new ArrayList<>();
        booleanList.add(false);
        booleanList.add(true);
        attrValueMapReq.put("attr11", booleanList);


        List<Byte> byteList = new ArrayList<>();
        byteList.add(Byte.MIN_VALUE);
        byteList.add(Byte.MAX_VALUE);
        attrValueMapReq.put("attr12", byteList);

        List<Short> shortList = new ArrayList<>();
        shortList.add(Short.MIN_VALUE);
        shortList.add(Short.MAX_VALUE);
        attrValueMapReq.put("attr13", shortList);

        List<Integer> integerList = new ArrayList<>();
        integerList.add(Integer.MIN_VALUE);
        integerList.add(Integer.MAX_VALUE);
        attrValueMapReq.put("attr14", integerList);

        List<Long> longList = new ArrayList<>();
        longList.add(Long.MIN_VALUE);
        longList.add(Long.MAX_VALUE);
        attrValueMapReq.put("attr15", longList);

        List<Float> floatList = new ArrayList<>();
        floatList.add(Float.MIN_VALUE);
        floatList.add(Float.MAX_VALUE);
        attrValueMapReq.put("attr16", floatList);

        List<Double> doubleList = new ArrayList<>();
        doubleList.add(Double.MIN_VALUE);
        doubleList.add(Double.MAX_VALUE);
        attrValueMapReq.put("attr17", doubleList);

        List<String> stringList = new ArrayList<>();
        stringList.add("value-1");
        stringList.add("value-2");
        attrValueMapReq.put("attr18", stringList);

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
    public void testAddOrUpdateBusinessMetadataAttributes_1() throws Exception {
        createTestEntity();
        Map<String, Map<String, Object>> bmAttrMapReq = populateBusinessMetadataAttributeMap(null);
        bmAttrMapReq = populateMultivaluedBusinessMetadataAttributeMap(bmAttrMapReq);
        entityREST.addOrUpdateBusinessAttributes(dbEntity.getGuid(), false, bmAttrMapReq);

        AtlasEntityWithExtInfo entityWithExtInfo = entityREST.getById(dbEntity.getGuid(), false, false);
        AtlasEntity atlasEntity = entityWithExtInfo.getEntity();
        Map<String, Map<String, Object>> bmAttrMapRes = atlasEntity.getBusinessAttributes();

        Assert.assertEquals(bmAttrMapReq, bmAttrMapRes);
    }

    @Test
    public void testAddOrUpdateBusinessMetadataAttributes_2() throws Exception {
        createTestEntity();

        Map<String, Map<String, Object>> bmAttrMapReq = populateBusinessMetadataAttributeMap(null);
        entityREST.addOrUpdateBusinessAttributes(dbEntity.getGuid(), false, bmAttrMapReq);

        bmAttrMapReq = populateMultivaluedBusinessMetadataAttributeMap(null);
        entityREST.addOrUpdateBusinessAttributes(dbEntity.getGuid(), true, bmAttrMapReq);

        AtlasEntityWithExtInfo entityWithExtInfo = entityREST.getById(dbEntity.getGuid(), false, false);
        AtlasEntity atlasEntity = entityWithExtInfo.getEntity();
        Map<String, Map<String, Object>> bmAttrMapRes = atlasEntity.getBusinessAttributes();

        Assert.assertNull(bmAttrMapRes.get("bmWithAllTypes"));
        Assert.assertNotNull(bmAttrMapRes.get("bmWithAllTypesMV"));
    }

    @Test
    public void testAddOrUpdateBusinessMetadataAttributes_3() throws Exception {
        createTestEntity();

        Map<String, Map<String, Object>> bmAttrMapReq = populateBusinessMetadataAttributeMap(null);
        entityREST.addOrUpdateBusinessAttributes(dbEntity.getGuid(), false, bmAttrMapReq);

        AtlasEntityWithExtInfo entityWithExtInfo = entityREST.getById(dbEntity.getGuid(), false, false);
        AtlasEntity atlasEntity = entityWithExtInfo.getEntity();
        Map<String, Map<String, Object>> bmAttrMapRes = atlasEntity.getBusinessAttributes();

        Assert.assertEquals(bmAttrMapReq, bmAttrMapRes);

        Map<String, Object> attrValueMapReq = new HashMap<>();
        bmAttrMapReq = new HashMap<>();
        attrValueMapReq.put("attr8", "value8-updated");
        bmAttrMapReq.put("bmWithAllTypes", attrValueMapReq);
        entityREST.addOrUpdateBusinessAttributes(dbEntity.getGuid(), true, bmAttrMapReq);

        entityWithExtInfo = entityREST.getById(dbEntity.getGuid(), false, false);
        atlasEntity = entityWithExtInfo.getEntity();
        bmAttrMapRes = atlasEntity.getBusinessAttributes();

        Assert.assertNull(bmAttrMapRes.get("bmWithAllTypes").get("attr1"));
        Assert.assertNull(bmAttrMapRes.get("bmWithAllTypes").get("attr2"));
        Assert.assertNull(bmAttrMapRes.get("bmWithAllTypes").get("attr3"));
        Assert.assertNull(bmAttrMapRes.get("bmWithAllTypes").get("attr4"));
        Assert.assertNull(bmAttrMapRes.get("bmWithAllTypes").get("attr5"));
        Assert.assertNull(bmAttrMapRes.get("bmWithAllTypes").get("attr6"));
        Assert.assertNull(bmAttrMapRes.get("bmWithAllTypes").get("attr7"));
        Assert.assertNull(bmAttrMapRes.get("bmWithAllTypes").get("attr9"));
        Assert.assertNull(bmAttrMapRes.get("bmWithAllTypes").get("attr10"));
        Assert.assertEquals(bmAttrMapRes.get("bmWithAllTypes").get("attr8"), "value8-updated");
    }

    @Test
    public void testAddOrUpdateBusinessAttributes_4() throws Exception {
        createTestEntity();

        Map<String, Map<String, Object>> bmAttrMapReq = populateBusinessMetadataAttributeMap(null);
        entityREST.addOrUpdateBusinessAttributes(dbEntity.getGuid(), false, bmAttrMapReq);

        AtlasEntityWithExtInfo entityWithExtInfo = entityREST.getById(dbEntity.getGuid(), false, false);
        AtlasEntity atlasEntity = entityWithExtInfo.getEntity();
        Map<String, Map<String, Object>> bmAttrMapRes = atlasEntity.getBusinessAttributes();

        Assert.assertEquals(bmAttrMapReq, bmAttrMapRes);

        Map<String, Object> attrValueMapReq_2 = new HashMap<>();
        Map<String, Map<String, Object>> bmAttrMapReq_2 = new HashMap<>();
        attrValueMapReq_2.put("attr8", "value8-updated");
        bmAttrMapReq_2.put("bmWithAllTypes", attrValueMapReq_2);
        entityREST.addOrUpdateBusinessAttributes(dbEntity.getGuid(), false, bmAttrMapReq_2);

        entityWithExtInfo = entityREST.getById(dbEntity.getGuid(), false, false);
        atlasEntity = entityWithExtInfo.getEntity();
        Map<String, Map<String, Object>> bmAttrMapRes_2 = atlasEntity.getBusinessAttributes();

        Assert.assertEquals(bmAttrMapRes.get("bmWithAllTypes").get("attr1"), bmAttrMapRes_2.get("bmWithAllTypes").get("attr1"));
        Assert.assertEquals(bmAttrMapRes.get("bmWithAllTypes").get("attr2"), bmAttrMapRes_2.get("bmWithAllTypes").get("attr2"));
        Assert.assertEquals(bmAttrMapRes.get("bmWithAllTypes").get("attr3"), bmAttrMapRes_2.get("bmWithAllTypes").get("attr3"));
        Assert.assertEquals(bmAttrMapRes.get("bmWithAllTypes").get("attr4"), bmAttrMapRes_2.get("bmWithAllTypes").get("attr4"));
        Assert.assertEquals(bmAttrMapRes.get("bmWithAllTypes").get("attr5"), bmAttrMapRes_2.get("bmWithAllTypes").get("attr5"));
        Assert.assertEquals(bmAttrMapRes.get("bmWithAllTypes").get("attr6"), bmAttrMapRes_2.get("bmWithAllTypes").get("attr6"));
        Assert.assertEquals(bmAttrMapRes.get("bmWithAllTypes").get("attr7"), bmAttrMapRes_2.get("bmWithAllTypes").get("attr7"));
        Assert.assertEquals(bmAttrMapRes.get("bmWithAllTypes").get("attr9"), bmAttrMapRes_2.get("bmWithAllTypes").get("attr9"));
        Assert.assertEquals(bmAttrMapRes.get("bmWithAllTypes").get("attr10"), bmAttrMapRes_2.get("bmWithAllTypes").get("attr10"));
        Assert.assertEquals(bmAttrMapRes_2.get("bmWithAllTypes").get("attr8"), "value8-updated");
    }

    @Test(expectedExceptions = AtlasBaseException.class)
    public void testAddOrUpdateBusinessAttributes_5() throws Exception {
        createTestEntity();

        Map<String, Map<String, Object>> bmAttrMapReq = new HashMap<>();
        Map<String, Object> attrValueMapReq = new HashMap<>();
        attrValueMapReq.put("attr2", "value2");
        bmAttrMapReq.put("bmWithAllTypes", attrValueMapReq);

        entityREST.addOrUpdateBusinessAttributes(dbEntity.getGuid(), false, bmAttrMapReq);
    }

    @Test(expectedExceptions = AtlasBaseException.class)
    public void testAddOrUpdateBusinessAttributes_6() throws Exception {
        createTestEntity();

        Map<String, Map<String, Object>> bmAttrMapReq = new HashMap<>();
        Map<String, Object> attrValueMapReq = new HashMap<>();
        attrValueMapReq.put("attr14", 14);
        bmAttrMapReq.put("bmWithAllTypesMV", attrValueMapReq);

        entityREST.addOrUpdateBusinessAttributes(dbEntity.getGuid(), false, bmAttrMapReq);
    }

    @Test(expectedExceptions = AtlasBaseException.class)
    public void testAddOrUpdateBusinessAttributes_7() throws Exception {
        createTestEntity();

        Map<String, Map<String, Object>> bmAttrMapReq = new HashMap<>();
        Map<String, Object> attrValueMapReq = new HashMap<>();
        List<String> stringList = new ArrayList<>();
        stringList.add("value-1");
        stringList.add("value-2");
        attrValueMapReq.put("attr16", stringList);
        bmAttrMapReq.put("bmWithAllTypesMV", attrValueMapReq);

        entityREST.addOrUpdateBusinessAttributes(dbEntity.getGuid(), false, bmAttrMapReq);
    }

    @Test(expectedExceptions = AtlasBaseException.class)
    public void testAddOrUpdateBusinessAttributes_8() throws Exception {
        createTestEntity();

        Map<String, Map<String, Object>> bmAttrMapReq = new HashMap<>();
        Map<String, Object> attrValueMapReq = new HashMap<>();
        attrValueMapReq.put("attr10", "USER123");
        bmAttrMapReq.put("bmWithAllTypes", attrValueMapReq);

        entityREST.addOrUpdateBusinessAttributes(dbEntity.getGuid(), false, bmAttrMapReq);
    }

    @Test(expectedExceptions = AtlasBaseException.class)
    public void testAddOrUpdateBusinessAttributes_9() throws Exception {
        createTestEntity();

        Map<String, Map<String, Object>> bmAttrMapReq = new HashMap<>();
        Map<String, Object> attrValueMapReq = new HashMap<>();
        List<String> stringList = new ArrayList<>();
        stringList.add("USER123");
        stringList.add("ROLE123");
        attrValueMapReq.put("attr20", stringList);
        bmAttrMapReq.put("bmWithAllTypesMV", attrValueMapReq);

        entityREST.addOrUpdateBusinessAttributes(dbEntity.getGuid(), false, bmAttrMapReq);
    }

}
