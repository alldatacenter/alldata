/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.impala.hook.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.atlas.impala.hook.AtlasImpalaHookContext;
import org.apache.atlas.impala.model.ImpalaDataType;
import org.apache.atlas.impala.model.ImpalaDependencyType;
import org.apache.atlas.impala.model.ImpalaNode;
import org.apache.atlas.impala.model.ImpalaVertexType;
import org.apache.atlas.impala.model.LineageEdge;
import org.apache.atlas.impala.model.ImpalaQuery;
import org.apache.atlas.impala.model.LineageVertex;
import org.apache.atlas.impala.model.LineageVertexMetadata;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.notification.HookNotification;
import org.apache.atlas.model.notification.HookNotification.EntityCreateRequestV2;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateImpalaProcess extends BaseImpalaEvent {
    private static final Logger LOG = LoggerFactory.getLogger(CreateImpalaProcess.class);

    public CreateImpalaProcess(AtlasImpalaHookContext context) {
        super(context);
    }

    public List<HookNotification> getNotificationMessages() throws Exception {
        List<HookNotification>   ret      = null;
        AtlasEntitiesWithExtInfo entities = getEntities();

        if (entities != null && CollectionUtils.isNotEmpty(entities.getEntities())) {
            ret = Collections.singletonList(new EntityCreateRequestV2(getUserName(), entities));
        }

        return ret;
    }

    public AtlasEntitiesWithExtInfo getEntities() throws Exception {
        AtlasEntitiesWithExtInfo ret     = null;
        List<ImpalaNode> inputNodes      = new ArrayList<>();
        List<ImpalaNode> outputNodes     = new ArrayList<>();
        List<AtlasEntity> inputs         = new ArrayList<>();
        List<AtlasEntity> outputs        = new ArrayList<>();
        Set<String> processedNames       = new HashSet<>();

        getInputOutList(context.getLineageQuery(), inputNodes, outputNodes);

        if (skipProcess(inputNodes, outputNodes)) {
            return ret;
        }

        ret = new AtlasEntitiesWithExtInfo();

        if (!inputNodes.isEmpty()) {
            for (ImpalaNode input : inputNodes) {
                String qualifiedName = getQualifiedName(input);

                if (qualifiedName == null || !processedNames.add(qualifiedName)) {
                    continue;
                }

                AtlasEntity entity = getInputOutputEntity(input, ret);

                if (entity != null) {
                    inputs.add(entity);
                }
            }
        }

        if (outputNodes != null) {
            for (ImpalaNode output : outputNodes) {
                String qualifiedName = getQualifiedName(output);

                if (qualifiedName == null || !processedNames.add(qualifiedName)) {
                    continue;
                }

                AtlasEntity entity = getInputOutputEntity(output, ret);

                if (entity != null) {
                    outputs.add(entity);

                    if (isDdlOperation()) {
                        AtlasEntity ddlEntity = createHiveDDLEntity(entity);
                        if (ddlEntity != null) {
                            ret.addEntity(ddlEntity);
                        }
                    }
                }
            }
        }

        if (!inputs.isEmpty() || !outputs.isEmpty()) {
            AtlasEntity process = getImpalaProcessEntity(inputs, outputs);
            if (process!= null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("get process entity with qualifiedName: {}",
                        process.getAttribute(ATTRIBUTE_QUALIFIED_NAME));
                }

                ret.addEntity(process);

                AtlasEntity processExecution = getImpalaProcessExecutionEntity(process);
                if (processExecution != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("get process executition entity with qualifiedName: {}",
                            processExecution.getAttribute(ATTRIBUTE_QUALIFIED_NAME));
                    }

                    ret.addEntity(processExecution);
                }

                processColumnLineage(process, ret);

                addProcessedEntities(ret);
            }
        } else {
            ret = null;
        }


        return ret;
    }

    private void processColumnLineage(AtlasEntity impalaProcess, AtlasEntitiesWithExtInfo entities) {
        List<LineageEdge> edges = context.getLineageQuery().getEdges();

        if (CollectionUtils.isEmpty(edges)) {
            return;
        }

        final List<AtlasEntity> columnLineages      = new ArrayList<>();
        final Set<String>       processedOutputCols = new HashSet<>();

        for (LineageEdge edge : edges) {

            if (!edge.getEdgeType().equals(ImpalaDependencyType.PROJECTION)) {
                // Impala dependency type can only be predicate or projection.
                // Impala predicate dependency: This is a dependency between a set of target
                // columns (or exprs) and a set of source columns (base table columns). It
                // indicates that the source columns restrict the values of their targets (e.g.
                // by participating in WHERE clause predicates). It should not be part of lineage
                continue;
            }

            List<AtlasEntity> outputColumns = new ArrayList<>();
            for (Long targetId : edge.getTargets()) {
                LineageVertex columnVertex = verticesMap.get(targetId);
                String outputColName = getQualifiedName(columnVertex);
                AtlasEntity outputColumn = context.getEntity(outputColName);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("processColumnLineage(): target id = {}, target column name = {}",
                        targetId, outputColName);
                }

                if (outputColumn == null) {
                    LOG.warn("column-lineage: non-existing output-column {}", outputColName);
                    continue;
                }

                if (processedOutputCols.contains(outputColName)) {
                    LOG.warn("column-lineage: duplicate for output-column {}", outputColName);
                    continue;
                } else {
                    processedOutputCols.add(outputColName);
                }

                outputColumns.add(outputColumn);
            }

            List<AtlasEntity> inputColumns = new ArrayList<>();

            for (Long sourceId : edge.getSources()) {
                LineageVertex columnVertex = verticesMap.get(sourceId);
                String        inputColName = getQualifiedName(columnVertex);
                AtlasEntity   inputColumn  = context.getEntity(inputColName);

                if (inputColumn == null) {
                    LOG.warn("column-lineage: non-existing input-column {} with id ={}", inputColName, sourceId);
                    continue;
                }

                inputColumns.add(inputColumn);
            }

            if (inputColumns.isEmpty()) {
                continue;
            }

            AtlasEntity columnLineageProcess = new AtlasEntity(ImpalaDataType.IMPALA_COLUMN_LINEAGE.getName());

            String columnQualifiedName = (String)impalaProcess.getAttribute(ATTRIBUTE_QUALIFIED_NAME) +
                AtlasImpalaHookContext.QNAME_SEP_PROCESS + outputColumns.get(0).getAttribute(ATTRIBUTE_NAME);
            columnLineageProcess.setAttribute(ATTRIBUTE_NAME, columnQualifiedName);
            columnLineageProcess.setAttribute(ATTRIBUTE_QUALIFIED_NAME, columnQualifiedName);
            columnLineageProcess.setAttribute(ATTRIBUTE_INPUTS, getObjectIds(inputColumns));
            columnLineageProcess.setAttribute(ATTRIBUTE_OUTPUTS, getObjectIds(outputColumns));
            columnLineageProcess.setAttribute(ATTRIBUTE_QUERY, getObjectId(impalaProcess));

            // based on https://github.com/apache/impala/blob/master/fe/src/main/java/org/apache/impala/analysis/ColumnLineageGraph.java#L267
            // There are two types of dependencies that are represented as edges in the column
            // lineage graph:
            //    a) Projection dependency: This is a dependency between a set of source
            //    columns (base table columns) and a single target (result expr or table column).
            //    This dependency indicates that values of the target depend on the values of the source
            //    columns.
            //    b) Predicate dependency: This is a dependency between a set of target
            //    columns (or exprs) and a set of source columns (base table columns). It indicates that
            //    the source columns restrict the values of their targets (e.g. by participating in
            //    WHERE clause predicates).
            columnLineageProcess.setAttribute(ATTRIBUTE_DEPENDENCY_TYPE, ImpalaDependencyType.PROJECTION.getName());

            columnLineages.add(columnLineageProcess);
        }

        for (AtlasEntity columnLineage : columnLineages) {
            String columnQualifiedName = (String)columnLineage.getAttribute(ATTRIBUTE_QUALIFIED_NAME);
            if (LOG.isDebugEnabled()) {
                LOG.debug("get column lineage entity with qualifiedName: {}", columnQualifiedName);
            }

            entities.addEntity(columnLineage);
        }
    }

    // Process the impala query, classify the vertices as input or output based on LineageEdge
    // Then organize the vertices into hierarchical structure: put all column vertices of a table
    // as children of a ImpalaNode representing that table.
    private void getInputOutList(ImpalaQuery lineageQuery, List<ImpalaNode> inputNodes,
        List<ImpalaNode> outputNodes) {
        // get vertex map with key being its id and
        // ImpalaNode map with its own vertex's vertexId as its key
        for (LineageVertex vertex : lineageQuery.getVertices()) {
            updateVertexMap(vertex);
        }

        // get set of source ID and set of target Id
        Set<Long> sourceIds = new HashSet<>();
        Set<Long> targetIds = new HashSet<>();
        for (LineageEdge edge : lineageQuery.getEdges()) {
            if (ImpalaDependencyType.PROJECTION.equals(edge.getEdgeType())) {
                sourceIds.addAll(edge.getSources());
                targetIds.addAll(edge.getTargets());
            }
        }

        Map<String, ImpalaNode> inputMap  = buildInputOutputList(sourceIds, verticesMap, vertexNameMap);
        Map<String, ImpalaNode> outputMap = buildInputOutputList(targetIds, verticesMap, vertexNameMap);

        inputNodes.addAll(inputMap.values());
        outputNodes.addAll(outputMap.values());
    }

    // Update internal maps using this vertex.
    private void updateVertexMap(LineageVertex vertex) {
        verticesMap.put(vertex.getId(), vertex);
        vertexNameMap.put(vertex.getVertexId(), new ImpalaNode(vertex));

        if (vertex.getVertexType() == ImpalaVertexType.COLUMN) {
            LineageVertexMetadata metadata = vertex.getMetadata();

            if (metadata == null) {
                return;
            }

            // if the vertex is column and contains metadata, create a vertex for its table
            String tableName = metadata.getTableName();
            ImpalaNode tableNode = vertexNameMap.get(tableName);

            if (tableNode == null) {
                tableNode = createTableNode(tableName, metadata.getTableCreateTime());
                vertexNameMap.put(tableName, tableNode);
            }
        }
    }

    /**
     * From the list of Ids and Id to Vertices map, generate the Table name to ImpalaNode map.
     * @param idSet the list of Ids. They are from lineage edges
     * @param vertexMap the Id to Vertex map
     * @param vertexNameMap the vertexId to ImpalaNode map.
     * @return the table name to ImpalaNode map, whose table node contains its columns
     */
    private Map<String, ImpalaNode> buildInputOutputList(Set<Long> idSet, Map<Long, LineageVertex> vertexMap,
        Map<String, ImpalaNode> vertexNameMap) {
        Map<String, ImpalaNode> returnTableMap = new HashMap<>();

        for (Long id : idSet) {
            LineageVertex vertex = vertexMap.get(id);
            if (vertex == null) {
                LOG.warn("cannot find vertex with id: {}", id);
                continue;
            }

            if (ImpalaVertexType.COLUMN.equals(vertex.getVertexType())) {
                // add column to its table node
                String tableName = getTableNameFromVertex(vertex);
                if (tableName == null) {
                    LOG.warn("cannot find tableName for vertex with id: {}, column name : {}",
                        id, vertex.getVertexId() == null? "null" : vertex.getVertexId());

                    continue;
                }

                ImpalaNode tableNode = returnTableMap.get(tableName);

                if (tableNode == null) {
                    tableNode = vertexNameMap.get(tableName);

                    if (tableNode == null) {
                        LOG.warn("cannot find table node for vertex with id: {}, column name : {}",
                            id, vertex.getVertexId());

                        tableNode = createTableNode(tableName, getCreateTimeInVertex(null));
                        vertexNameMap.put(tableName, tableNode);
                    }

                    returnTableMap.put(tableName, tableNode);
                }

                tableNode.addChild(vertex);
            }
        }

        return returnTableMap;
    }

    private boolean skipProcess(List<ImpalaNode> inputNodes, List<ImpalaNode> ouputNodes) {
        if (inputNodes.isEmpty() || ouputNodes.isEmpty()) {
            return true;
        }

        return false;
    }
}
