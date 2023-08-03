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
package org.apache.atlas.repository.graphdb;

import org.apache.atlas.AtlasException;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.groovy.GroovyExpression;
import org.apache.atlas.type.AtlasType;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

/**
 * Represents a graph.
 *
 * @param <V> vertex implementation class
 * @param <E> edge implementation class
 */
public interface AtlasGraph<V, E> {

    /**
     * Adds an edge to the graph.
     *
     * @param outVertex
     * @param inVertex
     * @param label
     * @return
     */
    AtlasEdge<V, E> addEdge(AtlasVertex<V, E> outVertex, AtlasVertex<V, E> inVertex, String label);

    /**
     * Fetch edges between two vertices using relationshipLabel
     * @param fromVertex
     * @param toVertex
     * @param relationshipLabel
     * @return
     */
    AtlasEdge<V, E> getEdgeBetweenVertices(AtlasVertex fromVertex, AtlasVertex toVertex, String relationshipLabel);

        /**
         * Adds a vertex to the graph.
         *
         * @return
         */
    AtlasVertex<V, E> addVertex();

    /**
     * Removes the specified edge from the graph.
     *
     * @param edge
     */
    void removeEdge(AtlasEdge<V, E> edge);

    /**
     * Removes the specified vertex from the graph.
     *
     * @param vertex
     */
    void removeVertex(AtlasVertex<V, E> vertex);

    /**
     * Retrieves the edge with the specified id.    As an optimization, a non-null Edge may be
     * returned by some implementations if the Edge does not exist.  In that case,
     * you can call {@link AtlasElement#exists()} to determine whether the vertex
     * exists.  This allows the retrieval of the Edge information to be deferred
     * or in come cases avoided altogether in implementations where that might
     * be an expensive operation.
     *
     * @param edgeId
     * @return
     */
    AtlasEdge<V, E> getEdge(String edgeId);

    /**
     * Gets all the edges in the graph.
     * @return
     */
    Iterable<AtlasEdge<V, E>> getEdges();

    /**
     * Gets all the vertices in the graph.
     * @return
     */
    Iterable<AtlasVertex<V, E>> getVertices();

    /**
     * Gets the vertex with the specified id.  As an optimization, a non-null vertex may be
     * returned by some implementations if the Vertex does not exist.  In that case,
     * you can call {@link AtlasElement#exists()} to determine whether the vertex
     * exists.  This allows the retrieval of the Vertex information to be deferred
     * or in come cases avoided altogether in implementations where that might
     * be an expensive operation.
     *
     * @param vertexId
     * @return
     */
    AtlasVertex<V, E> getVertex(String vertexId);

    /**
     * Gets the names of the indexes on edges
     * type.
     *
     * @return
     */
    Set<String> getEdgeIndexKeys();


    /**
     * Gets the names of the indexes on vertices.
     * type.
     *
     * @return
     */
    Set<String> getVertexIndexKeys();


    /**
     * Finds the vertices where the given property key
     * has the specified value.  For multi-valued properties,
     * finds the vertices where the value list contains
     * the specified value.
     *
     * @param key
     * @param value
     * @return
     */
    Iterable<AtlasVertex<V, E>> getVertices(String key, Object value);

    /**
     * Creates a graph query.
     *
     * @return
     */
    AtlasGraphQuery<V, E> query();

    /**
     * Start a graph traversal
     * @return
     */
    AtlasGraphTraversal<AtlasVertex, AtlasEdge> V(Object ... vertexIds);

    AtlasGraphTraversal<AtlasVertex, AtlasEdge> E(Object ... edgeIds);

    /**
     * Creates an index query.
     *
     * @param indexName index name
     * @param queryString the query
     *
     * @see <a
     * href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html">
     * Elastic Search Reference</a> for query syntax
     */
    AtlasIndexQuery<V, E> indexQuery(String indexName, String queryString);

    /**
     * Creates an index query.
     *
     * @param indexName index name
     * @param queryString the query
     * @param offset specify the offset that should be applied for the query. This is useful for paging through
     *               list of results
     *
     * @see <a
     * href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html">
     * Elastic Search Reference</a> for query syntax
     */
    AtlasIndexQuery<V, E> indexQuery(String indexName, String queryString, int offset);

    /**
     * Creates an index query.
     *
     * @param indexQueryParameters the parameterObject containing the information needed for creating the index.
     *
     */
    AtlasIndexQuery<V, E> indexQuery(GraphIndexQueryParameters indexQueryParameters);

    /**
     * Gets the management object associated with this graph and opens a transaction
     * for changes that are made.
     * @return
     */
    AtlasGraphManagement getManagementSystem();

    /**
     * Commits changes made to the graph in the current transaction.
     */
    void commit();

    /**
     * Rolls back changes made to the graph in the current transaction.
     */
    void rollback();

    /**
     * Unloads and releases any resources associated with the graph.
     */
    void shutdown();

    /**
     * Deletes all data in the graph.  May or may not delete
     * the indices, depending on the what the underlying graph supports.
     *
     * For testing only.
     *
     */
    void clear();

    /**
     * Gets all open transactions.
     */
    Set getOpenTransactions();

    /**
     * Converts the graph to gson and writes it to the specified stream.
     *
     * @param os
     * @throws IOException
     */
    void exportToGson(OutputStream os) throws IOException;

    //the following methods insulate Atlas from the details
    //of the interaction with Gremlin

    /**
     * This method is used in the generation of queries.  It is used to
     * convert property values from the value that is stored in the graph
     * to the value/type that the user expects to get back.
     *
     * @param valueExpr - gremlin expr that represents the persistent property value
     * @param type
     * @return
     */
    GroovyExpression generatePersisentToLogicalConversionExpression(GroovyExpression valueExpr, AtlasType type);

    /**
     * Indicates whether or not stored values with the specified type need to be converted
     * within generated gremlin queries before they can be compared with literal values.
     * As an example, a graph database might choose to store Long values as Strings or
     * List values as a delimited list.  In this case, the generated gremlin needs to
     * convert the stored property value prior to comparing it a literal.  In this returns
     * true, @code{generatePersisentToLogicalConversionExpression} is used to generate a
     * gremlin expression with the converted value.  In addition, this cause the gremlin
     * 'filter' step to be used to compare the values instead of a 'has' step.
     */
    boolean isPropertyValueConversionNeeded(AtlasType type);

    /**
     * Gets the version of Gremlin that this graph uses.
     *
     * @return
     */
    GremlinVersion getSupportedGremlinVersion();

    /**
     * Whether or not an initial predicate needs to be added to gremlin queries
     * in order for them to run successfully.  This is needed for some graph database where
     * graph scans are disabled.
     * @return
     */
    boolean requiresInitialIndexedPredicate();

    /**
     * Some graph database backends have graph scans disabled.  In order to execute some queries there,
     * an initial 'dummy' predicate needs to be added to gremlin queries so that the first
     * condition uses an index.
     *
     * @return
     */
    GroovyExpression getInitialIndexedPredicate(GroovyExpression parent);

    /**
     * As an optimization, a graph database implementation may want to retrieve additional
     * information about the query results.  For example, in the IBM Graph implementation,
     * this changes the query to return both matching vertices and their outgoing edges to
     * avoid the need to make an extra REST API call to look up those edges.  For implementations
     * that do not require any kind of transform, an empty String should be returned.
     */
    GroovyExpression addOutputTransformationPredicate(GroovyExpression expr, boolean isSelect, boolean isPath);

    /**
     * Get an instance of the script engine to execute Gremlin queries
     *
     * @return script engine to execute Gremlin queries
     */
    ScriptEngine getGremlinScriptEngine() throws AtlasBaseException;

    /**
     * Release an instance of the script engine obtained with getGremlinScriptEngine()
     *
     * @param scriptEngine: ScriptEngine to release
     */
    void releaseGremlinScriptEngine(ScriptEngine scriptEngine);

    /**
     * Executes a Gremlin script, returns an object with the result.
     *
     * @param query
     * @param isPath whether this is a path query
     *
     * @return the result from executing the script
     *
     * @throws ScriptException
     */
    Object executeGremlinScript(String query, boolean isPath) throws AtlasBaseException;

    /**
     * Executes a Gremlin script using a ScriptEngineManager provided by consumer, returns an object with the result.
     * This is useful for scenarios where an operation executes large number of queries.
     *
     * @param scriptEngine: ScriptEngine initialized by consumer.
     * @param bindings: Update bindings with Graph instance for ScriptEngine that is initilized externally.
     * @param query
     * @param isPath whether this is a path query
     *
     * @return the result from executing the script
     *
     * @throws ScriptException
     */
    Object executeGremlinScript(ScriptEngine scriptEngine, Map<? extends  String, ? extends  Object> bindings, String query, boolean isPath) throws ScriptException;


    /**
     * Convenience method to check whether the given property is
     * a multi-property.
     *
     * @param name
     * @return
     */
    boolean isMultiProperty(String name);

    /**
     * Create Index query parameter for use with atlas graph.
     * @param parameterName the name of the parameter that needs to be passed to index layer.
     * @param parameterValue the value of the paratemeter that needs to be passed to the index layer.
     * @return
     */
    AtlasIndexQueryParameter indexQueryParameter(String parameterName, String parameterValue);

    /**
     * Implementors should return graph index client.
     * @return the graph index client
     * @throws AtlasException when error encountered in creating the client.
     */
    AtlasGraphIndexClient getGraphIndexClient()throws AtlasException;
}
