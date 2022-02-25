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

package org.apache.atlas.web.integration;

import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.lineage.AtlasLineageInfo;
import org.apache.atlas.v1.model.instance.Id;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.atlas.AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME;

/**
 * Entity Lineage v2 Integration Tests.
 */
public class LineageClientV2IT extends DataSetLineageJerseyResourceIT {
    private static final Logger LOG = LoggerFactory.getLogger(LineageClientV2IT.class);
    private String salesFactTable;
    private String salesMonthlyTable;
    private String salesDBName;

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();

        createTypeDefinitionsV1();
        setupInstances();
    }

    @Test
    public void testGetLineageInfo() throws Exception {
        String tableId = atlasClientV1.getEntity(HIVE_TABLE_TYPE,
                REFERENCEABLE_ATTRIBUTE_NAME, salesMonthlyTable).getId()._getId();

        AtlasLineageInfo inputLineageInfo = atlasClientV2.getLineageInfo(tableId, AtlasLineageInfo.LineageDirection.INPUT, 5);
        Assert.assertNotNull(inputLineageInfo);
        Map<String, AtlasEntityHeader> entities = inputLineageInfo.getGuidEntityMap();
        Assert.assertNotNull(entities);

        Set<AtlasLineageInfo.LineageRelation> relations = inputLineageInfo.getRelations();
        Assert.assertNotNull(relations);

        Assert.assertEquals(entities.size(), 6);
        Assert.assertEquals(relations.size(), 5);
        Assert.assertEquals(inputLineageInfo.getLineageDirection(), AtlasLineageInfo.LineageDirection.INPUT);
        Assert.assertEquals(inputLineageInfo.getLineageDepth(), 5);
        Assert.assertEquals(inputLineageInfo.getBaseEntityGuid(), tableId);
    }

    @Test
    public void testGetLineageInfoByAttribute() throws Exception {
        Map<String, String> attributeMap = new HashMap<>();
        attributeMap.put("qualifiedName", salesMonthlyTable);

        AtlasLineageInfo bothLineageInfo = atlasClientV2.getLineageInfo(HIVE_TABLE_TYPE, attributeMap, AtlasLineageInfo.LineageDirection.BOTH, 5);
        Assert.assertNotNull(bothLineageInfo);
        Map<String, AtlasEntityHeader> entities = bothLineageInfo.getGuidEntityMap();
        Assert.assertNotNull(entities);

        Set<AtlasLineageInfo.LineageRelation> relations = bothLineageInfo.getRelations();
        Assert.assertNotNull(relations);

        Assert.assertEquals(entities.size(), 6);
        Assert.assertEquals(relations.size(), 5);
        Assert.assertEquals(bothLineageInfo.getLineageDirection(), AtlasLineageInfo.LineageDirection.BOTH);
        Assert.assertEquals(bothLineageInfo.getLineageDepth(), 5);
    }

    private void setupInstances() throws Exception {
        salesDBName = "Sales" + randomString();
        Id salesDB = database(salesDBName, "Sales Database", "John ETL", "hdfs://host:8000/apps/warehouse/sales");

        List<Referenceable> salesFactColumns = Arrays.asList(column("time_id", "int", "time id"), column("product_id", "int", "product id"),
                column("customer_id", "int", "customer id"),
                column("sales", "double", "product id"));

        salesFactTable = "sales_fact" + randomString();
        Id salesFact = table(salesFactTable, "sales fact table", salesDB, "Joe", "MANAGED", salesFactColumns);

        List<Referenceable> timeDimColumns = Arrays.asList(column("time_id", "int", "time id"), column("dayOfYear", "int", "day Of Year"),
                column("weekDay", "int", "week Day"));

        Id timeDim =
                table("time_dim" + randomString(), "time dimension table", salesDB, "John Doe", "EXTERNAL",
                        timeDimColumns);

        Id reportingDB =
                database("Reporting" + randomString(), "reporting database", "Jane BI",
                        "hdfs://host:8000/apps/warehouse/reporting");

        Id salesFactDaily =
                table("sales_fact_daily_mv" + randomString(), "sales fact daily materialized view", reportingDB,
                        "Joe BI", "MANAGED", salesFactColumns);

        loadProcess("loadSalesDaily" + randomString(), "John ETL", Arrays.asList(salesFact, timeDim),
                Collections.singletonList(salesFactDaily), "create table as select ", "plan", "id", "graph");

        salesMonthlyTable = "sales_fact_monthly_mv" + randomString();
        Id salesFactMonthly =
                table(salesMonthlyTable, "sales fact monthly materialized view", reportingDB, "Jane BI",
                        "MANAGED", salesFactColumns);

        loadProcess("loadSalesMonthly" + randomString(), "John ETL", Collections.singletonList(salesFactDaily),
                Collections.singletonList(salesFactMonthly), "create table as select ", "plan", "id", "graph");
    }
}
