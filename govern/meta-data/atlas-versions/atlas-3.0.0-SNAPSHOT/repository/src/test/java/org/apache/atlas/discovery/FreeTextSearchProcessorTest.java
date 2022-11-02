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
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.repository.store.graph.v2.EntityGraphRetriever;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

@Guice(modules = TestModules.TestOnlyModule.class)
public class FreeTextSearchProcessorTest extends BasicTestSetup {

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

    @Test
    public void searchTablesByName() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName("hive_table");
        params.setQuery("sales");
        params.setExcludeDeletedEntities(true);
        params.setLimit(3);

        SearchContext context = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());

        FreeTextSearchProcessor processor = new FreeTextSearchProcessor(context);

        assertEquals(processor.getResultCount(), 3);
        assertEquals(processor.execute().size(), 3);
    }

    @Test
    public void searchByNameSortBy() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName("hive_table");
        params.setQuery("sales");
        params.setExcludeDeletedEntities(true);
        params.setLimit(3);
        params.setSortBy("owner");
        params.setSortOrder(SortOrder.ASCENDING);

        SearchContext context = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());

        FreeTextSearchProcessor processor = new FreeTextSearchProcessor(context);

        List<AtlasVertex> vertices = processor.execute();

        assertEquals(processor.getResultCount(), 3);
        assertEquals(vertices.size(), 3);

        AtlasVertex firstVertex = vertices.get(0);

        String firstOwner = (String) entityRetriever.toAtlasEntityHeader(firstVertex, Sets.newHashSet(params.getSortBy())).getAttribute(params.getSortBy());

        AtlasVertex secondVertex = vertices.get(1);
        String secondOwner = (String) entityRetriever.toAtlasEntityHeader(secondVertex, Sets.newHashSet(params.getSortBy())).getAttribute(params.getSortBy());

        assertEquals(firstOwner, "Jane BI");
        assertEquals(secondOwner, "Joe");
    }

    @Test
    public void emptySearch() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName("hive_table");
        params.setQuery("not_exists");
        params.setExcludeDeletedEntities(true);
        params.setLimit(3);

        SearchContext context = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());

        FreeTextSearchProcessor processor = new FreeTextSearchProcessor(context);

        assertEquals(processor.getResultCount(), 0);
        assertEquals(processor.execute().size(), 0);
    }

    @Test(expectedExceptions = AtlasBaseException.class, expectedExceptionsMessageRegExp = "not_exists_type: Unknown/invalid typename")
    public void searchByNonExistingClassification() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName("not_exists_type");
        params.setQuery("aaa");
        params.setExcludeDeletedEntities(true);
        params.setLimit(3);

        SearchContext context = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());
        new FreeTextSearchProcessor(context);
    }

    @Test(description = "filtering internal types")
    public void searchByTextFilteringInternalTypes() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setQuery("*");
        params.setExcludeDeletedEntities(true);
        params.setLimit(500);
        params.setOffset(0);

        SearchContext context = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());

        FreeTextSearchProcessor processor = new FreeTextSearchProcessor(context);

        List<AtlasVertex> vertices = processor.execute();

        assertNotNull(vertices);

        for (AtlasVertex vertex : vertices) {
            String entityTypeName = AtlasGraphUtilsV2.getTypeName(vertex);
            AtlasEntityType entityType = context.getTypeRegistry().getEntityTypeByName(entityTypeName);

            assertFalse(entityType.isInternalType());
            assertNotEquals("AtlasGlossaryTerm", entityTypeName);
        }
    }

    @AfterClass
    public void teardown() throws Exception {
        AtlasGraphProvider.cleanup();

        super.cleanup();
    }
}