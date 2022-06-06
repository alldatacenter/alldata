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

package org.apache.atlas.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.v1.model.instance.Id;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.web.integration.BaseResourceIT;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

public class QuickStartIT extends BaseResourceIT {

    @BeforeClass
    public void runQuickStart() throws Exception {
        super.setUp();

        QuickStart.runQuickstart(new String[]{}, new String[]{"admin", "admin"});
    }

    @Test
    public void testDBIsAdded() throws Exception {
        Referenceable db = getDB(QuickStart.SALES_DB);
        assertEquals(QuickStart.SALES_DB, db.get("name"));
        assertEquals(QuickStart.SALES_DB_DESCRIPTION, db.get("description"));
    }

    private Referenceable getDB(String dbName) throws AtlasServiceException {
        return atlasClientV1.getEntity(QuickStart.DATABASE_TYPE, "name", dbName);
    }

    @Test
    public void testTablesAreAdded() throws AtlasServiceException {
        Referenceable table = getTable(QuickStart.SALES_FACT_TABLE);
        verifySimpleTableAttributes(table);

        verifyDBIsLinkedToTable(table);

        verifyColumnsAreAddedToTable(table);

        verifyTrait(table);
    }

    private Referenceable getTable(String tableName) throws AtlasServiceException {
        return atlasClientV1.getEntity(QuickStart.TABLE_TYPE, AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, tableName);
    }

    private void verifyTrait(Referenceable table) {
        assertNotNull(table.getTrait(QuickStart.FACT_TRAIT));
    }

    private void verifyColumnsAreAddedToTable(Referenceable table) {
        List<Referenceable> columns = (List<Referenceable>) table.get(QuickStart.COLUMNS_ATTRIBUTE);
        assertEquals(4, columns.size());
        Referenceable column = columns.get(0);
        assertEquals(QuickStart.TIME_ID_COLUMN, column.get("name"));
        assertEquals("int", column.get("dataType"));
    }

    private void verifyDBIsLinkedToTable(Referenceable table) throws AtlasServiceException {
        Referenceable db = getDB(QuickStart.SALES_DB);
        assertEquals(db.getId(), table.get(QuickStart.DB_ATTRIBUTE));
    }

    private void verifySimpleTableAttributes(Referenceable table) {
        assertEquals(QuickStart.SALES_FACT_TABLE, table.get("name"));
        assertEquals(QuickStart.SALES_FACT_TABLE_DESCRIPTION, table.get("description"));
    }

    @Test
    public void testProcessIsAdded() throws AtlasServiceException {
        Referenceable loadProcess = atlasClientV1.getEntity(QuickStart.LOAD_PROCESS_TYPE, AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME,
                QuickStart.LOAD_SALES_DAILY_PROCESS);

        assertEquals(QuickStart.LOAD_SALES_DAILY_PROCESS, loadProcess.get(AtlasClient.NAME));
        assertEquals(QuickStart.LOAD_SALES_DAILY_PROCESS_DESCRIPTION, loadProcess.get("description"));

        List<Id> inputs = (List<Id>)loadProcess.get(QuickStart.INPUTS_ATTRIBUTE);
        List<Id> outputs = (List<Id>)loadProcess.get(QuickStart.OUTPUTS_ATTRIBUTE);
        assertEquals(2, inputs.size());
        String salesFactTableId = getTableId(QuickStart.SALES_FACT_TABLE);
        String timeDimTableId = getTableId(QuickStart.TIME_DIM_TABLE);
        String salesFactDailyMVId = getTableId(QuickStart.SALES_FACT_DAILY_MV_TABLE);

        assertEquals(salesFactTableId, inputs.get(0)._getId());
        assertEquals(timeDimTableId, inputs.get(1)._getId());
        assertEquals(salesFactDailyMVId, outputs.get(0)._getId());
    }

    private String getTableId(String tableName) throws AtlasServiceException {
        return getTable(tableName).getId()._getId();
    }

    @Test
    public void testLineageIsMaintained() throws AtlasServiceException {
        String salesFactTableId = getTableId(QuickStart.SALES_FACT_TABLE);
        String timeDimTableId = getTableId(QuickStart.TIME_DIM_TABLE);
        String salesFactDailyMVId = getTableId(QuickStart.SALES_FACT_DAILY_MV_TABLE);

        ObjectNode inputGraph = atlasClientV1.getInputGraphForEntity(salesFactDailyMVId);
        JsonNode   vertices   = inputGraph.get("values").get("vertices");
        JsonNode   edges      = inputGraph.get("values").get("edges");

        assertTrue(vertices.has(salesFactTableId));
        assertTrue(vertices.has(timeDimTableId));
        assertTrue(vertices.has(salesFactDailyMVId));

        assertTrue(edges.has(salesFactDailyMVId));
        ArrayNode inputs = (ArrayNode)edges.get((edges.get(salesFactDailyMVId)).get(0).asText());
        String i1 = inputs.get(0).asText();
        String i2 = inputs.get(1).asText();
        assertTrue(salesFactTableId.equals(i1) || salesFactTableId.equals(i2));
        assertTrue(timeDimTableId.equals(i1) || timeDimTableId.equals(i2));
    }

    @Test
    public void testViewIsAdded() throws AtlasServiceException {

        Referenceable view = atlasClientV1.getEntity(QuickStart.VIEW_TYPE, AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, QuickStart.PRODUCT_DIM_VIEW);

        assertEquals(QuickStart.PRODUCT_DIM_VIEW, view.get(AtlasClient.NAME));

        Id productDimId = getTable(QuickStart.PRODUCT_DIM_TABLE).getId();
        Id inputTableId = ((List<Id>) view.get(QuickStart.INPUT_TABLES_ATTRIBUTE)).get(0);
        assertEquals(productDimId, inputTableId);
    }
}
