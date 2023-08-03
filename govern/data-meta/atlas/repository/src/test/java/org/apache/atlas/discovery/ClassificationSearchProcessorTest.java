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

import org.apache.atlas.AtlasClient;
import org.apache.atlas.BasicTestSetup;
import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.discovery.SearchParameters;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.graph.GraphBackedSearchIndexer;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStream;
import org.apache.atlas.repository.store.graph.v2.EntityGraphRetriever;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.atlas.model.discovery.SearchParameters.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

@Guice(modules = TestModules.TestOnlyModule.class)
public class ClassificationSearchProcessorTest extends BasicTestSetup {

    @Inject
    private AtlasGraph graph;
    @Inject
    public GraphBackedSearchIndexer indexer;
    @Inject
    private EntityGraphRetriever entityRetriever;

    private int    totalClassifiedEntities              = 0;
    private int    dimensionTagEntities                 = 10;
    private String dimensionTagDeleteGuid;
    private String dimensionalTagGuid;

    @BeforeClass
    public void setup() throws Exception {
        super.initialize();

        setupTestData();
        createDimensionTaggedEntityAndDelete();
        createDimensionalTaggedEntityWithAttr();
    }

    @Test(priority = -1)
    public void searchByALLTag() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setClassification(ALL_CLASSIFICATION_TYPES);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, indexer.getVertexIndexKeys());
        ClassificationSearchProcessor processor = new ClassificationSearchProcessor(context);
        List<AtlasVertex> vertices = processor.execute();

        Assert.assertTrue(CollectionUtils.isNotEmpty(vertices));
        totalClassifiedEntities = vertices.size();
    }

    @Test
    public void searchByALLTagAndIndexSysFilters() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setClassification(ALL_CLASSIFICATION_TYPES);
        FilterCriteria filterCriteria = getSingleFilterCondition("__timestamp", Operator.LT, String.valueOf(System.currentTimeMillis()));
        params.setTagFilters(filterCriteria);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, indexer.getVertexIndexKeys());
        ClassificationSearchProcessor processor = new ClassificationSearchProcessor(context);
        List<AtlasVertex> vertices = processor.execute();

        Assert.assertTrue(CollectionUtils.isNotEmpty(vertices));
        assertEquals(vertices.size(), totalClassifiedEntities);
    }

    @Test
    public void searchByALLTagAndIndexSysFiltersToTestLimit() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setClassification(ALL_CLASSIFICATION_TYPES);
        FilterCriteria filterCriteria = getSingleFilterCondition("__timestamp", Operator.LT, String.valueOf(System.currentTimeMillis()));
        params.setTagFilters(filterCriteria);
        params.setLimit(totalClassifiedEntities - 2);

        SearchContext context = new SearchContext(params, typeRegistry, graph, indexer.getVertexIndexKeys());
        ClassificationSearchProcessor processor = new ClassificationSearchProcessor(context);
        List<AtlasVertex> vertices = processor.execute();

        Assert.assertTrue(CollectionUtils.isNotEmpty(vertices));
        assertEquals(vertices.size(), totalClassifiedEntities - 2);
    }

    //@Test
    public void searchByNOTCLASSIFIED() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setClassification(NO_CLASSIFICATIONS);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, indexer.getVertexIndexKeys());
        ClassificationSearchProcessor processor = new ClassificationSearchProcessor(context);
        List<AtlasVertex> vertices = processor.execute();

        Assert.assertTrue(CollectionUtils.isNotEmpty(vertices));
        assertEquals(vertices.size(), 20);
    }

    @Test
    public void searchByTag() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setClassification(DIMENSION_CLASSIFICATION);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, indexer.getVertexIndexKeys());
        ClassificationSearchProcessor processor = new ClassificationSearchProcessor(context);
        List<AtlasVertex> vertices = processor.execute();

        Assert.assertTrue(CollectionUtils.isNotEmpty(vertices));
        assertEquals(vertices.size(), dimensionTagEntities);
    }

    @Test
    public void searchByTagAndTagFilters() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setClassification(DIMENSIONAL_CLASSIFICATION);
        FilterCriteria filterCriteria = getSingleFilterCondition("attr1", Operator.EQ, "Test");
        params.setTagFilters(filterCriteria);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, indexer.getVertexIndexKeys());
        ClassificationSearchProcessor processor = new ClassificationSearchProcessor(context);
        List<AtlasVertex> vertices = processor.execute();

        Assert.assertTrue(CollectionUtils.isNotEmpty(vertices));
        assertEquals(vertices.size(), 1);
        List<String> guids = vertices.stream().map(g -> {
            try {
                return entityRetriever.toAtlasEntityHeader(g).getGuid();
            } catch (AtlasBaseException e) {
                fail("Failure in mapping vertex to AtlasEntityHeader");
            }
            return "";
        }).collect(Collectors.toList());
        Assert.assertTrue(guids.contains(dimensionalTagGuid));

    }

    @Test
    public void searchByTagAndIndexSysFilters() throws AtlasBaseException {

        SearchParameters params = new SearchParameters();
        params.setClassification(DIMENSION_CLASSIFICATION);
        FilterCriteria filterCriteria = getSingleFilterCondition("__timestamp", Operator.LT, String.valueOf(System.currentTimeMillis()));
        params.setTagFilters(filterCriteria);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, indexer.getVertexIndexKeys());
        ClassificationSearchProcessor processor = new ClassificationSearchProcessor(context);
        List<AtlasVertex> vertices = processor.execute();

        Assert.assertTrue(CollectionUtils.isNotEmpty(vertices));
        assertEquals(vertices.size(), dimensionTagEntities);
    }

    @Test
    public void searchByWildcardTag() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setClassification("Dimension*");
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, indexer.getVertexIndexKeys());
        ClassificationSearchProcessor processor = new ClassificationSearchProcessor(context);
        List<AtlasVertex> vertices = processor.execute();

        Assert.assertTrue(CollectionUtils.isNotEmpty(vertices));
        assertEquals(vertices.size(), dimensionTagEntities + 1);

    }

    @Test
    public void searchByALLWildcardTag() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setClassification("*");
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, indexer.getVertexIndexKeys());
        ClassificationSearchProcessor processor = new ClassificationSearchProcessor(context);
        List<AtlasVertex> vertices = processor.execute();

        Assert.assertTrue(CollectionUtils.isNotEmpty(vertices));
        assertEquals(vertices.size(),20);

    }

    @Test
    public void searchWithNotContains() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setClassification(DIMENSIONAL_CLASSIFICATION);
        FilterCriteria filterCriteria = getSingleFilterCondition("attr1", Operator.NOT_CONTAINS, "Test");
        params.setTagFilters(filterCriteria);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, indexer.getVertexIndexKeys());
        ClassificationSearchProcessor processor = new ClassificationSearchProcessor(context);
        List<AtlasVertex> vertices = processor.execute();

        Assert.assertTrue(CollectionUtils.isEmpty(vertices));
    }


    @Test
    public void searchByTagAndGraphSysFilters() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setClassification(DIMENSION_CLASSIFICATION);
        FilterCriteria filterCriteria = getSingleFilterCondition("__entityStatus", Operator.EQ, "DELETED");
        params.setTagFilters(filterCriteria);
        params.setExcludeDeletedEntities(false);
        params.setLimit(20);

        SearchContext context = new SearchContext(params, typeRegistry, graph, indexer.getVertexIndexKeys());
        ClassificationSearchProcessor processor = new ClassificationSearchProcessor(context);
        List<AtlasVertex> vertices = processor.execute();

        Assert.assertTrue(CollectionUtils.isNotEmpty(vertices));
        assertEquals(vertices.size(), 1);
        List<String> guids = vertices.stream().map(g -> {
            try {
                return entityRetriever.toAtlasEntityHeader(g).getGuid();
            } catch (AtlasBaseException e) {
                fail("Failure in mapping vertex to AtlasEntityHeader");
            }
            return "";
        }).collect(Collectors.toList());
        Assert.assertTrue(guids.contains(dimensionTagDeleteGuid));

    }

    @Test
    public void searchByWildcardTagMarker() throws AtlasBaseException {
        final String LAST_MARKER = "-1";
        SearchParameters params = new SearchParameters();
        params.setClassification("*");
        int limit     = 5;
        String marker = "*";
        params.setLimit(limit);

        while (!StringUtils.equals(marker, LAST_MARKER)) {
            params.setMarker(marker);
            SearchContext context = new SearchContext(params, typeRegistry, graph, indexer.getVertexIndexKeys());
            ClassificationSearchProcessor processor = new ClassificationSearchProcessor(context);
            List<AtlasVertex> vertices = processor.execute();
            long totalCount = vertices.size();
            marker = processor.getNextMarker();

            if (totalCount < limit) {
                assertEquals(marker, LAST_MARKER);
                break;
            } else {
                Assert.assertNotNull(marker);
                assertEquals(vertices.size(), 5);
            }
        }
    }

    @Test   //marker functionality is not supported in this case
    public void searchByTagAndGraphSysFiltersMarker() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setClassification(DIMENSION_CLASSIFICATION);
        FilterCriteria filterCriteria = getSingleFilterCondition("__entityStatus", Operator.EQ, "DELETED");
        params.setTagFilters(filterCriteria);
        params.setExcludeDeletedEntities(false);
        params.setLimit(20);
        params.setMarker("*");

        SearchContext context = new SearchContext(params, typeRegistry, graph, indexer.getVertexIndexKeys());
        ClassificationSearchProcessor processor = new ClassificationSearchProcessor(context);
        List<AtlasVertex> vertices = processor.execute();

        Assert.assertTrue(CollectionUtils.isNotEmpty(vertices));
        assertEquals(vertices.size(), 1);
        List<String> guids = vertices.stream().map(g -> {
            try {
                return entityRetriever.toAtlasEntityHeader(g).getGuid();
            } catch (AtlasBaseException e) {
                fail("Failure in mapping vertex to AtlasEntityHeader");
            }
            return "";
        }).collect(Collectors.toList());
        Assert.assertTrue(guids.contains(dimensionTagDeleteGuid));

        Assert.assertNull(processor.getNextMarker());
    }

    private void createDimensionTaggedEntityAndDelete() throws AtlasBaseException {
        AtlasEntity entityToDelete = new AtlasEntity(HIVE_TABLE_TYPE);
        entityToDelete.setAttribute("name", "entity to be deleted");
        entityToDelete.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, "entity.tobedeleted");

        List<AtlasClassification> cls = new ArrayList<>();
        cls.add(new AtlasClassification(DIMENSION_CLASSIFICATION));
        entityToDelete.setClassifications(cls);

        //create entity
        EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(new AtlasEntity.AtlasEntitiesWithExtInfo(entityToDelete)), false);
        AtlasEntityHeader entityHeader = response.getCreatedEntities().get(0);
        dimensionTagDeleteGuid = entityHeader.getGuid();

        //delete entity
        entityStore.deleteById(dimensionTagDeleteGuid);
    }

    private void createDimensionalTaggedEntityWithAttr() throws AtlasBaseException {
        AtlasEntity entityToDelete = new AtlasEntity(HIVE_TABLE_TYPE);
        entityToDelete.setAttribute("name", "Entity1");
        entityToDelete.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, "entity.one");

        List<AtlasClassification> cls = new ArrayList<>();
        cls.add(new AtlasClassification(DIMENSIONAL_CLASSIFICATION, new HashMap<String, Object>() {{
            put("attr1", "Test");
        }}));
        entityToDelete.setClassifications(cls);

        //create entity
        final EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(new AtlasEntity.AtlasEntitiesWithExtInfo(entityToDelete)), false);
        AtlasEntityHeader entityHeader = response.getCreatedEntities().get(0);
        dimensionalTagGuid = entityHeader.getGuid();

    }

    @AfterClass
    public void teardown() throws Exception {
        AtlasGraphProvider.cleanup();

        super.cleanup();
    }
}
