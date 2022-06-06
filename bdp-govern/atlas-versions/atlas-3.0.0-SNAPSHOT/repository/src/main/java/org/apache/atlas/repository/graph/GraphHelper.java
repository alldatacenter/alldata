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

package org.apache.atlas.repository.graph;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.AtlasException;
import org.apache.atlas.GraphTransactionInterceptor;
import org.apache.atlas.RequestContext;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.Status;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.AtlasRelationship;
import org.apache.atlas.repository.graphdb.AtlasVertexQuery;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.type.AtlasArrayType;
import org.apache.atlas.type.AtlasMapType;
import org.apache.atlas.utils.AtlasPerfMetrics;
import org.apache.atlas.v1.model.instance.Id;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.model.typedef.AtlasRelationshipDef.PropagateTags;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute.AtlasRelationshipEdgeDirection;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.RepositoryException;
import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasEdgeDirection;
import org.apache.atlas.repository.graphdb.AtlasElement;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasGraphQuery;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.exception.EntityNotFoundException;
import org.apache.atlas.util.AttributeValueMap;
import org.apache.atlas.util.IndexedInstance;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.apache.atlas.model.instance.AtlasEntity.Status.ACTIVE;
import static org.apache.atlas.model.instance.AtlasEntity.Status.DELETED;

import static org.apache.atlas.repository.Constants.*;
import static org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2.isReference;
import static org.apache.atlas.type.AtlasStructType.AtlasAttribute.AtlasRelationshipEdgeDirection.BOTH;
import static org.apache.atlas.type.AtlasStructType.AtlasAttribute.AtlasRelationshipEdgeDirection.IN;
import static org.apache.atlas.type.AtlasStructType.AtlasAttribute.AtlasRelationshipEdgeDirection.OUT;

/**
 * Utility class for graph operations.
 */
public final class GraphHelper {

    private static final Logger LOG = LoggerFactory.getLogger(GraphHelper.class);
    public static final String EDGE_LABEL_PREFIX = "__";

    public static final String RETRY_COUNT = "atlas.graph.storage.num.retries";
    public static final String RETRY_DELAY = "atlas.graph.storage.retry.sleeptime.ms";
    public static final String DEFAULT_REMOVE_PROPAGATIONS_ON_ENTITY_DELETE = "atlas.graph.remove.propagations.default";

    private AtlasGraph graph;

    private int     maxRetries = 3;
    private long    retrySleepTimeMillis = 1000;
    private boolean removePropagations = false;

    public GraphHelper(AtlasGraph graph) {
        this.graph = graph;
        try {
            maxRetries           = ApplicationProperties.get().getInt(RETRY_COUNT, 3);
            retrySleepTimeMillis = ApplicationProperties.get().getLong(RETRY_DELAY, 1000);
            removePropagations   = ApplicationProperties.get().getBoolean(DEFAULT_REMOVE_PROPAGATIONS_ON_ENTITY_DELETE, false);
        } catch (AtlasException e) {
            LOG.error("Could not load configuration. Setting to default value for " + RETRY_COUNT, e);
        }
    }

    public static boolean isTermEntityEdge(AtlasEdge edge) {
        return StringUtils.equals(edge.getLabel(), TERM_ASSIGNMENT_LABEL);
    }

    public AtlasEdge addClassificationEdge(AtlasVertex entityVertex, AtlasVertex classificationVertex, boolean isPropagated) {
        AtlasEdge ret = addEdge(entityVertex, classificationVertex, CLASSIFICATION_LABEL);

        if (ret != null) {
            AtlasGraphUtilsV2.setEncodedProperty(ret, CLASSIFICATION_EDGE_NAME_PROPERTY_KEY, getTypeName(classificationVertex));
            AtlasGraphUtilsV2.setEncodedProperty(ret, CLASSIFICATION_EDGE_IS_PROPAGATED_PROPERTY_KEY, isPropagated);
        }

        return ret;
    }

    public AtlasEdge addEdge(AtlasVertex fromVertex, AtlasVertex toVertex, String edgeLabel) {
        AtlasEdge ret;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding edge for {} -> label {} -> {}", string(fromVertex), edgeLabel, string(toVertex));
        }

        ret = graph.addEdge(fromVertex, toVertex, edgeLabel);

        if (ret != null) {
            AtlasGraphUtilsV2.setEncodedProperty(ret, STATE_PROPERTY_KEY, ACTIVE.name());
            AtlasGraphUtilsV2.setEncodedProperty(ret, TIMESTAMP_PROPERTY_KEY, RequestContext.get().getRequestTime());
            AtlasGraphUtilsV2.setEncodedProperty(ret, MODIFICATION_TIMESTAMP_PROPERTY_KEY, RequestContext.get().getRequestTime());
            AtlasGraphUtilsV2.setEncodedProperty(ret, CREATED_BY_KEY, RequestContext.get().getUser());
            AtlasGraphUtilsV2.setEncodedProperty(ret, MODIFIED_BY_KEY, RequestContext.get().getUser());

            if (LOG.isDebugEnabled()) {
                LOG.debug("Added {}", string(ret));
            }
        }

        return ret;
    }

    public AtlasEdge getOrCreateEdge(AtlasVertex outVertex, AtlasVertex inVertex, String edgeLabel) throws RepositoryException {
        AtlasPerfMetrics.MetricRecorder metric = RequestContext.get().startMetricRecord("getOrCreateEdge");

        for (int numRetries = 0; numRetries < maxRetries; numRetries++) {
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Running edge creation attempt {}", numRetries);
                }

                if (inVertex.hasEdges(AtlasEdgeDirection.IN, edgeLabel) && outVertex.hasEdges(AtlasEdgeDirection.OUT, edgeLabel)) {
                    AtlasEdge edge = graph.getEdgeBetweenVertices(outVertex, inVertex, edgeLabel);
                    if (edge != null) {
                        return edge;
                    }
                }

                return addEdge(outVertex, inVertex, edgeLabel);
            } catch (Exception e) {
                LOG.warn(String.format("Exception while trying to create edge from %s to %s with label %s. Retrying",
                        vertexString(outVertex), vertexString(inVertex), edgeLabel), e);
                if (numRetries == (maxRetries - 1)) {
                    LOG.error("Max retries exceeded for edge creation {} {} {} ", outVertex, inVertex, edgeLabel, e);
                    throw new RepositoryException("Edge creation failed after retries", e);
                }

                try {
                    LOG.info("Retrying with delay of {} ms ", retrySleepTimeMillis);
                    Thread.sleep(retrySleepTimeMillis);
                } catch(InterruptedException ie) {
                    LOG.warn("Retry interrupted during edge creation ");
                    throw new RepositoryException("Retry interrupted during edge creation", ie);
                }
            }
        }

        RequestContext.get().endMetricRecord(metric);
        return null;
    }

    public AtlasEdge getEdgeByEdgeId(AtlasVertex outVertex, String edgeLabel, String edgeId) {
        if (edgeId == null) {
            return null;
        }
        return graph.getEdge(edgeId);

        //TODO get edge id is expensive. Use this logic. But doesn't work for now
        /**
        Iterable<AtlasEdge> edges = outVertex.getEdges(Direction.OUT, edgeLabel);
        for (AtlasEdge edge : edges) {
            if (edge.getObjectId().toString().equals(edgeId)) {
                return edge;
            }
        }
        return null;
         **/
    }

    /**
     * Args of the format prop1, key1, prop2, key2...
     * Searches for a AtlasVertex with prop1=key1 && prop2=key2
     * @param args
     * @return AtlasVertex with the given property keys
     * @throws AtlasBaseException
     */
    public AtlasVertex findVertex(Object... args) throws EntityNotFoundException {
        return (AtlasVertex) findElement(true, args);
    }

    /**
     * Args of the format prop1, key1, prop2, key2...
     * Searches for a AtlasEdge with prop1=key1 && prop2=key2
     * @param args
     * @return AtlasEdge with the given property keys
     * @throws AtlasBaseException
     */
    public AtlasEdge findEdge(Object... args) throws EntityNotFoundException {
        return (AtlasEdge) findElement(false, args);
    }

    private AtlasElement findElement(boolean isVertexSearch, Object... args) throws EntityNotFoundException {
        AtlasGraphQuery query = graph.query();

        for (int i = 0; i < args.length; i += 2) {
            query = query.has((String) args[i], args[i + 1]);
        }

        Iterator<AtlasElement> results = isVertexSearch ? query.vertices().iterator() : query.edges().iterator();
        AtlasElement           element = (results != null && results.hasNext()) ? results.next() : null;

        if (element == null) {
            throw new EntityNotFoundException("Could not find " + (isVertexSearch ? "vertex" : "edge") + " with condition: " + getConditionString(args));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Found {} with condition {}", string(element), getConditionString(args));
        }

        return element;
    }

    //In some cases of parallel APIs, the edge is added, but get edge by label doesn't return the edge. ATLAS-1104
    //So traversing all the edges
    public static Iterator<AtlasEdge> getAdjacentEdgesByLabel(AtlasVertex instanceVertex, AtlasEdgeDirection direction, final String edgeLabel) {
        AtlasPerfMetrics.MetricRecorder metric = RequestContext.get().startMetricRecord("getAdjacentEdgesByLabel");
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finding edges for {} with label {}", string(instanceVertex), edgeLabel);
        }

        Iterator<AtlasEdge> ret = null;
        if(instanceVertex != null && edgeLabel != null) {
            ret = instanceVertex.getEdges(direction, edgeLabel).iterator();
        }

        RequestContext.get().endMetricRecord(metric);
        return ret;
    }

    public static long getAdjacentEdgesCountByLabel(AtlasVertex instanceVertex, AtlasEdgeDirection direction, final String edgeLabel) {
        AtlasPerfMetrics.MetricRecorder metric = RequestContext.get().startMetricRecord("getAdjacentEdgesCountByLabel");
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finding edges for {} with label {}", string(instanceVertex), edgeLabel);
        }

        long ret = 0;
        if(instanceVertex != null && edgeLabel != null) {
            ret = instanceVertex.getEdgesCount(direction, edgeLabel);
        }

        RequestContext.get().endMetricRecord(metric);
        return ret;
    }

    public static boolean isPropagationEnabled(AtlasVertex classificationVertex) {
        boolean ret = false;

        if (classificationVertex != null) {
            Boolean enabled = AtlasGraphUtilsV2.getEncodedProperty(classificationVertex, CLASSIFICATION_VERTEX_PROPAGATE_KEY, Boolean.class);

            ret = (enabled == null) ? true : enabled;
        }

        return ret;
    }

    public static boolean getRemovePropagations(AtlasVertex classificationVertex) {
        boolean ret = false;

        if (classificationVertex != null) {
            Boolean enabled = AtlasGraphUtilsV2.getEncodedProperty(classificationVertex, CLASSIFICATION_VERTEX_REMOVE_PROPAGATIONS_KEY, Boolean.class);

            ret = (enabled == null) ? true : enabled;
        }

        return ret;
    }

    public static AtlasVertex getClassificationVertex(AtlasVertex entityVertex, String classificationName) {
        AtlasVertex ret   = null;
        Iterable    edges = entityVertex.query().direction(AtlasEdgeDirection.OUT).label(CLASSIFICATION_LABEL)
                                                .has(CLASSIFICATION_EDGE_IS_PROPAGATED_PROPERTY_KEY, false)
                                                .has(CLASSIFICATION_EDGE_NAME_PROPERTY_KEY, classificationName).edges();
        if (edges != null) {
            Iterator<AtlasEdge> iterator = edges.iterator();

            if (iterator.hasNext()) {
                AtlasEdge edge = iterator.next();

                ret = (edge != null) ? edge.getInVertex() : null;
            }
        }

        return ret;
    }

    public static AtlasEdge getClassificationEdge(AtlasVertex entityVertex, AtlasVertex classificationVertex) {
        AtlasEdge ret   = null;
        Iterable  edges = entityVertex.query().direction(AtlasEdgeDirection.OUT).label(CLASSIFICATION_LABEL)
                                              .has(CLASSIFICATION_EDGE_IS_PROPAGATED_PROPERTY_KEY, false)
                                              .has(CLASSIFICATION_EDGE_NAME_PROPERTY_KEY, getTypeName(classificationVertex)).edges();
        if (edges != null) {
            Iterator<AtlasEdge> iterator = edges.iterator();

            if (iterator.hasNext()) {
                AtlasEdge edge = iterator.next();

                ret = (edge != null && edge.getInVertex().equals(classificationVertex)) ? edge : null;
            }
        }

        return ret;
    }

    public static AtlasEdge getPropagatedClassificationEdge(AtlasVertex entityVertex, String classificationName, String associatedEntityGuid) {
        AtlasEdge ret   = null;
        Iterable  edges = entityVertex.query().direction(AtlasEdgeDirection.OUT).label(CLASSIFICATION_LABEL)
                                              .has(CLASSIFICATION_EDGE_IS_PROPAGATED_PROPERTY_KEY, true)
                                              .has(CLASSIFICATION_EDGE_NAME_PROPERTY_KEY, classificationName).edges();
        if (edges != null) {
            Iterator<AtlasEdge> iterator = edges.iterator();

            while (iterator.hasNext()) {
                AtlasEdge   edge                 = iterator.next();
                AtlasVertex classificationVertex = (edge != null) ? edge.getInVertex() : null;

                if (classificationVertex != null) {
                    String guid = AtlasGraphUtilsV2.getEncodedProperty(classificationVertex, CLASSIFICATION_ENTITY_GUID, String.class);

                    if (StringUtils.equals(guid, associatedEntityGuid)) {
                        ret = edge;
                        break;
                    }
                }
            }
        }

        return ret;
    }

    public static AtlasEdge getPropagatedClassificationEdge(AtlasVertex entityVertex, AtlasVertex classificationVertex) {
        AtlasEdge ret   = null;
        Iterable  edges = entityVertex.query().direction(AtlasEdgeDirection.OUT).label(CLASSIFICATION_LABEL)
                                              .has(CLASSIFICATION_EDGE_IS_PROPAGATED_PROPERTY_KEY, true)
                                              .has(CLASSIFICATION_EDGE_NAME_PROPERTY_KEY, getTypeName(classificationVertex)).edges();

        if (edges != null && classificationVertex != null) {
            Iterator<AtlasEdge> iterator = edges.iterator();

            while (iterator != null && iterator.hasNext()) {
                AtlasEdge edge = iterator.next();

                if (edge != null && edge.getInVertex().equals(classificationVertex)) {
                    ret = edge;
                    break;
                }
            }
        }

        return ret;
    }

    public static List<AtlasEdge> getPropagatedEdges(AtlasVertex classificationVertex) {
        List<AtlasEdge> ret   = new ArrayList<>();
        Iterable        edges = classificationVertex.query().direction(AtlasEdgeDirection.IN).label(CLASSIFICATION_LABEL)
                                                    .has(CLASSIFICATION_EDGE_IS_PROPAGATED_PROPERTY_KEY, true)
                                                    .has(CLASSIFICATION_EDGE_NAME_PROPERTY_KEY, getTypeName(classificationVertex)).edges();
        if (edges != null) {
            Iterator<AtlasEdge> iterator = edges.iterator();

            while (iterator.hasNext()) {
                AtlasEdge edge = iterator.next();

                ret.add(edge);
            }
        }

        return ret;
    }

    public static boolean hasEntityReferences(AtlasVertex classificationVertex) {
        return classificationVertex.hasEdges(AtlasEdgeDirection.IN, CLASSIFICATION_LABEL);
    }

    public static List<AtlasVertex> getAllPropagatedEntityVertices(AtlasVertex classificationVertex) {
        List<AtlasVertex> ret = new ArrayList<>();

        if (classificationVertex != null) {
            List<AtlasEdge> edges = getPropagatedEdges(classificationVertex);

            if (CollectionUtils.isNotEmpty(edges)) {
                for (AtlasEdge edge : edges) {
                    ret.add(edge.getOutVertex());
                }
            }
        }

        return ret;
    }

    public static Iterator<AtlasEdge> getIncomingEdgesByLabel(AtlasVertex instanceVertex, String edgeLabel) {
        return getAdjacentEdgesByLabel(instanceVertex, AtlasEdgeDirection.IN, edgeLabel);
    }

    public static Iterator<AtlasEdge> getOutGoingEdgesByLabel(AtlasVertex instanceVertex, String edgeLabel) {
        return getAdjacentEdgesByLabel(instanceVertex, AtlasEdgeDirection.OUT, edgeLabel);
    }

    public static long getOutGoingEdgesCountByLabel(AtlasVertex instanceVertex, String edgeLabel) {
        return getAdjacentEdgesCountByLabel(instanceVertex, AtlasEdgeDirection.OUT, edgeLabel);
    }

    public static long getInComingEdgesCountByLabel(AtlasVertex instanceVertex, String edgeLabel) {
        return getAdjacentEdgesCountByLabel(instanceVertex, AtlasEdgeDirection.IN, edgeLabel);
    }

    public AtlasEdge getEdgeForLabel(AtlasVertex vertex, String edgeLabel, AtlasRelationshipEdgeDirection edgeDirection) {
        AtlasEdge ret;

        switch (edgeDirection) {
            case IN:
            ret = getEdgeForLabel(vertex, edgeLabel, AtlasEdgeDirection.IN);
            break;

            case OUT:
            ret = getEdgeForLabel(vertex, edgeLabel, AtlasEdgeDirection.OUT);
            break;

            case BOTH:
            default:
                ret = getEdgeForLabel(vertex, edgeLabel, AtlasEdgeDirection.BOTH);
                break;
        }

        return ret;
    }

    public static Iterator<AtlasEdge> getEdgesForLabel(AtlasVertex vertex, String edgeLabel, AtlasRelationshipEdgeDirection edgeDirection) {
        Iterator<AtlasEdge> ret = null;

        switch (edgeDirection) {
            case IN:
                ret = getIncomingEdgesByLabel(vertex, edgeLabel);
                break;

            case OUT:
            ret = getOutGoingEdgesByLabel(vertex, edgeLabel);
            break;

            case BOTH:
                ret = getAdjacentEdgesByLabel(vertex, AtlasEdgeDirection.BOTH, edgeLabel);
                break;
        }

        return ret;
    }

    /**
     * Returns the active edge for the given edge label.
     * If the vertex is deleted and there is no active edge, it returns the latest deleted edge
     * @param vertex
     * @param edgeLabel
     * @return
     */
    public AtlasEdge getEdgeForLabel(AtlasVertex vertex, String edgeLabel) {
        return getEdgeForLabel(vertex, edgeLabel, AtlasEdgeDirection.OUT);
    }

    public AtlasEdge getEdgeForLabel(AtlasVertex vertex, String edgeLabel, AtlasEdgeDirection edgeDirection) {
        Iterator<AtlasEdge> iterator = getAdjacentEdgesByLabel(vertex, edgeDirection, edgeLabel);
        AtlasEdge latestDeletedEdge = null;
        long latestDeletedEdgeTime = Long.MIN_VALUE;

        while (iterator != null && iterator.hasNext()) {
            AtlasEdge edge = iterator.next();
            Id.EntityState edgeState = getState(edge);
            if (edgeState == null || edgeState == Id.EntityState.ACTIVE) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Found {}", string(edge));
                }

                return edge;
            } else {
                Long modificationTime = edge.getProperty(MODIFICATION_TIMESTAMP_PROPERTY_KEY, Long.class);
                if (modificationTime != null && modificationTime >= latestDeletedEdgeTime) {
                    latestDeletedEdgeTime = modificationTime;
                    latestDeletedEdge = edge;
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Found {}", latestDeletedEdge == null ? "null" : string(latestDeletedEdge));
        }

        //If the vertex is deleted, return latest deleted edge
        return latestDeletedEdge;
    }

    public static String vertexString(final AtlasVertex vertex) {
        StringBuilder properties = new StringBuilder();
        for (String propertyKey : vertex.getPropertyKeys()) {
            Collection<?> propertyValues = vertex.getPropertyValues(propertyKey, Object.class);
            properties.append(propertyKey).append("=").append(propertyValues.toString()).append(", ");
        }

        return "v[" + vertex.getIdForDisplay() + "], Properties[" + properties + "]";
    }

    public static String edgeString(final AtlasEdge edge) {
        return "e[" + edge.getLabel() + "], [" + edge.getOutVertex() + " -> " + edge.getLabel() + " -> "
                + edge.getInVertex() + "]";
    }

    private static <T extends AtlasElement> String string(T element) {
        if (element instanceof AtlasVertex) {
            return string((AtlasVertex) element);
        } else if (element instanceof AtlasEdge) {
            return string((AtlasEdge)element);
        }
        return element.toString();
    }

    /**
     * Remove the specified edge from the graph.
     *
     * @param edge
     */
    public void removeEdge(AtlasEdge edge) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> removeEdge({})", string(edge));
        }

        graph.removeEdge(edge);

        if (LOG.isDebugEnabled()) {
            LOG.info("<== removeEdge()");
        }
    }

    /**
     * Remove the specified AtlasVertex from the graph.
     *
     * @param vertex
     */
    public void removeVertex(AtlasVertex vertex) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> GraphHelper.removeVertex({})", string(vertex));
        }

        graph.removeVertex(vertex);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== GraphHelper.removeVertex()");
        }
    }

    public AtlasVertex getVertexForGUID(String guid) throws EntityNotFoundException {
        return findVertex(Constants.GUID_PROPERTY_KEY, guid);
    }

    public AtlasEdge getEdgeForGUID(String guid) throws AtlasBaseException {
        AtlasEdge ret;

        try {
            ret = findEdge(Constants.RELATIONSHIP_GUID_PROPERTY_KEY, guid);
        } catch (EntityNotFoundException e) {
            throw new AtlasBaseException(AtlasErrorCode.RELATIONSHIP_GUID_NOT_FOUND, guid);
        }

        return ret;
    }

    /**
     * Finds the Vertices that correspond to the given property values.  Property
     * values that are not found in the graph will not be in the map.
     *
     *  @return propertyValue to AtlasVertex map with the result.
     */
    public Map<String, AtlasVertex> getVerticesForPropertyValues(String property, List<String> values) {

        if(values.isEmpty()) {
            return Collections.emptyMap();
        }
        Collection<String> nonNullValues = new HashSet<>(values.size());

        for(String value : values) {
            if(value != null) {
                nonNullValues.add(value);
            }
        }

        //create graph query that finds vertices with the guids
        AtlasGraphQuery query = graph.query();
        query.in(property, nonNullValues);
        Iterable<AtlasVertex> results = query.vertices();

        Map<String, AtlasVertex> result = new HashMap<>(values.size());
        //Process the result, using the guidToIndexMap to figure out where
        //each vertex should go in the result list.
        for(AtlasVertex vertex : results) {
            if(vertex.exists()) {
                String propertyValue = vertex.getProperty(property, String.class);
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Found a vertex {} with {} =  {}", string(vertex), property, propertyValue);
                }
                result.put(propertyValue, vertex);
            }
        }
        return result;
    }

    /**
     * Finds the Vertices that correspond to the given GUIDs.  GUIDs
     * that are not found in the graph will not be in the map.
     *
     *  @return GUID to AtlasVertex map with the result.
     */
    public Map<String, AtlasVertex> getVerticesForGUIDs(List<String> guids) {

        return getVerticesForPropertyValues(Constants.GUID_PROPERTY_KEY, guids);
    }

    public static void updateModificationMetadata(AtlasVertex vertex) {
        AtlasGraphUtilsV2.setEncodedProperty(vertex, MODIFICATION_TIMESTAMP_PROPERTY_KEY, RequestContext.get().getRequestTime());
        AtlasGraphUtilsV2.setEncodedProperty(vertex, MODIFIED_BY_KEY, RequestContext.get().getUser());
    }

    public static String getQualifiedNameForMapKey(String prefix, String key) {
        return prefix + "." + key;
    }

    public static String getTraitLabel(String typeName, String attrName) {
        return attrName;
    }

    public static String getTraitLabel(String traitName) {
        return traitName;
    }

    public static List<String> getTraitNames(AtlasVertex entityVertex) {
        return getTraitNames(entityVertex, false);
    }

    public static List<String> getPropagatedTraitNames(AtlasVertex entityVertex) {
        return getTraitNames(entityVertex, true);
    }

    public static List<String> getAllTraitNames(AtlasVertex entityVertex) {
        return getTraitNames(entityVertex, null);
    }

    public static List<String> getTraitNames(AtlasVertex entityVertex, Boolean propagated) {
        List<String>     ret   = new ArrayList<>();
        AtlasVertexQuery query = entityVertex.query().direction(AtlasEdgeDirection.OUT).label(CLASSIFICATION_LABEL);

        if (propagated != null) {
            query = query.has(CLASSIFICATION_EDGE_IS_PROPAGATED_PROPERTY_KEY, propagated);
        }

        Iterable edges = query.edges();

        if (edges != null) {
            Iterator<AtlasEdge> iterator = edges.iterator();

            while (iterator.hasNext()) {
                AtlasEdge edge = iterator.next();

                ret.add(AtlasGraphUtilsV2.getEncodedProperty(edge, CLASSIFICATION_EDGE_NAME_PROPERTY_KEY, String.class));
            }
        }

        return ret;
    }

    public static List<AtlasVertex> getPropagatableClassifications(AtlasEdge edge) {
        List<AtlasVertex> ret = new ArrayList<>();

        if (edge != null && getStatus(edge) != DELETED) {
            PropagateTags propagateTags = getPropagateTags(edge);
            AtlasVertex   outVertex     = edge.getOutVertex();
            AtlasVertex   inVertex      = edge.getInVertex();

            if (propagateTags == PropagateTags.ONE_TO_TWO || propagateTags == PropagateTags.BOTH) {
                ret.addAll(getPropagationEnabledClassificationVertices(outVertex));
            }

            if (propagateTags == PropagateTags.TWO_TO_ONE || propagateTags == PropagateTags.BOTH) {
                ret.addAll(getPropagationEnabledClassificationVertices(inVertex));
            }
        }

        return ret;
    }

    public static List<AtlasVertex> getPropagationEnabledClassificationVertices(AtlasVertex entityVertex) {
        List<AtlasVertex> ret   = new ArrayList<>();
        if (entityVertex.hasEdges(AtlasEdgeDirection.OUT, CLASSIFICATION_LABEL)) {
            Iterable edges = entityVertex.query().direction(AtlasEdgeDirection.OUT).label(CLASSIFICATION_LABEL).edges();

            if (edges != null) {
                Iterator<AtlasEdge> iterator = edges.iterator();

                while (iterator.hasNext()) {
                    AtlasEdge edge = iterator.next();

                    if (edge != null) {
                        AtlasVertex classificationVertex = edge.getInVertex();

                        if (isPropagationEnabled(classificationVertex)) {
                            ret.add(classificationVertex);
                        }
                    }
                }
            }
        }

        return ret;
    }

    public static List<AtlasEdge> getClassificationEdges(AtlasVertex entityVertex) {
        return getClassificationEdges(entityVertex, false);
    }

    public static List<AtlasEdge> getPropagatedClassificationEdges(AtlasVertex entityVertex) {
        return getClassificationEdges(entityVertex, true);
    }

    public static List<AtlasEdge> getAllClassificationEdges(AtlasVertex entityVertex) {
        return getClassificationEdges(entityVertex, null);
    }

    public static List<AtlasEdge> getClassificationEdges(AtlasVertex entityVertex, Boolean propagated) {
        List<AtlasEdge>  ret   = new ArrayList<>();
        AtlasVertexQuery query = entityVertex.query().direction(AtlasEdgeDirection.OUT).label(CLASSIFICATION_LABEL);

        if (propagated != null) {
            query = query.has(CLASSIFICATION_EDGE_IS_PROPAGATED_PROPERTY_KEY, propagated);
        }

        Iterable edges = query.edges();

        if (edges != null) {
            Iterator<AtlasEdge> iterator = edges.iterator();

            while (iterator.hasNext()) {
                AtlasEdge edge = iterator.next();

                if (edge != null) {
                    ret.add(edge);
                }
            }
        }

        return ret;
    }

    public static List<String> getSuperTypeNames(AtlasVertex<?,?> entityVertex) {
        ArrayList<String>  superTypes     = new ArrayList<>();
        Collection<String> propertyValues = entityVertex.getPropertyValues(SUPER_TYPES_PROPERTY_KEY, String.class);

        if (CollectionUtils.isNotEmpty(propertyValues)) {
            for(String value : propertyValues) {
                superTypes.add(value);
            }
        }

        return superTypes;
    }

    public static String getEdgeLabel(AtlasAttribute aInfo) throws AtlasException {
        return aInfo.getRelationshipEdgeLabel();
    }

    public static Id getIdFromVertex(String dataTypeName, AtlasVertex vertex) {
        return new Id(getGuid(vertex),
                getVersion(vertex).intValue(), dataTypeName,
                getStateAsString(vertex));
    }

    public static Id getIdFromVertex(AtlasVertex vertex) {
        return getIdFromVertex(getTypeName(vertex), vertex);
    }

    public static String getRelationshipGuid(AtlasElement element) {
        return element.getProperty(Constants.RELATIONSHIP_GUID_PROPERTY_KEY, String.class);
    }

    public static String getGuid(AtlasVertex vertex) {
        Object vertexId = vertex.getId();
        String ret = GraphTransactionInterceptor.getVertexGuidFromCache(vertexId);

        if (ret == null) {
            ret = vertex.<String>getProperty(Constants.GUID_PROPERTY_KEY, String.class);

            GraphTransactionInterceptor.addToVertexGuidCache(vertexId, ret);
        }

        return ret;
    }

    public static String getHomeId(AtlasElement element) {
        return element.getProperty(Constants.HOME_ID_KEY, String.class);
    }

    public static Boolean isProxy(AtlasElement element) {
        return element.getProperty(Constants.IS_PROXY_KEY, Boolean.class);
    }

    public static Boolean isEntityIncomplete(AtlasElement element) {
        Integer value = element.getProperty(Constants.IS_INCOMPLETE_PROPERTY_KEY, Integer.class);
        Boolean ret   = value != null && value.equals(INCOMPLETE_ENTITY_VALUE) ? Boolean.TRUE : Boolean.FALSE;

        return ret;
    }

    public static Map getCustomAttributes(AtlasElement element) {
        Map    ret               = null;
        String customAttrsString = element.getProperty(CUSTOM_ATTRIBUTES_PROPERTY_KEY, String.class);

        if (customAttrsString != null) {
            ret = AtlasType.fromJson(customAttrsString, Map.class);
        }

        return ret;
    }

    public static Set<String> getLabels(AtlasElement element) {
        return parseLabelsString(element.getProperty(LABELS_PROPERTY_KEY, String.class));
    }

    public static Integer getProvenanceType(AtlasElement element) {
        return element.getProperty(Constants.PROVENANCE_TYPE_KEY, Integer.class);
    }

    public static String getTypeName(AtlasElement element) {
        return element.getProperty(ENTITY_TYPE_PROPERTY_KEY, String.class);
    }

    public static Id.EntityState getState(AtlasElement element) {
        String state = getStateAsString(element);
        return state == null ? null : Id.EntityState.valueOf(state);
    }

    public static Long getVersion(AtlasElement element) {
        return element.getProperty(Constants.VERSION_PROPERTY_KEY, Long.class);
    }

    public static String getStateAsString(AtlasElement element) {
        return element.getProperty(STATE_PROPERTY_KEY, String.class);
    }

    public static Status getStatus(AtlasVertex vertex) {
        Object vertexId = vertex.getId();
        Status ret = GraphTransactionInterceptor.getVertexStateFromCache(vertexId);

        if (ret == null) {
            ret = (getState(vertex) == Id.EntityState.DELETED) ? Status.DELETED : Status.ACTIVE;

            GraphTransactionInterceptor.addToVertexStateCache(vertexId, ret);
        }

        return ret;
    }

    public static Status getStatus(AtlasEdge edge) {
        Object edgeId = edge.getId();
        Status ret = GraphTransactionInterceptor.getEdgeStateFromCache(edgeId);

        if (ret == null) {
            ret = (getState(edge) == Id.EntityState.DELETED) ? Status.DELETED : Status.ACTIVE;

            GraphTransactionInterceptor.addToEdgeStateCache(edgeId, ret);
        }

        return ret;
    }


    public static AtlasRelationship.Status getEdgeStatus(AtlasElement element) {
        return (getState(element) == Id.EntityState.DELETED) ? AtlasRelationship.Status.DELETED : AtlasRelationship.Status.ACTIVE;
    }

    public static String getClassificationName(AtlasVertex classificationVertex) {
        return AtlasGraphUtilsV2.getEncodedProperty(classificationVertex, CLASSIFICATION_VERTEX_NAME_KEY, String.class);
    }

    public static String getClassificationEntityGuid(AtlasVertex classificationVertex) {
        return AtlasGraphUtilsV2.getEncodedProperty(classificationVertex, CLASSIFICATION_ENTITY_GUID, String.class);
    }

    public static Integer getIndexValue(AtlasEdge edge) {
        Integer index = edge.getProperty(ATTRIBUTE_INDEX_PROPERTY_KEY, Integer.class);
        return (index == null) ? 0: index;
    }

    public static boolean isPropagatedClassificationEdge(AtlasEdge edge) {
        boolean ret = false;

        if (edge != null) {
            Boolean isPropagated = edge.getProperty(Constants.CLASSIFICATION_EDGE_IS_PROPAGATED_PROPERTY_KEY, Boolean.class);

            if (isPropagated != null) {
                ret = isPropagated.booleanValue();
            }
        }

        return ret;
    }

    public static boolean isClassificationEdge(AtlasEdge edge) {
        boolean ret = false;

        if (edge != null) {
            String  edgeLabel    = edge.getLabel();
            Boolean isPropagated = edge.getProperty(Constants.CLASSIFICATION_EDGE_IS_PROPAGATED_PROPERTY_KEY, Boolean.class);

            if (edgeLabel != null && isPropagated != null) {
                ret = edgeLabel.equals(CLASSIFICATION_LABEL) && !isPropagated;
            }
        }

        return ret;
    }

    public static List<String> getBlockedClassificationIds(AtlasEdge edge) {
        List<String> ret = null;

        if (edge != null) {
            List<String> classificationIds = AtlasGraphUtilsV2.getEncodedProperty(edge, RELATIONSHIPTYPE_BLOCKED_PROPAGATED_CLASSIFICATIONS_KEY, List.class);

            ret = CollectionUtils.isNotEmpty(classificationIds) ? classificationIds : Collections.emptyList();
        }

        return ret;
    }

    public static PropagateTags getPropagateTags(AtlasElement element) {
        String propagateTags = element.getProperty(RELATIONSHIPTYPE_TAG_PROPAGATION_KEY, String.class);

        return (propagateTags == null) ? null : PropagateTags.valueOf(propagateTags);
    }

    public static Status getClassificationEntityStatus(AtlasElement element) {
        String status = element.getProperty(CLASSIFICATION_ENTITY_STATUS, String.class);

        return (status == null) ? null : Status.valueOf(status);
    }

    //Added conditions in fetching system attributes to handle test failures in GremlinTest where these properties are not set
    public static String getCreatedByAsString(AtlasElement element){
        return element.getProperty(CREATED_BY_KEY, String.class);
    }

    public static String getModifiedByAsString(AtlasElement element){
        return element.getProperty(MODIFIED_BY_KEY, String.class);
    }

    public static long getCreatedTime(AtlasElement element){
        return element.getProperty(TIMESTAMP_PROPERTY_KEY, Long.class);
    }

    public static long getModifiedTime(AtlasElement element){
        return element.getProperty(MODIFICATION_TIMESTAMP_PROPERTY_KEY, Long.class);
    }

    public static boolean isActive(AtlasEntity entity) {
        return entity != null ? entity.getStatus() == ACTIVE : false;
    }

    /**
     * For the given type, finds an unique attribute and checks if there is an existing instance with the same
     * unique value
     *
     * @param classType
     * @param instance
     * @return
     * @throws AtlasException
     */
    public AtlasVertex getVertexForInstanceByUniqueAttribute(AtlasEntityType classType, Referenceable instance)
        throws AtlasException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Checking if there is an instance with the same unique attributes for instance {}", instance.toShortString());
        }

        AtlasVertex result = null;
        for (AtlasAttribute attributeInfo : classType.getUniqAttributes().values()) {
            String propertyKey = attributeInfo.getQualifiedName();
            try {
                result = findVertex(propertyKey, instance.get(attributeInfo.getName()),
                        ENTITY_TYPE_PROPERTY_KEY, classType.getTypeName(),
                        STATE_PROPERTY_KEY, Id.EntityState.ACTIVE.name());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Found vertex by unique attribute : {}={}", propertyKey, instance.get(attributeInfo.getName()));
                }
            } catch (EntityNotFoundException e) {
                //Its ok if there is no entity with the same unique value
            }
        }

        return result;
    }

    /**
     * Finds vertices that match at least one unique attribute of the instances specified.  The AtlasVertex at a given index in the result corresponds
     * to the IReferencableInstance at that same index that was passed in.  The number of elements in the resultant list is guaranteed to match the
     * number of instances that were passed in.  If no vertex is found for a given instance, that entry will be null in the resultant list.
     *
     *
     * @param classType
     * @param instancesForClass
     * @return
     * @throws AtlasException
     */
    public List<AtlasVertex> getVerticesForInstancesByUniqueAttribute(AtlasEntityType classType, List<? extends Referenceable> instancesForClass) throws AtlasException {

        //For each attribute, need to figure out what values to search for and which instance(s)
        //those values correspond to.
        Map<String, AttributeValueMap> map = new HashMap<String, AttributeValueMap>();

        for (AtlasAttribute attributeInfo : classType.getUniqAttributes().values()) {
            String propertyKey = attributeInfo.getQualifiedName();
            AttributeValueMap mapForAttribute = new AttributeValueMap();
            for(int idx = 0; idx < instancesForClass.size(); idx++) {
                Referenceable instance = instancesForClass.get(idx);
                Object value = instance.get(attributeInfo.getName());
                mapForAttribute.put(value, instance, idx);
            }
            map.put(propertyKey, mapForAttribute);
        }

        AtlasVertex[] result = new AtlasVertex[instancesForClass.size()];
        if(map.isEmpty()) {
            //no unique attributes
            return Arrays.asList(result);
        }

        //construct gremlin query
        AtlasGraphQuery query = graph.query();

        query.has(ENTITY_TYPE_PROPERTY_KEY, classType.getTypeName());
        query.has(STATE_PROPERTY_KEY,Id.EntityState.ACTIVE.name());

        List<AtlasGraphQuery> orChildren = new ArrayList<AtlasGraphQuery>();


        //build up an or expression to find vertices which match at least one of the unique attribute constraints
        //For each unique attributes, we add a within clause to match vertices that have a value of that attribute
        //that matches the value in some instance.
        for(Map.Entry<String, AttributeValueMap> entry : map.entrySet()) {
            AtlasGraphQuery orChild = query.createChildQuery();
            String propertyName = entry.getKey();
            AttributeValueMap valueMap = entry.getValue();
            Set<Object> values = valueMap.getAttributeValues();
            if(values.size() == 1) {
                orChild.has(propertyName, values.iterator().next());
            }
            else if(values.size() > 1) {
                orChild.in(propertyName, values);
            }
            orChildren.add(orChild);
        }

        if(orChildren.size() == 1) {
            AtlasGraphQuery child = orChildren.get(0);
            query.addConditionsFrom(child);
        }
        else if(orChildren.size() > 1) {
            query.or(orChildren);
        }

        Iterable<AtlasVertex> queryResult = query.vertices();


        for(AtlasVertex matchingVertex : queryResult) {
            Collection<IndexedInstance> matches = getInstancesForVertex(map, matchingVertex);
            for(IndexedInstance wrapper : matches) {
                result[wrapper.getIndex()]= matchingVertex;
            }
        }
        return Arrays.asList(result);
    }

    //finds the instance(s) that correspond to the given vertex
    private Collection<IndexedInstance> getInstancesForVertex(Map<String, AttributeValueMap> map, AtlasVertex foundVertex) {

        //loop through the unique attributes.  For each attribute, check to see if the vertex property that
        //corresponds to that attribute has a value from one or more of the instances that were passed in.

        for(Map.Entry<String, AttributeValueMap> entry : map.entrySet()) {

            String propertyName = entry.getKey();
            AttributeValueMap valueMap = entry.getValue();

            Object vertexValue = foundVertex.getProperty(propertyName, Object.class);

            Collection<IndexedInstance> instances = valueMap.get(vertexValue);
            if(instances != null && instances.size() > 0) {
                //return first match.  Let the underling graph determine if this is a problem
                //(i.e. if the other unique attributes change be changed safely to match what
                //the user requested).
                return instances;
            }
            //try another attribute
        }
        return Collections.emptyList();
    }

    public static List<String> getTypeNames(List<AtlasVertex> vertices) {
        List<String> ret = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(vertices)) {
            for (AtlasVertex vertex : vertices) {
                String entityTypeProperty = vertex.getProperty(ENTITY_TYPE_PROPERTY_KEY, String.class);

                if (entityTypeProperty != null) {
                    ret.add(getTypeName(vertex));
                }
            }
        }

        return ret;
    }

    public static AtlasVertex getAssociatedEntityVertex(AtlasVertex classificationVertex) {
        AtlasVertex ret   = null;
        Iterable    edges = classificationVertex.query().direction(AtlasEdgeDirection.IN).label(CLASSIFICATION_LABEL)
                                                .has(CLASSIFICATION_EDGE_IS_PROPAGATED_PROPERTY_KEY, false)
                                                .has(CLASSIFICATION_EDGE_NAME_PROPERTY_KEY, getTypeName(classificationVertex)).edges();
        if (edges != null) {
            Iterator<AtlasEdge> iterator = edges.iterator();

            if (iterator != null && iterator.hasNext()) {
                AtlasEdge edge = iterator.next();

                ret = edge.getOutVertex();
            }
        }

        return ret;
    }

    public AtlasGraph getGraph() {
        return this.graph;
    }

    public Boolean getDefaultRemovePropagations() {
        return this.removePropagations;
    }

    /**
     * Guid and AtlasVertex combo
     */
    public static class VertexInfo {
        private final AtlasEntityHeader entity;
        private final AtlasVertex       vertex;

        public VertexInfo(AtlasEntityHeader entity, AtlasVertex vertex) {
            this.entity = entity;
            this.vertex = vertex;
        }

        public AtlasEntityHeader getEntity() { return entity; }
        public AtlasVertex getVertex() {
            return vertex;
        }
        public String getGuid() {
            return entity.getGuid();
        }
        public String getTypeName() {
            return entity.getTypeName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VertexInfo that = (VertexInfo) o;
            return Objects.equals(entity, that.entity) &&
                    Objects.equals(vertex, that.vertex);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entity, vertex);
        }
    }

    /*
     /**
     * Get the GUIDs and vertices for all composite entities owned/contained by the specified root entity AtlasVertex.
     * The graph is traversed from the root entity through to the leaf nodes of the containment graph.
     *
     * @param entityVertex the root entity vertex
     * @return set of VertexInfo for all composite entities
     * @throws AtlasException
     */
    /*
    public Set<VertexInfo> getCompositeVertices(AtlasVertex entityVertex) throws AtlasException {
        Set<VertexInfo> result = new HashSet<>();
        Stack<AtlasVertex> vertices = new Stack<>();
        vertices.push(entityVertex);
        while (vertices.size() > 0) {
            AtlasVertex vertex = vertices.pop();
            String typeName = GraphHelper.getTypeName(vertex);
            String guid = GraphHelper.getGuid(vertex);
            Id.EntityState state = GraphHelper.getState(vertex);
            if (state == Id.EntityState.DELETED) {
                //If the reference vertex is marked for deletion, skip it
                continue;
            }
            result.add(new VertexInfo(guid, vertex, typeName));
            ClassType classType = typeSystem.getDataType(ClassType.class, typeName);
            for (AttributeInfo attributeInfo : classType.fieldMapping().fields.values()) {
                if (!attributeInfo.isComposite) {
                    continue;
                }
                String edgeLabel = GraphHelper.getEdgeLabel(classType, attributeInfo);
                switch (attributeInfo.dataType().getTypeCategory()) {
                    case CLASS:
                        AtlasEdge edge = getEdgeForLabel(vertex, edgeLabel);
                        if (edge != null && GraphHelper.getState(edge) == Id.EntityState.ACTIVE) {
                            AtlasVertex compositeVertex = edge.getInVertex();
                            vertices.push(compositeVertex);
                        }
                        break;
                    case ARRAY:
                        IDataType elementType = ((DataTypes.ArrayType) attributeInfo.dataType()).getElemType();
                        DataTypes.TypeCategory elementTypeCategory = elementType.getTypeCategory();
                        if (elementTypeCategory != TypeCategory.CLASS) {
                            continue;
                        }
                        Iterator<AtlasEdge> edges = getOutGoingEdgesByLabel(vertex, edgeLabel);
                        if (edges != null) {
                            while (edges.hasNext()) {
                                edge = edges.next();
                                if (edge != null && GraphHelper.getState(edge) == Id.EntityState.ACTIVE) {
                                    AtlasVertex compositeVertex = edge.getInVertex();
                                    vertices.push(compositeVertex);
                                }
                            }
                        }
                        break;
                    case MAP:
                        DataTypes.MapType mapType = (DataTypes.MapType) attributeInfo.dataType();
                        DataTypes.TypeCategory valueTypeCategory = mapType.getValueType().getTypeCategory();
                        if (valueTypeCategory != TypeCategory.CLASS) {
                            continue;
                        }
                        String propertyName = GraphHelper.getQualifiedFieldName(classType, attributeInfo.name);
                        List<String> keys = vertex.getProperty(propertyName, List.class);
                        if (keys != null) {
                            for (String key : keys) {
                                String mapEdgeLabel = GraphHelper.getQualifiedNameForMapKey(edgeLabel, key);
                                edge = getEdgeForLabel(vertex, mapEdgeLabel);
                                if (edge != null && GraphHelper.getState(edge) == Id.EntityState.ACTIVE) {
                                    AtlasVertex compositeVertex = edge.getInVertex();
                                    vertices.push(compositeVertex);
                                }
                            }
                        }
                        break;
                    default:
                }
            }
        }
        return result;
    }
    */

    /*
    public static Referenceable[] deserializeClassInstances(AtlasTypeRegistry typeRegistry, String entityInstanceDefinition)
    throws AtlasException {
        try {
            JSONArray referableInstances = new JSONArray(entityInstanceDefinition);
            Referenceable[] instances = new Referenceable[referableInstances.length()];
            for (int index = 0; index < referableInstances.length(); index++) {
                Referenceable entityInstance =
                        AtlasType.fromV1Json(referableInstances.getString(index), Referenceable.class);
                Referenceable typedInstrance = getTypedReferenceableInstance(typeRegistry, entityInstance);
                instances[index] = typedInstrance;
            }
            return instances;
        } catch(TypeNotFoundException  e) {
            throw e;
        } catch (Exception e) {  // exception from deserializer
            LOG.error("Unable to deserialize json={}", entityInstanceDefinition, e);
            throw new IllegalArgumentException("Unable to deserialize json", e);
        }
    }

    public static Referenceable getTypedReferenceableInstance(AtlasTypeRegistry typeRegistry, Referenceable entityInstance)
            throws AtlasException {
        final String entityTypeName = ParamChecker.notEmpty(entityInstance.getTypeName(), "Entity type cannot be null");

        AtlasEntityType entityType = typeRegistry.getEntityTypeByName(entityTypeName);

        //Both assigned id and values are required for full update
        //classtype.convert() will remove values if id is assigned. So, set temp id, convert and
        // then replace with original id
        Id origId = entityInstance.getId();
        entityInstance.setId(new Id(entityInstance.getTypeName()));
        Referenceable typedInstrance = new Referenceable(entityInstance);
        typedInstrance.setId(origId);
        return typedInstrance;
    }
    */

    public static boolean isInternalType(AtlasVertex vertex) {
        return vertex != null && isInternalType(getTypeName(vertex));
    }

    public static boolean isInternalType(String typeName) {
        return typeName != null && typeName.startsWith(Constants.INTERNAL_PROPERTY_KEY_PREFIX);
    }

    public static List<Object> getArrayElementsProperty(AtlasType elementType, AtlasVertex instanceVertex, AtlasAttribute attribute) {
        String propertyName = attribute.getVertexPropertyName();

        if (isReference(elementType)) {
            return (List) getCollectionElementsUsingRelationship(instanceVertex, attribute);
        } else {
            return (List) instanceVertex.getListProperty(propertyName);
        }
    }

    public static Map<String, Object> getMapElementsProperty(AtlasMapType mapType, AtlasVertex instanceVertex, String propertyName, AtlasAttribute attribute) {
        AtlasType mapValueType = mapType.getValueType();

        if (isReference(mapValueType)) {
            return getReferenceMap(instanceVertex, attribute);
        } else {
            return (Map) instanceVertex.getProperty(propertyName, Map.class);
        }
    }

    // map elements for reference types - AtlasObjectId, AtlasStruct
    public static Map<String, Object> getReferenceMap(AtlasVertex instanceVertex, AtlasAttribute attribute) {
        Map<String, Object> ret            = new HashMap<>();
        List<AtlasEdge>     referenceEdges = getCollectionElementsUsingRelationship(instanceVertex, attribute);

        for (AtlasEdge edge : referenceEdges) {
            String key = edge.getProperty(ATTRIBUTE_KEY_PROPERTY_KEY, String.class);

            if (StringUtils.isNotEmpty(key)) {
                ret.put(key, edge);
            }
        }

        return ret;
    }

    public static List<AtlasEdge> getMapValuesUsingRelationship(AtlasVertex vertex, AtlasAttribute attribute) {
        String                         edgeLabel     = attribute.getRelationshipEdgeLabel();
        AtlasRelationshipEdgeDirection edgeDirection = attribute.getRelationshipEdgeDirection();
        Iterator<AtlasEdge>            edgesForLabel = getEdgesForLabel(vertex, edgeLabel, edgeDirection);

        return (List<AtlasEdge>) IteratorUtils.toList(edgesForLabel);
    }

    // map elements for primitive types
    public static Map<String, Object> getPrimitiveMap(AtlasVertex instanceVertex, String propertyName) {
        Map<String, Object> ret = instanceVertex.getProperty(AtlasGraphUtilsV2.encodePropertyKey(propertyName), Map.class);

        return ret;
    }

    public static List<AtlasEdge> getCollectionElementsUsingRelationship(AtlasVertex vertex, AtlasAttribute attribute) {
        List<AtlasEdge>                ret;
        String                         edgeLabel     = attribute.getRelationshipEdgeLabel();
        AtlasRelationshipEdgeDirection edgeDirection = attribute.getRelationshipEdgeDirection();
        Iterator<AtlasEdge>            edgesForLabel = getEdgesForLabel(vertex, edgeLabel, edgeDirection);

        ret = IteratorUtils.toList(edgesForLabel);

        sortCollectionElements(attribute, ret);

        return ret;
    }

    private static void sortCollectionElements(AtlasAttribute attribute, List<AtlasEdge> edges) {
        // sort array elements based on edge index
        if (attribute.getAttributeType() instanceof AtlasArrayType &&
                CollectionUtils.isNotEmpty(edges) &&
                edges.get(0).getProperty(ATTRIBUTE_INDEX_PROPERTY_KEY, Integer.class) != null) {
            Collections.sort(edges, (e1, e2) -> {
                Integer e1Index = getIndexValue(e1);
                Integer e2Index = getIndexValue(e2);

                return e1Index.compareTo(e2Index);
            });
        }
    }

    public static void dumpToLog(final AtlasGraph<?,?> graph) {
        LOG.debug("*******************Graph Dump****************************");
        LOG.debug("Vertices of {}", graph);
        for (AtlasVertex vertex : graph.getVertices()) {
            LOG.debug(vertexString(vertex));
        }

        LOG.debug("Edges of {}", graph);
        for (AtlasEdge edge : graph.getEdges()) {
            LOG.debug(edgeString(edge));
        }
        LOG.debug("*******************Graph Dump****************************");
    }

    public static String string(Referenceable instance) {
        return String.format("entity[type=%s guid=%s]", instance.getTypeName(), instance.getId()._getId());
    }

    public static String string(AtlasVertex<?,?> vertex) {
        if(vertex == null) {
            return "vertex[null]";
        } else {
            if (LOG.isDebugEnabled()) {
                return getVertexDetails(vertex);
            } else {
                return String.format("vertex[id=%s]", vertex.getIdForDisplay());
            }
        }
    }

    public static String getVertexDetails(AtlasVertex<?,?> vertex) {

        return String.format("vertex[id=%s type=%s guid=%s]", vertex.getIdForDisplay(), getTypeName(vertex),
                getGuid(vertex));
    }


    public static String string(AtlasEdge<?,?> edge) {
        if(edge == null) {
            return "edge[null]";
        } else {
            if (LOG.isDebugEnabled()) {
                return getEdgeDetails(edge);
            } else {
                return String.format("edge[id=%s]", edge.getIdForDisplay());
            }
        }
    }

    public static String getEdgeDetails(AtlasEdge<?,?> edge) {

        return String.format("edge[id=%s label=%s from %s -> to %s]", edge.getIdForDisplay(), edge.getLabel(),
                string(edge.getOutVertex()), string(edge.getInVertex()));
    }

    @VisibleForTesting
    //Keys copied from com.thinkaurelius.titan.graphdb.types.StandardRelationTypeMaker
    //Titan checks that these chars are not part of any keys. So, encoding...
    public static BiMap<String, String> RESERVED_CHARS_ENCODE_MAP =
            HashBiMap.create(new HashMap<String, String>() {{
                put("{", "_o");
                put("}", "_c");
                put("\"", "_q");
                put("$", "_d");
                put("%", "_p");
            }});


    public static String decodePropertyKey(String key) {
        if (StringUtils.isBlank(key)) {
            return key;
        }

        for (String encodedStr : RESERVED_CHARS_ENCODE_MAP.values()) {
            key = key.replace(encodedStr, RESERVED_CHARS_ENCODE_MAP.inverse().get(encodedStr));
        }
        return key;
    }

    public Object getVertexId(String guid) throws EntityNotFoundException {
        AtlasVertex instanceVertex   = getVertexForGUID(guid);
        Object      instanceVertexId = instanceVertex.getId();

        return instanceVertexId;
    }

    /*
    public static AttributeInfo getAttributeInfoForSystemAttributes(String field) {
        switch (field) {
        case Constants.STATE_PROPERTY_KEY:
        case Constants.GUID_PROPERTY_KEY:
        case Constants.CREATED_BY_KEY:
        case Constants.MODIFIED_BY_KEY:
                return TypesUtil.newAttributeInfo(field, DataTypes.STRING_TYPE);

        case Constants.TIMESTAMP_PROPERTY_KEY:
        case Constants.MODIFICATION_TIMESTAMP_PROPERTY_KEY:
            return TypesUtil.newAttributeInfo(field, DataTypes.DATE_TYPE);
        }
        return null;
    }
    */

    public static boolean elementExists(AtlasElement v) {
        return v != null && v.exists();
    }

    public static void setListPropertyFromElementIds(AtlasVertex<?, ?> instanceVertex, String propertyName,
            List<AtlasElement> elements) {
        String actualPropertyName = AtlasGraphUtilsV2.encodePropertyKey(propertyName);
        instanceVertex.setPropertyFromElementsIds(actualPropertyName, elements);

    }

    public static void setPropertyFromElementId(AtlasVertex<?, ?> instanceVertex, String propertyName,
            AtlasElement value) {
        String actualPropertyName = AtlasGraphUtilsV2.encodePropertyKey(propertyName);
        instanceVertex.setPropertyFromElementId(actualPropertyName, value);

    }

    public static void setListProperty(AtlasVertex instanceVertex, String propertyName, ArrayList<String> value) {
        String actualPropertyName = AtlasGraphUtilsV2.encodePropertyKey(propertyName);
        instanceVertex.setListProperty(actualPropertyName, value);
    }

    public static List<String> getListProperty(AtlasVertex instanceVertex, String propertyName) {
        String actualPropertyName = AtlasGraphUtilsV2.encodePropertyKey(propertyName);
        return instanceVertex.getListProperty(actualPropertyName);
    }


    private String getConditionString(Object[] args) {
        StringBuilder condition = new StringBuilder();

        for (int i = 0; i < args.length; i+=2) {
            condition.append(args[i]).append(" = ").append(args[i+1]).append(", ");
        }

        return condition.toString();
    }

    /**
     * Get relationshipDef name from entityType using relationship attribute.
     * if more than one relationDefs are returned for an attribute.
     * e.g. hive_column.table
     *
     * hive_table.columns       -> hive_column.table
     * hive_table.partitionKeys -> hive_column.table
     *
     * resolve by comparing all incoming edges typename with relationDefs name returned for an attribute
     * to pick the right relationshipDef name
     */
    public String getRelationshipTypeName(AtlasVertex entityVertex, AtlasEntityType entityType, String attributeName) {
        String      ret               = null;
        Set<String> relationshipTypes = entityType.getAttributeRelationshipTypes(attributeName);

        if (CollectionUtils.isNotEmpty(relationshipTypes)) {
            if (relationshipTypes.size() == 1) {
                ret = relationshipTypes.iterator().next();
            } else {
                Iterator<AtlasEdge> iter = entityVertex.getEdges(AtlasEdgeDirection.IN).iterator();

                while (iter.hasNext() && ret == null) {
                    String edgeTypeName = AtlasGraphUtilsV2.getTypeName(iter.next());

                    if (relationshipTypes.contains(edgeTypeName)) {
                        ret = edgeTypeName;

                        break;
                    }
                }

                if (ret == null) {
                    //relationshipTypes will have at least one relationshipDef
                    ret = relationshipTypes.iterator().next();
                }
            }
        }

        return ret;
    }

    //get entity type of relationship (End vertex entity type) from relationship label
    public static String getReferencedEntityTypeName(AtlasVertex entityVertex, String relation) {
        String ret = null;
        Iterator<AtlasEdge> edges    = GraphHelper.getAdjacentEdgesByLabel(entityVertex, AtlasEdgeDirection.BOTH, relation);

        if (edges != null && edges.hasNext()) {
            AtlasEdge   relationEdge = edges.next();
            AtlasVertex outVertex    = relationEdge.getOutVertex();
            AtlasVertex inVertex     = relationEdge.getInVertex();

            if (outVertex != null && inVertex != null) {
                String outVertexId    = outVertex.getIdForDisplay();
                String entityVertexId = entityVertex.getIdForDisplay();
                AtlasVertex endVertex = StringUtils.equals(outVertexId, entityVertexId) ? inVertex : outVertex;
                ret                   = GraphHelper.getTypeName(endVertex);
            }
        }

       return ret;
    }

    public static boolean isRelationshipEdge(AtlasEdge edge) {
        if (edge == null) {
            return false;
        }

        String edgeLabel = edge.getLabel();

        return StringUtils.isNotEmpty(edge.getLabel()) ? edgeLabel.startsWith("r:") : false;
    }

    public static AtlasObjectId getReferenceObjectId(AtlasEdge edge, AtlasRelationshipEdgeDirection relationshipDirection,
                                                     AtlasVertex parentVertex) {
        AtlasObjectId ret = null;

        if (relationshipDirection == OUT) {
            ret = getAtlasObjectIdForInVertex(edge);
        } else if (relationshipDirection == IN) {
            ret = getAtlasObjectIdForOutVertex(edge);
        } else if (relationshipDirection == BOTH){
            // since relationship direction is BOTH, edge direction can be inward or outward
            // compare with parent entity vertex and pick the right reference vertex
            if (verticesEquals(parentVertex, edge.getOutVertex())) {
                ret = getAtlasObjectIdForInVertex(edge);
            } else {
                ret = getAtlasObjectIdForOutVertex(edge);
            }
        }

        return ret;
    }

    public static AtlasObjectId getAtlasObjectIdForOutVertex(AtlasEdge edge) {
        return new AtlasObjectId(getGuid(edge.getOutVertex()), getTypeName(edge.getOutVertex()));
    }

    public static AtlasObjectId getAtlasObjectIdForInVertex(AtlasEdge edge) {
        return new AtlasObjectId(getGuid(edge.getInVertex()), getTypeName(edge.getInVertex()));
    }

    private static boolean verticesEquals(AtlasVertex vertexA, AtlasVertex vertexB) {
        return StringUtils.equals(getGuid(vertexB), getGuid(vertexA));
    }

    public static String getDelimitedClassificationNames(Collection<String> classificationNames) {
        String ret = null;

        if (CollectionUtils.isNotEmpty(classificationNames)) {
            ret = CLASSIFICATION_NAME_DELIMITER + StringUtils.join(classificationNames, CLASSIFICATION_NAME_DELIMITER)
                + CLASSIFICATION_NAME_DELIMITER;
        }
        return ret;
    }

    private static Set<String> parseLabelsString(String labels) {
        Set<String> ret = new HashSet<>();

        if (StringUtils.isNotEmpty(labels)) {
            ret.addAll(Arrays.asList(StringUtils.split(labels, "\\" + LABEL_NAME_DELIMITER)));
        }

        return ret;
    }
}