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

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class ImportTransformsTest {
    private final String ATTR_NAME_QUALIFIED_NAME =  "qualifiedName";
    private final String COLUMN_QUALIFIED_NAME_FORMAT = "col%s.TABLE1.default@cl1";

    private final String lowerCaseCL1   = "@cl1";
    private final String lowerCaseCL2   = "@cl2";
    private final String jsonLowerCaseReplace = "{ \"hive_table\": { \"qualifiedName\":[ \"lowercase\", \"replace:@cl1:@cl2\" ] } }";
    private final String jsonReplaceLowerCase = "{ \"Asset\": { \"qualifiedName\":[ \"replace:@cl1:@cl2\" ] }, \"hive_table\": { \"qualifiedName\":[ \"lowercase\", \"replace:@cl1:@cl2\" ] } }";
    private final String jsonReplaceRemoveClassification = "{ \"hive_table\": { \"qualifiedName\":[ \"replace:@%s:@%s\"], \"*\":[ \"removeClassification:%s_to_%s\" ] } }";
    private final String jsonReplaceAndAddAttrValue = "{ \"hive_table\": { \"qualifiedName\":[ \"replace:@%s:@%s\"], \"*\":[ \"add:%s=list:%s\" ] } }";
    private final String jsonSingleClearAttrValue = "{ \"hive_table\": { \"*\":[ \"clearAttrValue:replicatedToCluster\", \"clearAttrValue:replicatedFromCluster\" ] } }";
    private final String jsonMultipleClearAttrValue = "{ \"hive_table\": { \"*\":[ \"clearAttrValue:replicatedToCluster,replicatedFromCluster\" ] } }";
    private final String jsonSetDeleted = "{ \"hive_table\": { \"*\":[ \"setDeleted\" ] } }";
    private final String jsonAddClasification = "{ \"hive_table\": { \"*\":[ \"addClassification:REPLICATED\" ] } }";
    private final String jsonAddClasification2 = "{ \"hive_table\": { \"*\":[ \"addClassification:REPLICATED_2\" ] } }";
    private final String jsonAddClasificationScoped = "{ \"hive_column\": { \"*\":[ \"addClassification:REPLICATED_2:topLevel\" ] } }";

    private ImportTransforms transform;
    private String HIVE_TABLE_ATTR_SYNC_INFO = "hive_table.syncInfo";
    private String HIVE_TABLE_ATTR_REPLICATED_FROM = "replicatedFromCluster";
    private String HIVE_TABLE_ATTR_REPLICATED_TO = "replicatedToCluster";

    @BeforeTest
    public void setup() {
        transform = ImportTransforms.fromJson(jsonLowerCaseReplace);
    }

    @BeforeMethod
    public void setUp() {
    }

    @Test
    public void transformEntityWith2Transforms() throws AtlasBaseException {
        AtlasEntity entity    = getHiveTableAtlasEntity();
        String      attrValue = (String) entity.getAttribute(ATTR_NAME_QUALIFIED_NAME);

        transform.apply(entity);

        assertEquals(entity.getAttribute(ATTR_NAME_QUALIFIED_NAME), applyDefaultTransform(attrValue));
    }

    @Test
    public void transformEntityWithExtInfo() throws AtlasBaseException {
        addColumnTransform(transform);

        AtlasEntityWithExtInfo entityWithExtInfo = getAtlasEntityWithExtInfo();
        AtlasEntity            entity            = entityWithExtInfo.getEntity();
        String                 attrValue         = (String) entity.getAttribute(ATTR_NAME_QUALIFIED_NAME);
        String[]               expectedValues    = getExtEntityExpectedValues(entityWithExtInfo);

        transform.apply(entityWithExtInfo);

        assertEquals(entityWithExtInfo.getEntity().getAttribute(ATTR_NAME_QUALIFIED_NAME), applyDefaultTransform(attrValue));

        for (int i = 0; i < expectedValues.length; i++) {
            assertEquals(entityWithExtInfo.getReferredEntities().get(Integer.toString(i)).getAttribute(ATTR_NAME_QUALIFIED_NAME), expectedValues[i]);
        }
    }

    @Test
    public void transformEntityWithExtInfoNullCheck() throws AtlasBaseException {
        addColumnTransform(transform);

        AtlasEntityWithExtInfo entityWithExtInfo = getAtlasEntityWithExtInfo();

        entityWithExtInfo.setReferredEntities(null);

        AtlasEntityWithExtInfo transformedEntityWithExtInfo = transform.apply(entityWithExtInfo);

        assertNotNull(transformedEntityWithExtInfo);
        assertEquals(entityWithExtInfo.getEntity().getGuid(), transformedEntityWithExtInfo.getEntity().getGuid());
    }

    @Test
    public void transformFromJsonWithMultipleEntries() {
        ImportTransforms t = ImportTransforms.fromJson(jsonReplaceLowerCase);

        assertNotNull(t);
        assertEquals(t.getTransforms().size(), 2);
    }

    @Test
    public void removeClassificationTransform_RemovesSpecifiedClassification() throws AtlasBaseException {
        List<AtlasClassification> classifications = new ArrayList<>();
        classifications.add(new AtlasClassification("cl2_to_cl1"));

        String s = String.format(jsonReplaceRemoveClassification, "cl1", "cl2", "cl2", "cl1");
        ImportTransforms t = ImportTransforms.fromJson(s);

        AtlasEntity entity = getHiveTableAtlasEntity();
        String expected_qualifiedName = entity.getAttribute(ATTR_NAME_QUALIFIED_NAME).toString().replace("@cl1", "@cl2");
        entity.setClassifications(classifications);
        assertEquals(entity.getClassifications().size(), 1);

        t.apply(entity);

        assertEquals(entity.getClassifications().size(), 0);
        assertNotNull(t);
        assertEquals(entity.getAttribute(ATTR_NAME_QUALIFIED_NAME), expected_qualifiedName);
    }

    @Test
    public void add_setsValueOfAttribute() throws AtlasBaseException {
        final String expected_syncInfo = "cl1:import";
        String s = String.format(jsonReplaceAndAddAttrValue, "cl1", "cl2", HIVE_TABLE_ATTR_SYNC_INFO, expected_syncInfo);
        ImportTransforms t = ImportTransforms.fromJson(s);

        AtlasEntity entity = getHiveTableAtlasEntity();
        String expected_qualifiedName = entity.getAttribute(ATTR_NAME_QUALIFIED_NAME).toString().replace("@cl1", "@cl2");

        t.apply(entity);

        assertNotNull(t);
        assertEquals(entity.getAttribute(ATTR_NAME_QUALIFIED_NAME), expected_qualifiedName);
        assertEquals(entity.getAttribute(HIVE_TABLE_ATTR_SYNC_INFO), new ArrayList<String>() {{ add(expected_syncInfo); }});
    }


    @Test
    public void clearAttrValue_removesValueOfAttribute() throws AtlasBaseException {
        AtlasEntity entity = getHiveTableAtlasEntity();
        assertNotNull(entity.getAttribute(HIVE_TABLE_ATTR_REPLICATED_FROM));
        assertNotNull(entity.getAttribute(HIVE_TABLE_ATTR_REPLICATED_TO));

        ImportTransforms t = ImportTransforms.fromJson(jsonSingleClearAttrValue);

        assertTrue(t.getTransforms().size() > 0);

        t.apply(entity);

        assertNotNull(t);
        assertNull(entity.getAttribute(HIVE_TABLE_ATTR_REPLICATED_FROM));
        assertNull(entity.getAttribute(HIVE_TABLE_ATTR_REPLICATED_TO));
    }

    @Test
    public void clearAttrValueForMultipleAttributes_removesValueOfAttribute() throws AtlasBaseException {
        AtlasEntity entity = getHiveTableAtlasEntity();
        ImportTransforms t = ImportTransforms.fromJson(jsonMultipleClearAttrValue);

        assertTrue(t.getTransforms().size() > 0);

        t.apply(entity);

        assertNotNull(t);
        assertNull(entity.getAttribute(HIVE_TABLE_ATTR_REPLICATED_FROM));
        assertNull(entity.getAttribute(HIVE_TABLE_ATTR_REPLICATED_TO));
    }

    @Test
    public void setDeleted_SetsStatusToDeleted() throws AtlasBaseException {
        AtlasEntity entity = getHiveTableAtlasEntity();
        assertEquals(entity.getStatus(),  AtlasEntity.Status.ACTIVE);
        ImportTransforms t = ImportTransforms.fromJson(jsonSetDeleted);

        assertTrue(t.getTransforms().size() > 0);

        t.apply(entity);
        assertNotNull(t);
        assertEquals(entity.getStatus(),  AtlasEntity.Status.DELETED);
    }

    @Test
    public void addClassification_AddsClassificationToEntitiy() throws AtlasBaseException {
        AtlasEntity entity = getHiveTableAtlasEntity();
        int existingClassificationsCount =  entity.getClassifications() != null ? entity.getClassifications().size() : 0;
        ImportTransforms t = ImportTransforms.fromJson(jsonAddClasification);

        assertTrue(t.getTransforms().size() > 0);

        t.apply(entity);

        assertNotNull(t);
        assertEquals(entity.getClassifications().size(), existingClassificationsCount + 1);
        addClassification_ExistingClassificationsAreHandled(entity);
        addClassification_MultipleClassificationsAreAdded(entity);
    }

    @Test
    public void addScopedClassification() throws AtlasBaseException {
        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = getAtlasEntityWithExtInfo();
        AtlasEntity entity = entityWithExtInfo.getReferredEntities().get("2");

        int existingClassificationsCount =  entityWithExtInfo.getEntity().getClassifications() != null ? entity.getClassifications().size() : 0;
        ImportTransforms t = ImportTransforms.fromJson(jsonAddClasificationScoped);

        assertTrue(t.getTransforms().size() > 0);

        ImportTransformer.AddClassification classification = (ImportTransformer.AddClassification) t.getTransforms().get("hive_column").get("*").get(0);
        AtlasObjectId objectId = new AtlasObjectId("hive_column", ATTR_NAME_QUALIFIED_NAME, String.format(COLUMN_QUALIFIED_NAME_FORMAT, 2));
        classification.addFilter(objectId);
        t.apply(entityWithExtInfo);

        assertNotNull(t);

        assertNull(entityWithExtInfo.getEntity().getClassifications());
        assertNull(entityWithExtInfo.getReferredEntities().get("0").getClassifications());
        assertEquals(entityWithExtInfo.getReferredEntities().get("1").getClassifications().size(), existingClassificationsCount + 1);
        assertNull(entityWithExtInfo.getReferredEntities().get("2").getClassifications());
    }

    private void addClassification_ExistingClassificationsAreHandled(AtlasEntity entity) throws AtlasBaseException {
        int existingClassificationsCount =  entity.getClassifications() != null ? entity.getClassifications().size() : 0;
        assertTrue(existingClassificationsCount > 0);
        ImportTransforms.fromJson(jsonAddClasification).apply(entity);

        assertEquals(entity.getClassifications().size(), existingClassificationsCount);
    }

    private void addClassification_MultipleClassificationsAreAdded(AtlasEntity entity) throws AtlasBaseException {
        int existingClassificationsCount =  entity.getClassifications().size();
        ImportTransforms.fromJson(jsonAddClasification2).apply(entity);

        assertEquals(entity.getClassifications().size(), existingClassificationsCount + 1);
    }

    private String[] getExtEntityExpectedValues(AtlasEntityWithExtInfo entityWithExtInfo) {
        String[] ret = new String[entityWithExtInfo.getReferredEntities().size()];

        for (int i = 0; i < ret.length; i++) {
            String attrValue = (String) entityWithExtInfo.getReferredEntities().get(Integer.toString(i)).getAttribute(ATTR_NAME_QUALIFIED_NAME);

            ret[i] = attrValue.replace(lowerCaseCL1, lowerCaseCL2);
        }

        return ret;
    }

    private void addColumnTransform(ImportTransforms transform) throws AtlasBaseException {
        Map<String, List<ImportTransformer>> tr     = new HashMap<>();
        List<ImportTransformer>              trList = new ArrayList<>();

        trList.add(ImportTransformer.getTransformer(String.format("replace:%s:%s", lowerCaseCL1, lowerCaseCL2)));
        tr.put(ATTR_NAME_QUALIFIED_NAME, trList);
        transform.getTransforms().put("hive_column", tr);
    }

    private String applyDefaultTransform(String attrValue) {
        return attrValue.toLowerCase().replace(lowerCaseCL1, lowerCaseCL2);
    }

    private AtlasEntity getHiveTableAtlasEntity() {
        AtlasEntity entity = new AtlasEntity("hive_table");
        entity.setStatus(AtlasEntity.Status.ACTIVE);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ATTR_NAME_QUALIFIED_NAME, "TABLE1.default" + lowerCaseCL1);
        attributes.put("dbname", "someDB");
        attributes.put("name", "somename");
        attributes.put(HIVE_TABLE_ATTR_SYNC_INFO, null);
        attributes.put(HIVE_TABLE_ATTR_REPLICATED_FROM, "cl1");
        attributes.put(HIVE_TABLE_ATTR_REPLICATED_TO, "clx");

        entity.setAttributes(attributes);
        return entity;
    }

    private AtlasEntity getHiveColumnAtlasEntity(int index) {
        AtlasEntity entity = new AtlasEntity("hive_column");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ATTR_NAME_QUALIFIED_NAME, String.format(COLUMN_QUALIFIED_NAME_FORMAT, index));
        attributes.put("name", "col" + index);

        entity.setAttributes(attributes);
        return entity;
    }

    private AtlasEntityWithExtInfo getAtlasEntityWithExtInfo() {
        AtlasEntityWithExtInfo ret = new AtlasEntityWithExtInfo(getHiveTableAtlasEntity());

        Map<String, AtlasEntity> referredEntities = new HashMap<>();
        referredEntities.put("0", getHiveColumnAtlasEntity(1));
        referredEntities.put("1", getHiveColumnAtlasEntity(2));
        referredEntities.put("2", getHiveColumnAtlasEntity(3));

        ret.setReferredEntities(referredEntities);

        return ret;
    }
}
