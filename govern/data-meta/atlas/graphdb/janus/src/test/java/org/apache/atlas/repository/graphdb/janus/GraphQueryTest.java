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

package org.apache.atlas.repository.graphdb.janus;



import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.atlas.AtlasException;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasGraphQuery;
import org.apache.atlas.repository.graphdb.AtlasGraphQuery.ComparisionOperator;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.testng.annotations.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;



/**
 * Tests for AtlasJanusGraphQuery.
 */
@Test
public class GraphQueryTest extends AbstractGraphDatabaseTest {


    @Test
    public <V, E> void testQueryThatCannotRunInMemory() throws AtlasException {
        AtlasGraph<V, E> graph = getGraph();
        AtlasVertex<V, E> v1 = createVertex(graph);

        v1.setProperty("name", "Fred");
        v1.setProperty("size15", "15");

        AtlasVertex<V, E> v2 = createVertex(graph);
        v2.setProperty("name", "Fred");

        AtlasVertex<V, E> v3 = createVertex(graph);
        v3.setProperty("size15", "15");

        graph.commit();

        AtlasVertex<V, E> v4 = createVertex(graph);
        v4.setProperty("name", "Fred");
        v4.setProperty("size15", "15");

        AtlasGraphQuery q = graph.query();
        q.has("name", ComparisionOperator.NOT_EQUAL, "George");
        q.has("size15", "15");
        graph.commit();
        pause(); //pause to let the index get updated

        // Janusgraph 0.5.3 introduced changes to NOT-EQUAL operator
        // https://github.com/JanusGraph/janusgraph/issues/2205
        // Earlier behavior doesn't check if property exists, so all vertices without this property was also returned in 'neq'.
        // Now 'neq' checks if property exists and property is not-equal to property value
        assertQueryMatches(q, v1, v4);
    }

    @Test
    public  void testCombinationOfAndsAndOrs() throws AtlasException {
        AtlasJanusGraph graph = getAtlasJanusGraph();

        AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> v1 = createVertex(graph);

        v1.setProperty("name", "Fred");
        v1.setProperty("size15", "15");
        v1.setProperty("typeName", "Person");

        AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> v2 = createVertex(graph);
        v2.setProperty("name", "George");
        v2.setProperty("size15", "16");
        v2.setProperty("typeName", "Person");

        AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> v3 = createVertex(graph);
        v3.setProperty("name", "Jane");
        v3.setProperty("size15", "17");
        v3.setProperty("typeName", "Person");


        AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> v4 = createVertex(graph);
        v4.setProperty("name", "Bob");
        v4.setProperty("size15", "18");
        v4.setProperty("typeName", "Person");

        AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> v5 = createVertex(graph);
        v5.setProperty("name", "Julia");
        v5.setProperty("size15", "19");
        v5.setProperty("typeName", "Manager");


        AtlasGraphQuery q = getGraphQuery();
        q.has("typeName", "Person");
        //initially match
        AtlasGraphQuery inner1a = q.createChildQuery();
        AtlasGraphQuery inner1b = q.createChildQuery();
        inner1a.has("name", "Fred");
        inner1b.has("name", "Jane");
        q.or(toList(inner1a, inner1b));


        AtlasGraphQuery inner2a = q.createChildQuery();
        AtlasGraphQuery inner2b = q.createChildQuery();
        AtlasGraphQuery inner2c = q.createChildQuery();
        inner2a.has("size15", "18");
        inner2b.has("size15", "15");
        inner2c.has("size15", "16");
        q.or(toList(inner2a, inner2b, inner2c));

        assertQueryMatches(q, v1);
        graph.commit();
        pause(); //let the index update
        assertQueryMatches(q, v1);
    }

    @Test
    public  void testWithinStep() throws AtlasException {
        AtlasJanusGraph graph = getAtlasJanusGraph();

        AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> v1 = createVertex(graph);

        v1.setProperty("name", "Fred");
        v1.setProperty("size15", "15");
        v1.setProperty("typeName", "Person");

        AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> v2 = createVertex(graph);
        v2.setProperty("name", "George");
        v2.setProperty("size15", "16");
        v2.setProperty("typeName", "Person");

        AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> v3 = createVertex(graph);
        v3.setProperty("name", "Jane");
        v3.setProperty("size15", "17");
        v3.setProperty("typeName", "Person");


        AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> v4 = createVertex(graph);
        v4.setProperty("name", "Bob");
        v4.setProperty("size15", "18");
        v4.setProperty("typeName", "Person");

        AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> v5 = createVertex(graph);
        v5.setProperty("name", "Julia");
        v5.setProperty("size15", "19");
        v5.setProperty("typeName", "Manager");


        AtlasGraphQuery q = getGraphQuery();
        q.has("typeName", "Person");
        //initially match
        q.in("name", toList("Fred", "Jane"));
        q.in("size15", toList("18", "15", "16"));

        assertQueryMatches(q, v1);
        graph.commit();
        pause(); //let the index update
        assertQueryMatches(q, v1);
    }

    @Test
    public  void testWithinStepWhereGraphIsStale() throws AtlasException {
        AtlasJanusGraph graph = getAtlasJanusGraph();

        AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> v1 = createVertex(graph);

        v1.setProperty("name", "Fred");
        v1.setProperty("size15", "15");
        v1.setProperty("typeName", "Person");

        AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> v2 = createVertex(graph);
        v2.setProperty("name", "George");
        v2.setProperty("size15", "16");
        v2.setProperty("typeName", "Person");

        AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> v3 = createVertex(graph);
        v3.setProperty("name", "Jane");
        v3.setProperty("size15", "17");
        v3.setProperty("typeName", "Person");


        AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> v4 = createVertex(graph);
        v4.setProperty("name", "Bob");
        v4.setProperty("size15", "18");
        v4.setProperty("typeName", "Person");

        AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> v5 = createVertex(graph);
        v5.setProperty("name", "Julia");
        v5.setProperty("size15", "19");
        v5.setProperty("typeName", "Manager");


        AtlasGraphQuery q = getGraphQuery();
        q.has("typeName", "Person");
        //initially match
        q.in("name", toList("Fred", "Jane"));

        graph.commit();
        pause(); //let the index update
        assertQueryMatches(q, v1, v3);
      //make v3 no longer match the query.  Within step should filter out the vertex since it no longer matches.
        v3.setProperty("name", "Janet");
        assertQueryMatches(q, v1);
    }

    @Test
    public  void testSimpleOrQuery() throws AtlasException {
        AtlasJanusGraph graph = getAtlasJanusGraph();


        AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> v1 = createVertex(graph);

        v1.setProperty("name", "Fred");
        v1.setProperty("size15", "15");

        AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> v2 = createVertex(graph);
        v2.setProperty("name", "Fred");

        AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> v3 = createVertex(graph);
        v3.setProperty("size15", "15");

        graph.commit();

        AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> v4 = createVertex(graph);
        v4.setProperty("name", "Fred");
        v4.setProperty("size15", "15");

        AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> v5 = createVertex(graph);
        v5.setProperty("name", "George");
        v5.setProperty("size15", "16");

        AtlasGraphQuery q = graph.query();
        AtlasGraphQuery inner1 = q.createChildQuery().has("name", "Fred");
        AtlasGraphQuery inner2 = q.createChildQuery().has("size15", "15");
        q.or(toList(inner1, inner2));
        assertQueryMatches(q, v1, v2, v3, v4);
        graph.commit();
        pause(); //pause to let the indexer get updated (this fails frequently without a pause)
        assertQueryMatches(q, v1, v2, v3, v4);
    }




    @Test
    public <V, E> void testQueryMatchesAddedVertices() throws AtlasException {
        AtlasGraph<V, E> graph = getGraph();

        AtlasVertex<V, E> v1 = createVertex(graph);

        v1.setProperty("name", "Fred");
        v1.setProperty("size15", "15");

        AtlasVertex<V, E> v2 = createVertex(graph);
        v2.setProperty("name", "Fred");

        AtlasVertex<V, E> v3 = createVertex(graph);
        v3.setProperty("size15", "15");

        graph.commit();

        AtlasVertex<V, E> v4 = createVertex(graph);
        v4.setProperty("name", "Fred");
        v4.setProperty("size15", "15");

        AtlasGraphQuery q = getGraphQuery();
        q.has("name", "Fred");
        q.has("size15", "15");

        assertQueryMatches(q, v1, v4);
        graph.commit();
        assertQueryMatches(q, v1, v4);

    }


    @Test
    public <V, E> void testQueryDoesNotMatchRemovedVertices() throws AtlasException {
        AtlasGraph<V, E> graph = getGraph();

        AtlasVertex<V, E> v1 = createVertex(graph);

        v1.setProperty("name", "Fred");
        v1.setProperty("size15", "15");

        AtlasVertex<V, E> v2 = createVertex(graph);
        v2.setProperty("name", "Fred");

        AtlasVertex<V, E> v3 = createVertex(graph);
        v3.setProperty("size15", "15");

        AtlasVertex<V, E> v4 = createVertex(graph);
        v4.setProperty("name", "Fred");
        v4.setProperty("size15", "15");

        graph.commit();

        graph.removeVertex(v1);

        AtlasGraphQuery q = getGraphQuery();
        q.has("name", "Fred");
        q.has("size15", "15");

        assertQueryMatches(q, v4);
        graph.commit();

        assertQueryMatches(q, v4);
    }

    @Test
    public <V, E> void testQueryDoesNotMatchUncommittedAddedAndRemovedVertices() throws AtlasException {
        AtlasGraph<V, E> graph = getGraph();

        AtlasVertex<V, E> v1 = createVertex(graph);

        v1.setProperty("name", "Fred");
        v1.setProperty("size15", "15");

        AtlasVertex<V, E> v2 = createVertex(graph);
        v2.setProperty("name", "Fred");

        AtlasVertex<V, E> v3 = createVertex(graph);
        v3.setProperty("size15", "15");

        AtlasVertex<V, E> v4 = createVertex(graph);
        v4.setProperty("name", "Fred");
        v4.setProperty("size15", "15");


        AtlasGraphQuery q = getGraphQuery();
        q.has("name", "Fred");
        q.has("size15", "15");

        assertQueryMatches(q, v1, v4);

        graph.removeVertex(v1);


        assertQueryMatches(q, v4);
        graph.commit();

        assertQueryMatches(q, v4);
    }


    @Test
    public <V, E> void testQueryResultsReflectPropertyAdd() throws AtlasException {
        AtlasGraph<V, E> graph = getGraph();

        AtlasVertex<V, E> v1 = createVertex(graph);
        v1.setProperty("name", "Fred");
        v1.setProperty("size15", "15");
        v1.addProperty(TRAIT_NAMES, "trait1");
        v1.addProperty(TRAIT_NAMES, "trait2");

        AtlasVertex<V, E> v2 = createVertex(graph);
        v2.setProperty("name", "Fred");
        v2.addProperty(TRAIT_NAMES, "trait1");

        AtlasVertex<V, E> v3 = createVertex(graph);
        v3.setProperty("size15", "15");
        v3.addProperty(TRAIT_NAMES, "trait2");

        AtlasGraphQuery query = getGraphQuery();
        query.has("name", "Fred");
        query.has(TRAIT_NAMES, "trait1");
        query.has("size15", "15");

        assertQueryMatches(query, v1);
        //make v3 match the query
        v3.setProperty("name", "Fred");
        v3.addProperty(TRAIT_NAMES, "trait1");
        assertQueryMatches(query, v1, v3);
        v3.removeProperty(TRAIT_NAMES);
        assertQueryMatches(query, v1);
        v3.addProperty(TRAIT_NAMES, "trait2");
        assertQueryMatches(query, v1);
        v1.removeProperty(TRAIT_NAMES);
        assertQueryMatches(query);
        graph.commit();
        assertQueryMatches(query);

    }

    private static <T> List<T> toList(Iterable<T> itr) {
        List<T> result = new ArrayList<T>();
        for(T object : itr) {
            result.add(object);
        }
        return result;

    }

    private <V, E> void assertQueryMatches(AtlasGraphQuery expr, AtlasVertex... expectedResults) throws AtlasException {

        //getGraph().commit();
        Collection<AtlasVertex<AtlasJanusVertex, AtlasJanusEdge>> temp = toList(expr.vertices());
        //filter out vertices from previous test executions
        Collection<AtlasVertex<AtlasJanusVertex, AtlasJanusEdge>> result =
                Collections2.filter(temp, new Predicate<AtlasVertex<AtlasJanusVertex, AtlasJanusEdge>>() {

                    @Override
                    public boolean apply(AtlasVertex<AtlasJanusVertex, AtlasJanusEdge> input) {
                        return newVertices.contains(input);
                    }

                });
        String errorMessage = "Expected/found result sizes differ.  Expected: "
                + Arrays.asList(expectedResults).toString() +", found: " + result;
        assertEquals(errorMessage, expectedResults.length, result.size());

        for(AtlasVertex<V, E> v : expectedResults) {
            assertTrue(result.contains(v));
        }
    }

    private static List<Object> toList(Object...objects) {
        return Arrays.asList(objects);
    }

    private AtlasGraphQuery<AtlasJanusVertex, AtlasJanusEdge> getGraphQuery() {
        return getAtlasJanusGraph().query();
    }

    private void pause() {
        try {
            Thread.sleep(5000);
        } catch(InterruptedException e) {
           //ignore
        }
    }
}
