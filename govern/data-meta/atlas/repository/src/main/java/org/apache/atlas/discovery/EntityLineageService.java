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


import org.apache.atlas.AtlasConfiguration;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.annotation.GraphTransaction;
import org.apache.atlas.authorize.AtlasAuthorizationUtils;
import org.apache.atlas.authorize.AtlasEntityAccessRequest;
import org.apache.atlas.authorize.AtlasPrivilege;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.lineage.AtlasLineageInfo;
import org.apache.atlas.model.lineage.AtlasLineageInfo.LineageInfoOnDemand;
import org.apache.atlas.model.lineage.AtlasLineageInfo.LineageDirection;
import org.apache.atlas.model.lineage.AtlasLineageInfo.LineageRelation;
import org.apache.atlas.model.lineage.LineageOnDemandConstraints;
import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasEdgeDirection;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.repository.store.graph.v2.EntityGraphRetriever;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.atlas.util.AtlasGremlinQueryProvider;
import org.apache.atlas.v1.model.lineage.SchemaResponse.SchemaDetails;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.atlas.AtlasClient.DATA_SET_SUPER_TYPE;
import static org.apache.atlas.AtlasClient.PROCESS_SUPER_TYPE;
import static org.apache.atlas.AtlasErrorCode.INSTANCE_LINEAGE_QUERY_FAILED;
import static org.apache.atlas.model.lineage.AtlasLineageInfo.LineageDirection.BOTH;
import static org.apache.atlas.model.lineage.AtlasLineageInfo.LineageDirection.INPUT;
import static org.apache.atlas.model.lineage.AtlasLineageInfo.LineageDirection.OUTPUT;
import static org.apache.atlas.repository.Constants.RELATIONSHIP_GUID_PROPERTY_KEY;
import static org.apache.atlas.repository.graphdb.AtlasEdgeDirection.IN;
import static org.apache.atlas.repository.graphdb.AtlasEdgeDirection.OUT;
import static org.apache.atlas.util.AtlasGremlinQueryProvider.AtlasGremlinQuery.FULL_LINEAGE_DATASET;
import static org.apache.atlas.util.AtlasGremlinQueryProvider.AtlasGremlinQuery.FULL_LINEAGE_PROCESS;
import static org.apache.atlas.util.AtlasGremlinQueryProvider.AtlasGremlinQuery.PARTIAL_LINEAGE_DATASET;
import static org.apache.atlas.util.AtlasGremlinQueryProvider.AtlasGremlinQuery.PARTIAL_LINEAGE_PROCESS;

@Service
public class EntityLineageService implements AtlasLineageService {
    private static final Logger LOG = LoggerFactory.getLogger(EntityLineageService.class);

    private static final String  PROCESS_INPUTS_EDGE                  = "__Process.inputs";
    private static final String  PROCESS_OUTPUTS_EDGE                 = "__Process.outputs";
    private static final String  COLUMNS                              = "columns";
    private static final boolean LINEAGE_USING_GREMLIN                = AtlasConfiguration.LINEAGE_USING_GREMLIN.getBoolean();
    private static final Integer DEFAULT_LINEAGE_MAX_NODE_COUNT       = 9000;
    private static final int     LINEAGE_ON_DEMAND_DEFAULT_DEPTH      = 3;
    private static final int     LINEAGE_ON_DEMAND_DEFAULT_NODE_COUNT = AtlasConfiguration.LINEAGE_ON_DEMAND_DEFAULT_NODE_COUNT.getInt();
    private static final String  SEPARATOR                            = "->";

    private final AtlasGraph                graph;
    private final AtlasGremlinQueryProvider gremlinQueryProvider;
    private final EntityGraphRetriever      entityRetriever;
    private final AtlasTypeRegistry         atlasTypeRegistry;

    @Inject
    EntityLineageService(AtlasTypeRegistry typeRegistry, AtlasGraph atlasGraph) {
        this.graph = atlasGraph;
        this.gremlinQueryProvider = AtlasGremlinQueryProvider.INSTANCE;
        this.entityRetriever = new EntityGraphRetriever(atlasGraph, typeRegistry);
        this.atlasTypeRegistry = typeRegistry;
    }

    @Override
    @GraphTransaction
    public AtlasLineageInfo getAtlasLineageInfo(String guid, LineageDirection direction, int depth) throws AtlasBaseException {
        AtlasLineageInfo ret;

        boolean isDataSet = validateEntityTypeAndCheckIfDataSet(guid);

        if (LINEAGE_USING_GREMLIN) {
            ret = getLineageInfoV1(guid, direction, depth, isDataSet);
        } else {
            ret = getLineageInfoV2(guid, direction, depth, isDataSet);
        }

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasLineageInfo getAtlasLineageInfo(String guid, Map<String, LineageOnDemandConstraints> lineageConstraintsMap) throws AtlasBaseException {
        AtlasLineageInfo ret;

        if (MapUtils.isEmpty(lineageConstraintsMap)) {
            lineageConstraintsMap = new HashMap<>();
            lineageConstraintsMap.put(guid, getDefaultLineageConstraints(guid));
        }

        boolean isDataSet = validateEntityTypeAndCheckIfDataSet(guid);

        ret = getLineageInfoOnDemand(guid, lineageConstraintsMap, isDataSet);

        appendLineageOnDemandPayload(ret, lineageConstraintsMap);

        // filtering out on-demand relations which has input & output nodes within the limit
        cleanupRelationsOnDemand(ret);

        return ret;
    }

    private boolean validateEntityTypeAndCheckIfDataSet(String guid) throws AtlasBaseException {
        AtlasEntityHeader entity = entityRetriever.toAtlasEntityHeaderWithClassifications(guid);

        AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(atlasTypeRegistry, AtlasPrivilege.ENTITY_READ, entity), "read entity lineage: guid=", guid);

        AtlasEntityType entityType = atlasTypeRegistry.getEntityTypeByName(entity.getTypeName());

        if (entityType == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_NOT_FOUND, entity.getTypeName());
        }

        boolean isDataSet = entityType.getTypeAndAllSuperTypes().contains(DATA_SET_SUPER_TYPE);

        if (!isDataSet) {
            boolean isProcess = entityType.getTypeAndAllSuperTypes().contains(PROCESS_SUPER_TYPE);

            if (!isProcess) {
                throw new AtlasBaseException(AtlasErrorCode.INVALID_LINEAGE_ENTITY_TYPE, guid, entity.getTypeName());
            }
        }

        return isDataSet;
    }

    private void appendLineageOnDemandPayload(AtlasLineageInfo lineageInfo, Map<String, LineageOnDemandConstraints> lineageConstraintsMap) {
        if (lineageInfo == null || MapUtils.isEmpty(lineageConstraintsMap)) {
            return;
        }
        lineageInfo.setLineageOnDemandPayload(lineageConstraintsMap);
    }

    //Consider only relationsOnDemand which has either more inputs or more outputs than given limit
    private void cleanupRelationsOnDemand(AtlasLineageInfo lineageInfo) {
        if (lineageInfo != null && MapUtils.isNotEmpty(lineageInfo.getRelationsOnDemand())) {
            lineageInfo.getRelationsOnDemand().entrySet().removeIf(x -> !(x.getValue().hasMoreInputs() || x.getValue().hasMoreOutputs()));
        }
    }

    @Override
    @GraphTransaction
    public SchemaDetails getSchemaForHiveTableByName(final String datasetName) throws AtlasBaseException {
        if (StringUtils.isEmpty(datasetName)) {
            // TODO: Complete error handling here
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST);
        }

        AtlasEntityType hive_table = atlasTypeRegistry.getEntityTypeByName("hive_table");

        Map<String, Object> lookupAttributes = new HashMap<>();
        lookupAttributes.put("qualifiedName", datasetName);
        String guid = AtlasGraphUtilsV2.getGuidByUniqueAttributes(hive_table, lookupAttributes);

        return getSchemaForHiveTableByGuid(guid);
    }

    @Override
    @GraphTransaction
    public SchemaDetails getSchemaForHiveTableByGuid(final String guid) throws AtlasBaseException {
        if (StringUtils.isEmpty(guid)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST);
        }
        SchemaDetails ret = new SchemaDetails();
        AtlasEntityType hive_column = atlasTypeRegistry.getEntityTypeByName("hive_column");

        ret.setDataType(AtlasTypeUtil.toClassTypeDefinition(hive_column));

        AtlasEntityWithExtInfo entityWithExtInfo = entityRetriever.toAtlasEntityWithExtInfo(guid);
        AtlasEntity            entity            = entityWithExtInfo.getEntity();

        AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(atlasTypeRegistry, AtlasPrivilege.ENTITY_READ, new AtlasEntityHeader(entity)),
                                             "read entity schema: guid=", guid);

        Map<String, AtlasEntity> referredEntities = entityWithExtInfo.getReferredEntities();
        List<String>             columnIds        = getColumnIds(entity);

        if (MapUtils.isNotEmpty(referredEntities)) {
            List<Map<String, Object>> rows = referredEntities.entrySet()
                                                             .stream()
                                                             .filter(e -> isColumn(columnIds, e))
                                                             .map(e -> AtlasTypeUtil.toMap(e.getValue()))
                                                             .collect(Collectors.toList());
            ret.setRows(rows);
        }

        return ret;
    }

    private List<String> getColumnIds(AtlasEntity entity) {
        List<String> ret        = new ArrayList<>();
        Object       columnObjs = entity.getAttribute(COLUMNS);

        if (columnObjs instanceof List) {
            for (Object pkObj : (List) columnObjs) {
                if (pkObj instanceof AtlasObjectId) {
                    ret.add(((AtlasObjectId) pkObj).getGuid());
                }
            }
        }

        return ret;
    }

    private boolean isColumn(List<String> columnIds, Map.Entry<String, AtlasEntity> e) {
        return columnIds.contains(e.getValue().getGuid());
    }

    private AtlasLineageInfo getLineageInfoV1(String guid, LineageDirection direction, int depth, boolean isDataSet) throws AtlasBaseException {
        AtlasLineageInfo ret;

        if (direction.equals(INPUT)) {
            ret = getLineageInfo(guid, INPUT, depth, isDataSet);
        } else if (direction.equals(OUTPUT)) {
            ret = getLineageInfo(guid, OUTPUT, depth, isDataSet);
        } else {
            ret = getBothLineageInfoV1(guid, depth, isDataSet);
        }

        return ret;
    }

    private AtlasLineageInfo getLineageInfo(String guid, LineageDirection direction, int depth, boolean isDataSet) throws AtlasBaseException {
        final Map<String, Object>      bindings     = new HashMap<>();
        String                         lineageQuery = getLineageQuery(guid, direction, depth, isDataSet, bindings);
        List                           results      = executeGremlinScript(bindings, lineageQuery);
        Map<String, AtlasEntityHeader> entities     = new HashMap<>();
        Set<LineageRelation>           relations    = new HashSet<>();

        if (CollectionUtils.isNotEmpty(results)) {
            for (Object result : results) {
                if (result instanceof Map) {
                    for (final Object o : ((Map) result).entrySet()) {
                        final Map.Entry entry = (Map.Entry) o;
                        Object          value = entry.getValue();

                        if (value instanceof List) {
                            for (Object elem : (List) value) {
                                if (elem instanceof AtlasEdge) {
                                    processEdge((AtlasEdge) elem, entities, relations);
                                } else {
                                    LOG.warn("Invalid value of type {} found, ignoring", (elem != null ? elem.getClass().getSimpleName() : "null"));
                                }
                            }
                        } else if (value instanceof AtlasEdge) {
                            processEdge((AtlasEdge) value, entities, relations);
                        } else {
                            LOG.warn("Invalid value of type {} found, ignoring", (value != null ? value.getClass().getSimpleName() : "null"));
                        }
                    }
                } else if (result instanceof AtlasEdge) {
                    processEdge((AtlasEdge) result, entities, relations);
                }
            }
        }

        return new AtlasLineageInfo(guid, entities, relations, direction, depth);
    }

    private AtlasLineageInfo getLineageInfoV2(String guid, LineageDirection direction, int depth, boolean isDataSet) throws AtlasBaseException {
        AtlasLineageInfo ret = initializeLineageInfo(guid, direction, depth);

        if (depth == 0) {
            depth = -1;
        }

        if (isDataSet) {
            AtlasVertex datasetVertex = AtlasGraphUtilsV2.findByGuid(this.graph, guid);

            if (direction == INPUT || direction == BOTH) {
                traverseEdges(datasetVertex, true, depth, ret);
            }

            if (direction == OUTPUT || direction == BOTH) {
                traverseEdges(datasetVertex, false, depth, ret);
            }
        } else  {
            AtlasVertex processVertex = AtlasGraphUtilsV2.findByGuid(this.graph, guid);

            // make one hop to the next dataset vertices from process vertex and traverse with 'depth = depth - 1'
            if (direction == INPUT || direction == BOTH) {
                Iterable<AtlasEdge> processEdges = processVertex.getEdges(AtlasEdgeDirection.OUT, PROCESS_INPUTS_EDGE);

                for (AtlasEdge processEdge : processEdges) {
                    addEdgeToResult(processEdge, ret);

                    AtlasVertex datasetVertex = processEdge.getInVertex();

                    traverseEdges(datasetVertex, true, depth - 1, ret);
                }
            }

            if (direction == OUTPUT || direction == BOTH) {
                Iterable<AtlasEdge> processEdges = processVertex.getEdges(AtlasEdgeDirection.OUT, PROCESS_OUTPUTS_EDGE);

                for (AtlasEdge processEdge : processEdges) {
                    addEdgeToResult(processEdge, ret);

                    AtlasVertex datasetVertex = processEdge.getInVertex();

                    traverseEdges(datasetVertex, false, depth - 1, ret);
                }
            }
        }

        return ret;
    }

    private LineageOnDemandConstraints getDefaultLineageConstraints(String guid) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("No lineage on-demand constraints provided for guid: {}, configuring with default values direction: {}, inputRelationsLimit: {}, outputRelationsLimit: {}, depth: {}",
                    guid, BOTH, LINEAGE_ON_DEMAND_DEFAULT_NODE_COUNT, LINEAGE_ON_DEMAND_DEFAULT_NODE_COUNT, LINEAGE_ON_DEMAND_DEFAULT_DEPTH);
        }

        return new LineageOnDemandConstraints();
    }

    private LineageOnDemandConstraints getAndValidateLineageConstraintsByGuid(String guid, Map<String, LineageOnDemandConstraints> lineageConstraintsMap) {

        if (lineageConstraintsMap == null || !lineageConstraintsMap.containsKey(guid)) {
            return getDefaultLineageConstraints(guid);
        }

        LineageOnDemandConstraints lineageConstraintsByGuid = lineageConstraintsMap.get(guid);;
        if (lineageConstraintsByGuid == null) {
            return getDefaultLineageConstraints(guid);
        }

        if (Objects.isNull(lineageConstraintsByGuid.getDirection())) {
            LOG.info("No lineage on-demand direction provided for guid: {}, configuring with default value {}", guid, LineageDirection.BOTH);
            lineageConstraintsByGuid.setDirection(BOTH);
        }

        if (lineageConstraintsByGuid.getInputRelationsLimit() == 0) {
            LOG.info("No lineage on-demand inputRelationsLimit provided for guid: {}, configuring with default value {}", guid, LINEAGE_ON_DEMAND_DEFAULT_NODE_COUNT);
            lineageConstraintsByGuid.setInputRelationsLimit(LINEAGE_ON_DEMAND_DEFAULT_NODE_COUNT);
        }

        if (lineageConstraintsByGuid.getOutputRelationsLimit() == 0) {
            LOG.info("No lineage on-demand outputRelationsLimit provided for guid: {}, configuring with default value {}", guid, LINEAGE_ON_DEMAND_DEFAULT_NODE_COUNT);
            lineageConstraintsByGuid.setOutputRelationsLimit(LINEAGE_ON_DEMAND_DEFAULT_NODE_COUNT);
        }

        if (lineageConstraintsByGuid.getDepth() == 0) {
            LOG.info("No lineage on-demand depth provided for guid: {}, configuring with default value {}", guid, LINEAGE_ON_DEMAND_DEFAULT_DEPTH);
            lineageConstraintsByGuid.setDepth(LINEAGE_ON_DEMAND_DEFAULT_DEPTH);
        }

        return lineageConstraintsByGuid;

    }

    private AtlasLineageInfo getLineageInfoOnDemand(String guid, Map<String, LineageOnDemandConstraints> lineageConstraintsMap, boolean isDataSet) throws AtlasBaseException {

        LineageOnDemandConstraints lineageConstraintsByGuid = getAndValidateLineageConstraintsByGuid(guid, lineageConstraintsMap);

        if (lineageConstraintsByGuid == null) {
            throw new AtlasBaseException(AtlasErrorCode.NO_LINEAGE_CONSTRAINTS_FOR_GUID, guid);
        }

        LineageDirection direction = lineageConstraintsByGuid.getDirection();
        int              depth     = lineageConstraintsByGuid.getDepth();

        AtlasLineageInfo ret = initializeLineageInfo(guid, direction, depth);

        if (depth == 0) {
            depth = -1;
        }

        if (isDataSet) {
            AtlasVertex datasetVertex = AtlasGraphUtilsV2.findByGuid(this.graph, guid);

            if (!ret.getRelationsOnDemand().containsKey(guid)) {
                ret.getRelationsOnDemand().put(guid, new LineageInfoOnDemand(lineageConstraintsByGuid));
            }

            if (direction == INPUT || direction == BOTH) {
                traverseEdgesOnDemand(datasetVertex, true, depth, new HashSet<>(), lineageConstraintsMap, ret);
            }

            if (direction == OUTPUT || direction == BOTH) {
                traverseEdgesOnDemand(datasetVertex, false, depth, new HashSet<>(), lineageConstraintsMap, ret);
            }
        } else  {
            AtlasVertex processVertex = AtlasGraphUtilsV2.findByGuid(this.graph, guid);

            // make one hop to the next dataset vertices from process vertex and traverse with 'depth = depth - 1'
            if (direction == INPUT || direction == BOTH) {
                Iterable<AtlasEdge> processEdges = processVertex.getEdges(AtlasEdgeDirection.OUT, PROCESS_INPUTS_EDGE);

                traverseEdgesOnDemand(processEdges, true, depth, lineageConstraintsMap, ret);
            }

            if (direction == OUTPUT || direction == BOTH) {
                Iterable<AtlasEdge> processEdges = processVertex.getEdges(AtlasEdgeDirection.OUT, PROCESS_OUTPUTS_EDGE);

                traverseEdgesOnDemand(processEdges, false, depth, lineageConstraintsMap, ret);
            }

        }

        return ret;
    }

    private void traverseEdges(AtlasVertex datasetVertex, boolean isInput, int depth, AtlasLineageInfo ret) throws AtlasBaseException {
        traverseEdges(datasetVertex, isInput, depth, new HashSet<>(), ret);
    }

    private void traverseEdges(AtlasVertex datasetVertex, boolean isInput, int depth, Set<String> visitedVertices, AtlasLineageInfo ret) throws AtlasBaseException {
        if (depth != 0) {
            // keep track of visited vertices to avoid circular loop
            visitedVertices.add(getId(datasetVertex));

            Iterable<AtlasEdge> incomingEdges = datasetVertex.getEdges(IN, isInput ? PROCESS_OUTPUTS_EDGE : PROCESS_INPUTS_EDGE);

            for (AtlasEdge incomingEdge : incomingEdges) {
                AtlasVertex         processVertex = incomingEdge.getOutVertex();
                Iterable<AtlasEdge> outgoingEdges = processVertex.getEdges(OUT, isInput ? PROCESS_INPUTS_EDGE : PROCESS_OUTPUTS_EDGE);

                for (AtlasEdge outgoingEdge : outgoingEdges) {
                    AtlasVertex entityVertex = outgoingEdge.getInVertex();

                    if (entityVertex != null) {
                        addEdgeToResult(incomingEdge, ret);
                        addEdgeToResult(outgoingEdge, ret);

                        if (!visitedVertices.contains(getId(entityVertex))) {
                            traverseEdges(entityVertex, isInput, depth - 1, visitedVertices, ret);
                        }
                    }
                }
            }
        }
    }

    private void traverseEdgesOnDemand(Iterable<AtlasEdge> processEdges, boolean isInput, int depth, Map<String, LineageOnDemandConstraints> lineageConstraintsMap, AtlasLineageInfo ret) throws AtlasBaseException {
        for (AtlasEdge processEdge : processEdges) {
            boolean isInputEdge  = processEdge.getLabel().equalsIgnoreCase(PROCESS_INPUTS_EDGE);

            if (incrementAndCheckIfRelationsLimitReached(processEdge, isInputEdge, lineageConstraintsMap, ret)) {
                break;
            } else {
                addEdgeToResult(processEdge, ret);
            }

            AtlasVertex datasetVertex = processEdge.getInVertex();

            String inGuid = AtlasGraphUtilsV2.getIdFromVertex(datasetVertex);
            LineageOnDemandConstraints inGuidLineageConstrains = getAndValidateLineageConstraintsByGuid(inGuid, lineageConstraintsMap);

            if (!ret.getRelationsOnDemand().containsKey(inGuid)) {
                ret.getRelationsOnDemand().put(inGuid, new LineageInfoOnDemand(inGuidLineageConstrains));
            }

            traverseEdgesOnDemand(datasetVertex, isInput, depth - 1, new HashSet<>(), lineageConstraintsMap, ret);
        }
    }

    private void traverseEdgesOnDemand(AtlasVertex datasetVertex, boolean isInput, int depth, Set<String> visitedVertices, Map<String, LineageOnDemandConstraints> lineageConstraintsMap, AtlasLineageInfo ret) throws AtlasBaseException {
        if (depth != 0) {
            // keep track of visited vertices to avoid circular loop
            visitedVertices.add(getId(datasetVertex));

            Iterable<AtlasEdge> incomingEdges = datasetVertex.getEdges(IN, isInput ? PROCESS_OUTPUTS_EDGE : PROCESS_INPUTS_EDGE);

            for (AtlasEdge incomingEdge : incomingEdges) {

                if (incrementAndCheckIfRelationsLimitReached(incomingEdge, !isInput, lineageConstraintsMap, ret)) {
                    break;
                } else {
                    addEdgeToResult(incomingEdge, ret);
                }

                AtlasVertex         processVertex = incomingEdge.getOutVertex();
                Iterable<AtlasEdge> outgoingEdges = processVertex.getEdges(OUT, isInput ? PROCESS_INPUTS_EDGE : PROCESS_OUTPUTS_EDGE);

                for (AtlasEdge outgoingEdge : outgoingEdges) {

                    if (incrementAndCheckIfRelationsLimitReached(outgoingEdge, isInput, lineageConstraintsMap, ret)) {
                        break;
                    } else {
                        addEdgeToResult(outgoingEdge, ret);
                    }

                    AtlasVertex entityVertex = outgoingEdge.getInVertex();

                    if (entityVertex != null && !visitedVertices.contains(getId(entityVertex))) {
                        traverseEdgesOnDemand(entityVertex, isInput, depth - 1, visitedVertices, lineageConstraintsMap, ret);
                    }
                }
            }
        }
    }

    private boolean incrementAndCheckIfRelationsLimitReached(AtlasEdge atlasEdge, boolean isInput, Map<String, LineageOnDemandConstraints> lineageConstraintsMap, AtlasLineageInfo ret) {

        if (lineageContainsEdge(ret, atlasEdge)) {
            return false;
        }

        boolean hasRelationsLimitReached = false;

        AtlasVertex                inVertex                 = isInput ? atlasEdge.getOutVertex() : atlasEdge.getInVertex();
        String                     inGuid                   = AtlasGraphUtilsV2.getIdFromVertex(inVertex);
        LineageOnDemandConstraints inGuidLineageConstraints = getAndValidateLineageConstraintsByGuid(inGuid, lineageConstraintsMap);

        AtlasVertex                outVertex                 = isInput ? atlasEdge.getInVertex() : atlasEdge.getOutVertex();
        String                     outGuid                   = AtlasGraphUtilsV2.getIdFromVertex(outVertex);
        LineageOnDemandConstraints outGuidLineageConstraints = getAndValidateLineageConstraintsByGuid(outGuid, lineageConstraintsMap);

        LineageInfoOnDemand inLineageInfo = ret.getRelationsOnDemand().containsKey(inGuid) ? ret.getRelationsOnDemand().get(inGuid) : new LineageInfoOnDemand(inGuidLineageConstraints);
        LineageInfoOnDemand outLineageInfo = ret.getRelationsOnDemand().containsKey(outGuid) ? ret.getRelationsOnDemand().get(outGuid) : new LineageInfoOnDemand(outGuidLineageConstraints);

        if (inLineageInfo.isInputRelationsReachedLimit()) {
            inLineageInfo.setHasMoreInputs(true);
            hasRelationsLimitReached = true;
        }else {
            inLineageInfo.incrementInputRelationsCount();
        }

        if (outLineageInfo.isOutputRelationsReachedLimit()) {
            outLineageInfo.setHasMoreOutputs(true);
            hasRelationsLimitReached = true;
        } else {
            outLineageInfo.incrementOutputRelationsCount();
        }

        if (!hasRelationsLimitReached) {
            ret.getRelationsOnDemand().put(inGuid, inLineageInfo);
            ret.getRelationsOnDemand().put(outGuid, outLineageInfo);
        }

        return hasRelationsLimitReached;
    }

    private void addEdgeToResult(AtlasEdge edge, AtlasLineageInfo lineageInfo) throws AtlasBaseException {
        if (!lineageContainsEdge(lineageInfo, edge) && !lineageMaxNodeCountReached(lineageInfo.getRelations())) {
            processEdge(edge, lineageInfo);
        }
    }

    private int getLineageMaxNodeAllowedCount() {
        return Math.min(DEFAULT_LINEAGE_MAX_NODE_COUNT, AtlasConfiguration.LINEAGE_MAX_NODE_COUNT.getInt());
    }

    private boolean lineageMaxNodeCountReached(Set<LineageRelation> relations) {
        return CollectionUtils.isNotEmpty(relations) && relations.size() > getLineageMaxNodeAllowedCount();
    }

    private String getVisitedEdgeLabel(String inGuid, String outGuid, String relationGuid) {
        if (isLineageOnDemandEnabled()) {
            return inGuid + SEPARATOR + outGuid;
        }
        return relationGuid;
    }

    private boolean lineageContainsEdge(AtlasLineageInfo lineageInfo, AtlasEdge edge) {
        boolean ret = false;

        if (edge != null && lineageInfo != null && CollectionUtils.isNotEmpty(lineageInfo.getVisitedEdges())) {
            String  inGuid           = AtlasGraphUtilsV2.getIdFromVertex(edge.getInVertex());
            String  outGuid          = AtlasGraphUtilsV2.getIdFromVertex(edge.getOutVertex());
            String  relationGuid     = AtlasGraphUtilsV2.getEncodedProperty(edge, RELATIONSHIP_GUID_PROPERTY_KEY, String.class);
            boolean isInputEdge      = edge.getLabel().equalsIgnoreCase(PROCESS_INPUTS_EDGE);
            String  visitedEdgeLabel = isInputEdge ? getVisitedEdgeLabel(inGuid, outGuid, relationGuid) : getVisitedEdgeLabel(outGuid, inGuid, relationGuid);

            if (lineageInfo.getVisitedEdges().contains(visitedEdgeLabel)) {
                ret = true;
            }
        }

        return ret;
    }

    private void processEdge(final AtlasEdge edge, final AtlasLineageInfo lineageInfo) throws AtlasBaseException {
        processEdge(edge, lineageInfo.getGuidEntityMap(), lineageInfo.getRelations(), lineageInfo.getVisitedEdges());
    }

    private AtlasLineageInfo initializeLineageInfo(String guid, LineageDirection direction, int depth) {
        return new AtlasLineageInfo(guid, new HashMap<>(), new HashSet<>(), new HashSet<>(), new HashMap<>(), direction, depth);
    }

    private static String getId(AtlasVertex vertex) {
        return vertex.getIdForDisplay();
    }

    private List executeGremlinScript(Map<String, Object> bindings, String lineageQuery) throws AtlasBaseException {
        List         ret;
        ScriptEngine engine = graph.getGremlinScriptEngine();

        try {
            ret = (List) graph.executeGremlinScript(engine, bindings, lineageQuery, false);
        } catch (ScriptException e) {
            throw new AtlasBaseException(INSTANCE_LINEAGE_QUERY_FAILED, lineageQuery);
        } finally {
            graph.releaseGremlinScriptEngine(engine);
        }

        return ret;
    }

    private void processEdge(final AtlasEdge edge, final Map<String, AtlasEntityHeader> entities, final Set<LineageRelation> relations) throws AtlasBaseException {
        processEdge(edge, entities, relations, null);
    }

    private void processEdge(final AtlasEdge edge, final Map<String, AtlasEntityHeader> entities, final Set<LineageRelation> relations, final Set<String> visitedEdges) throws AtlasBaseException {
        AtlasVertex inVertex     = edge.getInVertex();
        AtlasVertex outVertex    = edge.getOutVertex();
        String      inGuid       = AtlasGraphUtilsV2.getIdFromVertex(inVertex);
        String      outGuid      = AtlasGraphUtilsV2.getIdFromVertex(outVertex);
        String      relationGuid = AtlasGraphUtilsV2.getEncodedProperty(edge, RELATIONSHIP_GUID_PROPERTY_KEY, String.class);
        boolean     isInputEdge  = edge.getLabel().equalsIgnoreCase(PROCESS_INPUTS_EDGE);


        if (!entities.containsKey(inGuid)) {
            AtlasEntityHeader entityHeader = entityRetriever.toAtlasEntityHeader(inVertex);
            entities.put(inGuid, entityHeader);
        }

        if (!entities.containsKey(outGuid)) {
            AtlasEntityHeader entityHeader = entityRetriever.toAtlasEntityHeader(outVertex);
            entities.put(outGuid, entityHeader);
        }

        if (isInputEdge) {
            relations.add(new LineageRelation(inGuid, outGuid, relationGuid));
        } else {
            relations.add(new LineageRelation(outGuid, inGuid, relationGuid));
        }

        if (visitedEdges != null) {
            String visitedEdgeLabel = isInputEdge ? getVisitedEdgeLabel(inGuid, outGuid, relationGuid) : getVisitedEdgeLabel(outGuid, inGuid, relationGuid);
            visitedEdges.add(visitedEdgeLabel);
        }
    }

    private AtlasLineageInfo getBothLineageInfoV1(String guid, int depth, boolean isDataSet) throws AtlasBaseException {
        AtlasLineageInfo inputLineage  = getLineageInfo(guid, INPUT, depth, isDataSet);
        AtlasLineageInfo outputLineage = getLineageInfo(guid, OUTPUT, depth, isDataSet);
        AtlasLineageInfo ret           = inputLineage;

        ret.getRelations().addAll(outputLineage.getRelations());
        ret.getGuidEntityMap().putAll(outputLineage.getGuidEntityMap());
        ret.setLineageDirection(BOTH);

        return ret;
    }

    private String getLineageQuery(String entityGuid, LineageDirection direction, int depth, boolean isDataSet, Map<String, Object> bindings) {
        String incomingFrom = null;
        String outgoingTo   = null;
        String ret;

        if (direction.equals(INPUT)) {
            incomingFrom = PROCESS_OUTPUTS_EDGE;
            outgoingTo   = PROCESS_INPUTS_EDGE;
        } else if (direction.equals(OUTPUT)) {
            incomingFrom = PROCESS_INPUTS_EDGE;
            outgoingTo   = PROCESS_OUTPUTS_EDGE;
        }

        bindings.put("guid", entityGuid);
        bindings.put("incomingEdgeLabel", incomingFrom);
        bindings.put("outgoingEdgeLabel", outgoingTo);
        bindings.put("dataSetDepth", depth);
        bindings.put("processDepth", depth - 1);

        if (depth < 1) {
            ret = isDataSet ? gremlinQueryProvider.getQuery(FULL_LINEAGE_DATASET) :
                              gremlinQueryProvider.getQuery(FULL_LINEAGE_PROCESS);
        } else {
            ret = isDataSet ? gremlinQueryProvider.getQuery(PARTIAL_LINEAGE_DATASET) :
                              gremlinQueryProvider.getQuery(PARTIAL_LINEAGE_PROCESS);
        }

        return ret;
    }

    public boolean isLineageOnDemandEnabled() {
        return AtlasConfiguration.LINEAGE_ON_DEMAND_ENABLED.getBoolean();
    }
}