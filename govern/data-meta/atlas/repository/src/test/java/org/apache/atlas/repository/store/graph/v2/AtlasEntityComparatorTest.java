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

import com.google.common.collect.ImmutableList;
import org.apache.atlas.TestModules;
import org.apache.atlas.TestRelationshipUtilsV2;
import org.apache.atlas.TestUtilsV2;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.*;

import static org.apache.atlas.TestUtilsV2.NAME;
import static org.apache.atlas.type.AtlasTypeUtil.getAtlasObjectId;

@Guice(modules = TestModules.TestOnlyModule.class)
public class AtlasEntityComparatorTest extends AtlasEntityTestBase {

    private AtlasEntityComparator comparator;

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();

        AtlasTypesDef[] testTypesDefs = new AtlasTypesDef[] {
                TestUtilsV2.defineHiveTypes(),
                TestRelationshipUtilsV2.getInverseReferenceTestTypes()
        };
        createTypesDef(testTypesDefs);

        comparator = new AtlasEntityComparator(typeRegistry, null, null, false, false);
    }

    @AfterClass
    public void clear() throws Exception {

    }

    @Test
    public void testSameEntities () throws AtlasBaseException {
        AtlasEntity dbEntity = TestUtilsV2.createDBEntity();
        dbEntity.setAttribute("namespace", "db namespace");
        dbEntity.setAttribute("cluster", "Fenton_Cluster");

        AtlasEntity tableEntity = TestUtilsV2.createTableEntity(dbEntity);
        AtlasEntity newTableEntity = TestUtilsV2.createTableEntity(dbEntity, tableEntity.getAttribute("name").toString());

        tableEntity.setClassifications(toAtlasClassifications(new String[]{"test-1"}));
        newTableEntity.setClassifications(toAtlasClassifications(new String[]{"test-1"}));

        tableEntity.setCustomAttributes(new HashMap<String, String> (){{ put("foo","bar");}});
        newTableEntity.setCustomAttributes(new HashMap<String, String> (){{ put("foo","bar");}});

        AtlasEntityComparator.AtlasEntityDiffResult diffResult = comparator.getDiffResult(tableEntity, newTableEntity, true);
        Assert.assertFalse(diffResult.hasDifference());
        Assert.assertFalse(diffResult.hasDifferenceOnlyInCustomAttributes());
        Assert.assertFalse(diffResult.hasDifferenceOnlyInBusinessAttributes());

        diffResult = comparator.getDiffResult(tableEntity, newTableEntity, false);
        Assert.assertFalse(diffResult.hasDifference());
    }

    @Test
    public void testChangedEntities () throws AtlasBaseException {
        AtlasEntity dbEntity = TestUtilsV2.createDBEntity();
        dbEntity.setAttribute("namespace", "db namespace");
        dbEntity.setAttribute("cluster", "Fenton_Cluster");

        AtlasEntity tableEntity = TestUtilsV2.createTableEntity(dbEntity);
        AtlasEntity newTableEntity = TestUtilsV2.createTableEntity(dbEntity);

        tableEntity.setCustomAttributes(new HashMap<String, String> (){{ put("foo","bar-1");}});
        newTableEntity.setCustomAttributes(new HashMap<String, String> (){{ put("foo","bar-2");}});

        AtlasEntityComparator.AtlasEntityDiffResult diffResult = comparator.getDiffResult(newTableEntity, tableEntity, true);
        Assert.assertTrue(diffResult.hasDifference());
        Assert.assertFalse(diffResult.hasDifferenceOnlyInCustomAttributes());
        Assert.assertFalse(diffResult.hasDifferenceOnlyInBusinessAttributes());

        diffResult = comparator.getDiffResult(tableEntity, newTableEntity, false);
        Assert.assertNotNull(diffResult.getDiffEntity());
        Assert.assertFalse(diffResult.hasDifferenceOnlyInCustomAttributes());
        Assert.assertFalse(diffResult.hasDifferenceOnlyInBusinessAttributes());
    }

    @Test
    public void testChangedEntitiesCustomAttribute () throws AtlasBaseException {
        AtlasEntity dbEntity = TestUtilsV2.createDBEntity();
        dbEntity.setAttribute("namespace", "db namespace");
        dbEntity.setAttribute("cluster", "Fenton_Cluster");

        AtlasEntity tableEntity = TestUtilsV2.createTableEntity(dbEntity);
        AtlasEntity newTableEntity = TestUtilsV2.createTableEntity(dbEntity, tableEntity.getAttribute("name").toString());

        tableEntity.setCustomAttributes(new HashMap<String, String> (){{ put("foo","bar-1");}});
        newTableEntity.setCustomAttributes(new HashMap<String, String> (){{ put("foo","bar-2");}});

        AtlasEntityComparator.AtlasEntityDiffResult diffResult = comparator.getDiffResult(newTableEntity, tableEntity, true);
        Assert.assertTrue(diffResult.hasDifference());
        Assert.assertTrue(diffResult.hasDifferenceOnlyInCustomAttributes());
        Assert.assertFalse(diffResult.hasDifferenceOnlyInBusinessAttributes());

        diffResult = comparator.getDiffResult(tableEntity, newTableEntity, false);
        Assert.assertNotNull(diffResult.getDiffEntity());
        Assert.assertTrue(diffResult.hasDifferenceOnlyInCustomAttributes());
        Assert.assertFalse(diffResult.hasDifferenceOnlyInBusinessAttributes());
    }

    @Test
    public void testChangedEntitiesClassification () throws AtlasBaseException {
        AtlasEntity dbEntity = TestUtilsV2.createDBEntity();
        dbEntity.setAttribute("namespace", "db namespace");
        dbEntity.setAttribute("cluster", "Fenton_Cluster");

        AtlasEntity tableEntity = TestUtilsV2.createTableEntity(dbEntity);
        AtlasEntity newTableEntity = TestUtilsV2.createTableEntity(dbEntity, tableEntity.getAttribute("name").toString());

        tableEntity.setClassifications(toAtlasClassifications(new String[]{"test-1"}));
        newTableEntity.setClassifications(toAtlasClassifications(new String[]{"test-2"}));

        AtlasEntityComparator.AtlasEntityDiffResult diffResult = comparator.getDiffResult(newTableEntity, tableEntity, true);
        Assert.assertTrue(diffResult.hasDifference());
        Assert.assertFalse(diffResult.hasDifferenceOnlyInCustomAttributes());
        Assert.assertFalse(diffResult.hasDifferenceOnlyInBusinessAttributes());

        diffResult = comparator.getDiffResult(tableEntity, newTableEntity, false);
        Assert.assertNotNull(diffResult.getDiffEntity());
        Assert.assertFalse(diffResult.hasDifferenceOnlyInCustomAttributes());
        Assert.assertFalse(diffResult.hasDifferenceOnlyInBusinessAttributes());
    }

    @Test
    public void testChangedRelationshipAttributes () throws AtlasBaseException {
        AtlasEntity a1 = new AtlasEntity("A");
        a1.setAttribute(NAME, "a1_name");

        AtlasEntity a2 = new AtlasEntity("A");
        a2.setAttribute(NAME, "a2_name");

        AtlasEntity a3 = new AtlasEntity("A");
        a3.setAttribute(NAME, "a3_name");

        AtlasEntity b1 = new AtlasEntity("B");
        b1.setAttribute(NAME, "b_name");
        b1.setRelationshipAttribute("manyA", ImmutableList.of(getAtlasObjectId(a1), getAtlasObjectId(a2)));

        AtlasEntity b2 = new AtlasEntity("B");
        b2.setAttribute(NAME, "b_name");
        b2.setRelationshipAttribute("manyA", ImmutableList.of(getAtlasObjectId(a1), getAtlasObjectId(a2)));

        AtlasEntity b3 = new AtlasEntity("B");
        b3.setAttribute(NAME, "b_name");
        b3.setRelationshipAttribute("manyA", ImmutableList.of(getAtlasObjectId(a3)));

        AtlasEntity b4 = new AtlasEntity("B");
        b4.setAttribute(NAME, "b_name");

        Assert.assertFalse(comparator.getDiffResult(b1, b2, true).hasDifference());
        Assert.assertTrue(comparator.getDiffResult(b1, b3, true).hasDifference());
        Assert.assertTrue(comparator.getDiffResult(b1, b4, true).hasDifference());
        Assert.assertTrue(comparator.getDiffResult(b3, b4, true).hasDifference());

        Assert.assertFalse(comparator.getDiffResult(b1, b2, false).hasDifference());
        Assert.assertTrue(comparator.getDiffResult(b1, b3, false).hasDifference());
        Assert.assertTrue(comparator.getDiffResult(b1, b4, false).hasDifference());
        Assert.assertTrue(comparator.getDiffResult(b3, b4, false).hasDifference());
    }

    private List<AtlasClassification> toAtlasClassifications(String[] classificationNames) {
        List<AtlasClassification> ret = new ArrayList<>();

        if (classificationNames != null) {
            for (String classificationName : classificationNames) {
                ret.add(new AtlasClassification(classificationName));
            }
        }

        return ret;
    }

}
