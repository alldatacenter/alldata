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

import org.apache.atlas.BasicTestSetup;
import org.apache.atlas.SortOrder;
import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.discovery.SearchParameters;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.graph.GraphBackedSearchIndexer;
import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.commons.collections.CollectionUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

@Guice(modules = TestModules.TestOnlyModule.class)
public class RelationshipSearchProcessorTest extends BasicTestSetup {
    @Inject
    private AtlasGraph graph;

    @BeforeClass
    public void setup() throws Exception {
        super.initialize();

        setupRelationshipTestData();
    }

    @Inject
    public GraphBackedSearchIndexer indexer;

    @Test
    public void totalRelationships() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setRelationshipName("user_post");
        params.setLimit(20);

       executeAndAssert(params,17);
    }

    @Test
    public void searchByAttribute() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setRelationshipName("user_post");
        params.setRelationshipFilters(getSingleFilterCondition("user_name", SearchParameters.Operator.CONTAINS, "Ajay"));
        params.setLimit(20);

        executeAndAssert(params, 7);
    }

    @Test
    public void searchByAttributes() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setRelationshipName("user_post");

        SearchParameters.FilterCriteria filterCriteria = new SearchParameters.FilterCriteria();
        filterCriteria.setCondition(SearchParameters.FilterCriteria.Condition.AND);

        List<SearchParameters.FilterCriteria> criteria = new ArrayList<>();
        SearchParameters.FilterCriteria f1 = new SearchParameters.FilterCriteria();
        f1.setAttributeName("post_name");
        f1.setOperator(SearchParameters.Operator.EQ);
        f1.setAttributeValue("christmas-post@Mary");
        criteria.add(f1);

        SearchParameters.FilterCriteria f2 = new SearchParameters.FilterCriteria();
        f2.setAttributeName("reaction");
        f2.setOperator(SearchParameters.Operator.EQ);
        f2.setAttributeValue("like");
        criteria.add(f2);

        filterCriteria.setCriterion(criteria);

        params.setRelationshipFilters(filterCriteria);
        params.setLimit(20);

        executeAndAssert(params, 2);
    }

    @Test(expectedExceptions = AtlasBaseException.class, expectedExceptionsMessageRegExp = "Attribute name not found for type user_post")
    public void invalidAttribute() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setRelationshipName("user_post");
        params.setRelationshipFilters(getSingleFilterCondition("name", SearchParameters.Operator.CONTAINS, "Ajay"));
        params.setLimit(20);

        executeAndAssert(params, 0);
    }

    public void sortByPostName() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setRelationshipName("user_post");
        params.setRelationshipFilters(getSingleFilterCondition("user_name", SearchParameters.Operator.CONTAINS, "Ajay"));
        params.setLimit(20);
        params.setSortBy("post_name");
        params.setSortOrder(SortOrder.DESCENDING);

        List<AtlasEdge> edges = executeAndAssert(params, 7);

        //ideally it should be below, but as the field is of Text type, it tokenizes the string Eg: "christmas-post Mary" and hence we cannot guarentee sorting
        //assertEquals("christmas-post@Mary",edges.get(0).getProperty("user_post.post_name", String.class));
        //assertEquals("trip-post@Ajay",edges.get(6).getProperty("user_post.post_name", String.class));

        assertEquals("ganeshchaturthi-post@Divya",edges.get(0).getProperty("user_post.post_name", String.class));
    }

    @Test
    public void sortByReaction() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setRelationshipName("user_post");
        params.setRelationshipFilters(getSingleFilterCondition("user_name", SearchParameters.Operator.CONTAINS, "Ajay"));
        params.setLimit(20);
        params.setSortBy("reaction");
        params.setSortOrder(SortOrder.DESCENDING);

        List<AtlasEdge> edges = executeAndAssert(params, 7);

        assertEquals("wow", edges.get(0).getProperty("user_post.reaction", String.class));
        assertEquals("create", edges.get(6).getProperty("user_post.reaction", String.class));

    }
    @Test
    public void searchBymultipleTypes() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setRelationshipName("user_post,highlight_post");
        params.setLimit(30);

        executeAndAssert(params,25);
    }

    @Test(expectedExceptions = AtlasBaseException.class, expectedExceptionsMessageRegExp = "Attribute post_name not found for type highlight_post")
    public void searchBymultipleTypesFilter() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setRelationshipName("user_post,highlight_post");
        params.setRelationshipFilters(getSingleFilterCondition("post_name", SearchParameters.Operator.CONTAINS, "Ajay"));
        params.setLimit(30);

        executeAndAssert(params,0);
    }

    @Test
    public void searchByTypeMarker() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setRelationshipName("user_post,highlight_post");
        params.setLimit(10);

        //1st page
        params.setMarker("*");
        SearchContext context1 = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());
        RelationshipSearchProcessor processor1 = new RelationshipSearchProcessor(context1, indexer.getEdgeIndexKeys());

        List<AtlasEdge> result1 = processor1.executeEdges();

        Assert.assertTrue(CollectionUtils.isNotEmpty(result1));
        assertEquals(result1.size(), 10);
        assertNotNull(processor1.getNextMarker());

        //2nd page
        params.setMarker(processor1.getNextMarker());
        SearchContext context2 = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());
        RelationshipSearchProcessor processor2 = new RelationshipSearchProcessor(context2, indexer.getEdgeIndexKeys());

        List<AtlasEdge> result2 = processor2.executeEdges();

        Assert.assertTrue(CollectionUtils.isNotEmpty(result2));
        assertEquals(result2.size(), 10);
        assertNotNull(processor2.getNextMarker());
    }

    private List<AtlasEdge> executeAndAssert(SearchParameters params, int expected) throws AtlasBaseException {
        SearchContext context = new SearchContext(params, typeRegistry, graph, Collections.<String>emptySet());
        RelationshipSearchProcessor processor = new RelationshipSearchProcessor(context, indexer.getEdgeIndexKeys());

        List<AtlasEdge> result = processor.executeEdges();

        Assert.assertTrue(CollectionUtils.isNotEmpty(result));
        assertEquals(result.size(), expected);

        return result;
    }

    @AfterClass
    public void teardown() throws Exception {
        AtlasGraphProvider.cleanup();

        super.cleanup();
    }
}
