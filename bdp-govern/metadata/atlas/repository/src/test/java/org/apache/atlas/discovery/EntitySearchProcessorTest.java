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
package org.apache.atlas.discovery;

import com.google.common.collect.Sets;
import org.apache.atlas.BasicTestSetup;
import org.apache.atlas.SortOrder;
import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.discovery.SearchParameters;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.graph.GraphBackedSearchIndexer;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.v2.EntityGraphRetriever;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Guice(modules = TestModules.TestOnlyModule.class)
public class EntitySearchProcessorTest extends BasicTestSetup {
    private static final Logger LOG              = LoggerFactory.getLogger(EntitySearchProcessorTest.class);
    private final SimpleDateFormat formattedDate = new SimpleDateFormat("dd-MMM-yyyy");

    @Inject
    private AtlasGraph graph;

    @Inject
    private AtlasTypeRegistry typeRegistry;

    @Inject
    private EntityGraphRetriever entityRetriever;

    @BeforeClass
    public void setup() throws Exception {
        super.initialize();

        setupTestData();
    }

    @Inject
    public GraphBackedSearchIndexer indexer;
    private String expectedEntityName = "hive_Table_Null_tableType";

    @Test
    public void searchTablesByClassification() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName("hive_column");
        params.setClassification("PII");
        params.setLimit(10);

        SearchContext context = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());

        EntitySearchProcessor processor = new EntitySearchProcessor(context);

        assertEquals(processor.getResultCount(), 4);
        assertEquals(processor.execute().size(), 4);
    }

    @Test
    public void searchByClassificationSortBy() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName("hive_table");
        params.setClassification("Metric");
        params.setLimit(10);
        params.setSortBy("createTime");
        params.setSortOrder(SortOrder.ASCENDING);

        SearchContext context = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());
        EntitySearchProcessor processor = new EntitySearchProcessor(context);

        List<AtlasVertex> vertices = processor.execute();

        assertEquals(processor.getResultCount(), 4);
        assertEquals(vertices.size(), 4);


        AtlasVertex firstVertex = vertices.get(0);

        Date firstDate = (Date) entityRetriever.toAtlasEntityHeader(firstVertex, Sets.newHashSet(params.getSortBy())).getAttribute(params.getSortBy());

        AtlasVertex secondVertex = vertices.get(1);
        Date secondDate = (Date) entityRetriever.toAtlasEntityHeader(secondVertex, Sets.newHashSet(params.getSortBy())).getAttribute(params.getSortBy());

        assertTrue(firstDate.before(secondDate));
    }

    @Test
    public void emptySearchByClassification() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName("hive_table");
        params.setClassification("PII");
        params.setLimit(10);

        SearchContext context = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());

        EntitySearchProcessor processor = new EntitySearchProcessor(context);

        assertEquals(processor.getResultCount(), 0);
        assertEquals(processor.execute().size(), 0);
    }

    @Test(expectedExceptions = AtlasBaseException.class, expectedExceptionsMessageRegExp = "NotExisting: Unknown/invalid classification")
    public void searchByNonExistingClassification() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName("hive_process");
        params.setClassification("NotExisting");
        params.setLimit(10);

        SearchContext context = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());
        new EntitySearchProcessor(context);
    }

    @Test(priority = -1)
    public void searchWithNEQ_stringAttr() throws AtlasBaseException {
        createDummyEntity(expectedEntityName,HIVE_TABLE_TYPE);
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria filterCriteria = getSingleFilterCondition("tableType", SearchParameters.Operator.NEQ, "Managed");
        params.setEntityFilters(filterCriteria);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, indexer.getVertexIndexKeys());
        EntitySearchProcessor processor = new EntitySearchProcessor(context);
        List<AtlasVertex> vertices = processor.execute();

        assertEquals(vertices.size(), 3);

        List<String> nameList = new ArrayList<>();
        for (AtlasVertex vertex : vertices) {
            nameList.add((String) entityRetriever.toAtlasEntityHeader(vertex, Collections.singleton("name")).getAttribute("name"));
        }

        assertTrue(nameList.contains(expectedEntityName));
    }

    @Test
    public void searchWithNEQ_pipeSeperatedAttr() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria filterCriteria = getSingleFilterCondition("__classificationNames", SearchParameters.Operator.NEQ, METRIC_CLASSIFICATION);
        params.setEntityFilters(filterCriteria);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, indexer.getVertexIndexKeys());
        EntitySearchProcessor processor = new EntitySearchProcessor(context);
        List<AtlasVertex> vertices = processor.execute();

        assertEquals(vertices.size(), 7);

        List<String> nameList = new ArrayList<>();
        for (AtlasVertex vertex : vertices) {
            nameList.add((String) entityRetriever.toAtlasEntityHeader(vertex, Collections.singleton("name")).getAttribute("name"));
        }

        assertTrue(nameList.contains(expectedEntityName));
    }

    @Test
    public void searchWithNEQ_doubleAttr() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria filterCriteria = getSingleFilterCondition("retention", SearchParameters.Operator.NEQ, "5");
        params.setEntityFilters(filterCriteria);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, indexer.getVertexIndexKeys());
        EntitySearchProcessor processor = new EntitySearchProcessor(context);
        List<AtlasVertex> vertices = processor.execute();

        assertEquals(vertices.size(), 1);

        List<String> nameList = new ArrayList<>();
        for (AtlasVertex vertex : vertices) {
            nameList.add((String) entityRetriever.toAtlasEntityHeader(vertex, Collections.singleton("name")).getAttribute("name"));
        }

        assertTrue(nameList.contains("hive_Table_Null_tableType"));
    }

    @Test
    public void ALLEntityType() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(SearchParameters.ALL_ENTITY_TYPES);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());

        EntitySearchProcessor processor = new EntitySearchProcessor(context);
        assertEquals(processor.execute().size(), 20);
    }

    @Test
    public void ALLEntityTypeWithTag() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(SearchParameters.ALL_ENTITY_TYPES);
        params.setClassification(FACT_CLASSIFICATION);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());

        EntitySearchProcessor processor = new EntitySearchProcessor(context);
        assertEquals(processor.execute().size(), 5);
    }

    @Test
    public void entityType() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(DATABASE_TYPE);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());

        EntitySearchProcessor processor = new EntitySearchProcessor(context);
        assertEquals(processor.execute().size(), 3);
    }

    @Test
    public void entityTypes() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(DATABASE_TYPE+","+HIVE_TABLE_TYPE);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());

        EntitySearchProcessor processor = new EntitySearchProcessor(context);
        assertEquals(processor.execute().size(), 14);
    }

    @Test(expectedExceptions = AtlasBaseException.class, expectedExceptionsMessageRegExp = "Not_Exists: Unknown/invalid typename")
    public void entityTypesNotAllowed() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName("Not_Exists");
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());
    }

    @Test(expectedExceptions = AtlasBaseException.class, expectedExceptionsMessageRegExp = "Attribute tableType not found for type "+DATABASE_TYPE)
    public void entityFiltersNotAllowed() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(DATABASE_TYPE+","+HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria filterCriteria = getSingleFilterCondition("tableType", SearchParameters.Operator.CONTAINS, "ETL");
        params.setEntityFilters(filterCriteria);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());
    }

    @Test
    public void entityTypesAndTag() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(DATABASE_TYPE+","+HIVE_TABLE_TYPE);
        params.setClassification(FACT_CLASSIFICATION);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());

        EntitySearchProcessor processor = new EntitySearchProcessor(context);
        assertEquals(processor.execute().size(), 3);
    }

    @Test
    public void searchWithEntityTypesAndEntityFilters() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(DATABASE_TYPE+","+HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria filterCriteria = getSingleFilterCondition("owner", SearchParameters.Operator.CONTAINS, "ETL");
        params.setEntityFilters(filterCriteria);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());

        EntitySearchProcessor processor = new EntitySearchProcessor(context);
        assertEquals(processor.execute().size(), 4);
    }

    @Test
    public void searchWithEntityTypesAndEntityFiltersAndTag() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(DATABASE_TYPE+","+HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria filterCriteria = getSingleFilterCondition("owner", SearchParameters.Operator.CONTAINS, "ETL");
        params.setEntityFilters(filterCriteria);
        params.setClassification(LOGDATA_CLASSIFICATION);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());

        EntitySearchProcessor processor = new EntitySearchProcessor(context);
        assertEquals(processor.execute().size(), 2);
    }

    @Test
    public void searchWithNotContains_stringAttr() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria filterCriteria = getSingleFilterCondition("tableType", SearchParameters.Operator.NOT_CONTAINS, "Managed");
        params.setEntityFilters(filterCriteria);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, indexer.getVertexIndexKeys());
        EntitySearchProcessor processor = new EntitySearchProcessor(context);
        List<AtlasVertex> vertices = processor.execute();

        assertEquals(vertices.size(), 3);

        List<String> nameList = new ArrayList<>();
        for (AtlasVertex vertex : vertices) {
            nameList.add((String) entityRetriever.toAtlasEntityHeader(vertex, Collections.singleton("name")).getAttribute("name"));
        }

        assertTrue(nameList.contains(expectedEntityName));
    }

    @Test
    public void searchWithNotContains_pipeSeperatedAttr() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria filterCriteria = getSingleFilterCondition("__classificationNames", SearchParameters.Operator.NOT_CONTAINS, METRIC_CLASSIFICATION);
        params.setEntityFilters(filterCriteria);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, indexer.getVertexIndexKeys());
        EntitySearchProcessor processor = new EntitySearchProcessor(context);
        List<AtlasVertex> vertices = processor.execute();

        assertEquals(vertices.size(), 7);

        List<String> nameList = new ArrayList<>();
        for (AtlasVertex vertex : vertices) {
            nameList.add((String) entityRetriever.toAtlasEntityHeader(vertex, Collections.singleton("name")).getAttribute("name"));
        }

        assertTrue(nameList.contains(expectedEntityName));
    }

    @AfterClass
    public void teardown() throws Exception {
        AtlasGraphProvider.cleanup();

        super.cleanup();
    }

    @Test
    public void testLast7Days() throws AtlasBaseException {
        SearchParameters.FilterCriteria ret = filtercriteriaDateRange("LAST_7_DAYS",typeRegistry,graph);
        ret.setAttributeName("createTime");
        GregorianCalendar startDate = new GregorianCalendar();
        GregorianCalendar endDate = new GregorianCalendar();
        startDate.add(Calendar.DATE, -6);

        String[] dates    = ret.getAttributeValue().split(",");
        String attrValue1 = dates[0];
        String attrValue2 = dates[1];

        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue1)))), formattedDate.format(startDate.getTime()));
        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue2)))), formattedDate.format(endDate.getTime()));
    }

    @Test
    public void testLastMonth() throws AtlasBaseException {
        SearchParameters.FilterCriteria ret = filtercriteriaDateRange("LAST_MONTH", typeRegistry,graph);
        Calendar originalstartdate = Calendar.getInstance();
        Calendar originalenddate = Calendar.getInstance();

        originalstartdate.add(Calendar.MONTH, -1);
        originalstartdate.set(Calendar.DAY_OF_MONTH, originalstartdate.getActualMinimum(Calendar.DAY_OF_MONTH));
        originalenddate.add(Calendar.MONTH, -1);
        originalenddate.set(Calendar.DAY_OF_MONTH, originalenddate.getActualMaximum(Calendar.DAY_OF_MONTH));

        String[] dates    = ret.getAttributeValue().split(",");
        String attrValue1 = dates[0];
        String attrValue2 = dates[1];

        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue1)))), formattedDate.format(originalstartdate.getTime()));
        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue2)))), formattedDate.format(originalenddate.getTime()));
    }

    @Test
    public void testLast30Days() throws AtlasBaseException {
        SearchParameters.FilterCriteria ret = filtercriteriaDateRange("LAST_30_DAYS", typeRegistry,graph);
        GregorianCalendar startDate = new GregorianCalendar();
        GregorianCalendar endDate   = new GregorianCalendar();
        startDate.add(Calendar.DATE, -29);

        String[] dates    = ret.getAttributeValue().split(",");
        String attrValue1 = dates[0];
        String attrValue2 = dates[1];

        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue1)))), formattedDate.format(startDate.getTime()));
        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue2)))), formattedDate.format(endDate.getTime()));
    }

    @Test
    public void testYesterday() throws AtlasBaseException {
        SearchParameters.FilterCriteria ret = filtercriteriaDateRange("YESTERDAY", typeRegistry,graph);
        GregorianCalendar yesterdayDate = new GregorianCalendar();
        yesterdayDate.add(Calendar.DATE, -1);

        String[] dates    = ret.getAttributeValue().split(",");
        String attrValue1 = dates[0];

        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue1)))), formattedDate.format(yesterdayDate.getTime()));
    }

    @Test
    public void testThisMonth() throws AtlasBaseException {
        SearchParameters.FilterCriteria ret = filtercriteriaDateRange("THIS_MONTH", typeRegistry,graph);
        Calendar originalstartdate = Calendar.getInstance();
        Calendar originalenddate = Calendar.getInstance();

        originalstartdate.set(Calendar.DAY_OF_MONTH, originalstartdate.getActualMinimum(Calendar.DAY_OF_MONTH));
        originalenddate.set(Calendar.DAY_OF_MONTH, originalenddate.getActualMaximum(Calendar.DAY_OF_MONTH));

        String[] dates    = ret.getAttributeValue().split(",");
        String attrValue1 = dates[0];
        String attrValue2 = dates[1];

        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue1)))), formattedDate.format(originalstartdate.getTime()));
        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue2)))), formattedDate.format(originalenddate.getTime()));
    }

    @Test
    public void testThisQuarter() throws AtlasBaseException {
        SearchParameters.FilterCriteria ret = filtercriteriaDateRange("THIS_QUARTER", typeRegistry,graph);
        Calendar originalstartdate = Calendar.getInstance();
        Calendar originalenddate = Calendar.getInstance();
        originalstartdate.add(Calendar.MONTH, -1);
        originalstartdate.set(Calendar.DAY_OF_MONTH, originalstartdate.getActualMinimum(Calendar.DAY_OF_MONTH));
        originalenddate.add(Calendar.MONTH, 1);
        originalenddate.set(Calendar.DAY_OF_MONTH, originalenddate.getActualMaximum(Calendar.DAY_OF_MONTH));

        String[] dates    = ret.getAttributeValue().split(",");
        String attrValue1 = dates[0];
        String attrValue2 = dates[1];

        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue1)))), formattedDate.format(originalstartdate.getTime()));
        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue2)))), formattedDate.format(originalenddate.getTime()));
    }

    @Test
    public void testLastQuarter() throws AtlasBaseException {
        SearchParameters.FilterCriteria ret = filtercriteriaDateRange("LAST_QUARTER", typeRegistry,graph);
        Calendar originalstartdate = Calendar.getInstance();
        Calendar originalenddate = Calendar.getInstance();
        originalstartdate.add(Calendar.MONTH, -4);
        originalstartdate.set(Calendar.DAY_OF_MONTH, originalstartdate.getActualMinimum(Calendar.DAY_OF_MONTH));
        originalenddate.add(Calendar.MONTH, -2);
        originalenddate.set(Calendar.DAY_OF_MONTH, originalenddate.getActualMaximum(Calendar.DAY_OF_MONTH));

        String[] dates    = ret.getAttributeValue().split(",");
        String attrValue1 = dates[0];
        String attrValue2 = dates[1];

        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue1)))), formattedDate.format(originalstartdate.getTime()));
        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue2)))), formattedDate.format(originalenddate.getTime()));
    }


    @Test
    public void testLast3Months() throws AtlasBaseException {
        SearchParameters.FilterCriteria ret = filtercriteriaDateRange("LAST_3_MONTHS", typeRegistry,graph);
        Calendar originalstartdate = Calendar.getInstance();
        Calendar originalenddate = Calendar.getInstance();

        originalstartdate.add(Calendar.MONTH, -3);
        originalstartdate.set(Calendar.DAY_OF_MONTH, originalstartdate.getActualMinimum(Calendar.DAY_OF_MONTH));
        originalenddate.add(Calendar.MONTH, -1);
        originalenddate.set(Calendar.DAY_OF_MONTH, originalenddate.getActualMaximum(Calendar.DAY_OF_MONTH));

        String[] dates    = ret.getAttributeValue().split(",");
        String attrValue1 = dates[0];
        String attrValue2 = dates[1];

        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue1)))), formattedDate.format(originalstartdate.getTime()));
        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue2)))), formattedDate.format(originalenddate.getTime()));
    }

    @Test
    public void testThisYear() throws AtlasBaseException {
        SearchParameters.FilterCriteria ret = filtercriteriaDateRange("THIS_YEAR",typeRegistry,graph);
        Calendar originalstartdate = Calendar.getInstance();
        Calendar originalenddate = Calendar.getInstance();

        originalstartdate.set(Calendar.MONTH, 0);
        originalstartdate.set(Calendar.DAY_OF_MONTH, originalstartdate.getActualMinimum(Calendar.DAY_OF_MONTH));

        originalenddate.set(Calendar.MONTH, 11);
        originalenddate.set(Calendar.DAY_OF_MONTH, originalenddate.getActualMaximum(Calendar.DAY_OF_MONTH));

        String[] dates    = ret.getAttributeValue().split(",");
        String attrValue1 = dates[0];
        String attrValue2 = dates[1];

        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue1)))), formattedDate.format(originalstartdate.getTime()));
        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue2)))), formattedDate.format(originalenddate.getTime()));
    }

    @Test
    public void testLastYear() throws AtlasBaseException {
        SearchParameters.FilterCriteria ret = filtercriteriaDateRange("LAST_YEAR",typeRegistry,graph);
        Calendar originalstartdate = Calendar.getInstance();
        Calendar originalenddate = Calendar.getInstance();

        originalstartdate.add(Calendar.YEAR, -1);
        originalstartdate.set(Calendar.MONTH, 0);
        originalstartdate.set(Calendar.DAY_OF_MONTH, originalstartdate.getActualMinimum(Calendar.DAY_OF_MONTH));

        originalenddate.add(Calendar.YEAR, -1);
        originalenddate.set(Calendar.MONTH, 11);
        originalenddate.set(Calendar.DAY_OF_MONTH, originalenddate.getActualMaximum(Calendar.DAY_OF_MONTH));

        String[] dates    = ret.getAttributeValue().split(",");
        String attrValue1 = dates[0];
        String attrValue2 = dates[1];

        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue1)))), formattedDate.format(originalstartdate.getTime()));
        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue2)))), formattedDate.format(originalenddate.getTime()));
    }

    @Test
    public void testLast12Months() throws AtlasBaseException {
        SearchParameters.FilterCriteria ret = filtercriteriaDateRange("LAST_12_MONTHS",typeRegistry,graph);
        Calendar originalstartdate = Calendar.getInstance();
        Calendar originalenddate = Calendar.getInstance();

        originalstartdate.add(Calendar.MONTH, -12);
        originalstartdate.set(Calendar.DAY_OF_MONTH, originalstartdate.getActualMinimum(Calendar.DAY_OF_MONTH));

        originalenddate.add(Calendar.MONTH, -1);
        originalenddate.set(Calendar.DAY_OF_MONTH, originalenddate.getActualMaximum(Calendar.DAY_OF_MONTH));

        String[] dates    = ret.getAttributeValue().split(",");
        String attrValue1 = dates[0];
        String attrValue2 = dates[1];

        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue1)))), formattedDate.format(originalstartdate.getTime()));
        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue2)))), formattedDate.format(originalenddate.getTime()));
    }

    @Test
    public void testLast6Months() throws AtlasBaseException {
        SearchParameters.FilterCriteria ret = filtercriteriaDateRange("LAST_6_MONTHS",typeRegistry,graph);
        Calendar originalstartdate = Calendar.getInstance();
        Calendar originalenddate = Calendar.getInstance();

        originalstartdate.add(Calendar.MONTH, -6);
        originalstartdate.set(Calendar.DAY_OF_MONTH, originalstartdate.getActualMinimum(Calendar.DAY_OF_MONTH));

        originalenddate.add(Calendar.MONTH, -1);
        originalenddate.set(Calendar.DAY_OF_MONTH, originalenddate.getActualMaximum(Calendar.DAY_OF_MONTH));

        String[] dates    = ret.getAttributeValue().split(",");
        String attrValue1 = dates[0];
        String attrValue2 = dates[1];

        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue1)))), formattedDate.format(originalstartdate.getTime()));
        assertEquals(formattedDate.format(new Date((Long.parseLong(attrValue2)))), formattedDate.format(originalenddate.getTime()));
    }

    private static SearchParameters.FilterCriteria filtercriteriaDateRange(String attributeValue, AtlasTypeRegistry typeRegistry, AtlasGraph graph) throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria filterCriteria = new SearchParameters.FilterCriteria();
        params.setEntityFilters(filterCriteria);
        params.setLimit(20);
        SearchContext context = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());
        EntitySearchProcessor processor = new EntitySearchProcessor(context);
        filterCriteria.setCondition(SearchParameters.FilterCriteria.Condition.AND);
        filterCriteria.setOperator(SearchParameters.Operator.TIME_RANGE);
        filterCriteria.setAttributeValue(attributeValue);
        SearchParameters.FilterCriteria ret = processor.processDateRange(filterCriteria);
        return ret;
    }
}
