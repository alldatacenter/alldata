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
package org.apache.atlas.repository.store.graph.v2;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.RequestContext;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasCheckStateRequest;
import org.apache.atlas.model.instance.AtlasCheckStateResult;
import org.apache.atlas.model.instance.AtlasCheckStateResult.AtlasEntityState;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.graph.GraphHelper;
import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasEdgeDirection;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasGraphQuery;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.apache.atlas.repository.Constants.CLASSIFICATION_EDGE_IS_PROPAGATED_PROPERTY_KEY;
import static org.apache.atlas.repository.graph.GraphHelper.getDelimitedClassificationNames;

@Component
public final class EntityStateChecker {
    private static final Logger LOG = LoggerFactory.getLogger(EntityStateChecker.class);

    private final AtlasGraph           graph;
    private final AtlasTypeRegistry    typeRegistry;
    private final EntityGraphRetriever entityRetriever;

    @Inject
    public EntityStateChecker(AtlasGraph graph, AtlasTypeRegistry typeRegistry) {
        this.graph           = graph;
        this.typeRegistry    = typeRegistry;
        this.entityRetriever = new EntityGraphRetriever(graph, typeRegistry);
    }


    public AtlasCheckStateResult checkState(AtlasCheckStateRequest request) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> checkState({})", request);
        }

        AtlasCheckStateResult ret = new AtlasCheckStateResult();

        if (request != null) {
            if (CollectionUtils.isNotEmpty(request.getEntityGuids())) {
                for (String guid : request.getEntityGuids()) {
                    checkEntityState(guid, request.getFixIssues(), ret);
                }
            } else if (CollectionUtils.isNotEmpty(request.getEntityTypes())) {
                final Collection<String> entityTypes;

                if (request.getEntityTypes().contains("*")) {
                    entityTypes = typeRegistry.getAllEntityDefNames();
                } else {
                    entityTypes = request.getEntityTypes();
                }

                LOG.info("checkState(): scanning for entities of {} types", entityTypes.size());

                for (String typeName : entityTypes) {
                    AtlasEntityType entityType = typeRegistry.getEntityTypeByName(typeName);

                    if (entityType == null) {
                        LOG.warn("checkState(): {} - entity-type not found", typeName);

                        continue;
                    }

                    LOG.info("checkState(): scanning for {} entities", typeName);

                    AtlasGraphQuery query = AtlasGraphProvider.getGraphInstance().query().has(Constants.ENTITY_TYPE_PROPERTY_KEY, typeName);

                    int count = 0;
                    for (Iterator<AtlasVertex> iter = query.vertices().iterator(); iter.hasNext(); count++) {
                        checkEntityState(iter.next(), request.getFixIssues(), ret);
                    }

                    LOG.info("checkState(): scanned {} {} entities", count, typeName);
                }
            }

            int incorrectFixed          = ret.getEntitiesFixed();
            int incorrectPartiallyFixed = ret.getEntitiesPartiallyFixed();
            int incorrectNotFixed       = ret.getEntitiesNotFixed();

            if (incorrectFixed == 0 && incorrectPartiallyFixed == 0 && incorrectNotFixed == 0) {
                ret.setState(AtlasCheckStateResult.State.OK);
            } else if (incorrectPartiallyFixed != 0) {
                ret.setState(AtlasCheckStateResult.State.PARTIALLY_FIXED);
            } else if (incorrectNotFixed != 0) {
                ret.setState(incorrectFixed > 0 ? AtlasCheckStateResult.State.PARTIALLY_FIXED : AtlasCheckStateResult.State.NOT_FIXED);
            } else {
                ret.setState(AtlasCheckStateResult.State.FIXED);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== checkState({}, {})", request, ret);
        }

        return ret;
    }


    /**
     * Check an entity state given its GUID
     * @param guid
     * @return
     * @throws AtlasBaseException
     */
    public AtlasEntityState checkEntityState(String guid, boolean fixIssues, AtlasCheckStateResult result) throws AtlasBaseException {
        AtlasVertex entityVertex = AtlasGraphUtilsV2.findByGuid(this.graph, guid);

        if (entityVertex == null) {
            throw new AtlasBaseException(AtlasErrorCode.INSTANCE_GUID_NOT_FOUND, guid);
        }

        return checkEntityState(entityVertex, fixIssues, result);
    }

    /**
     * Check an entity state given its vertex
     * @param entityVertex
     * @return
     * @throws AtlasBaseException
     */
    public AtlasEntityState checkEntityState(AtlasVertex entityVertex, boolean fixIssues, AtlasCheckStateResult result) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> checkEntityState(guid={})", AtlasGraphUtilsV2.getIdFromVertex(entityVertex));
        }

        AtlasEntityState ret = new AtlasEntityState();

        ret.setGuid(AtlasGraphUtilsV2.getIdFromVertex(entityVertex));
        ret.setTypeName(AtlasGraphUtilsV2.getTypeName(entityVertex));
        ret.setName(getEntityName(entityVertex));
        ret.setStatus(AtlasGraphUtilsV2.getState(entityVertex));
        ret.setState(AtlasCheckStateResult.State.OK);

        checkEntityState_Classifications(entityVertex, ret, fixIssues);

        if (ret.getState() != AtlasCheckStateResult.State.OK) { // don't include clean entities in the response
            if (result.getEntities() == null) {
                result.setEntities(new HashMap<String, AtlasEntityState>());
            }

            result.getEntities().put(ret.getGuid(), ret);
        }

        result.incrEntitiesScanned();

        switch (ret.getState()) {
            case FIXED:
                result.incrEntitiesFixed();
                break;

            case PARTIALLY_FIXED:
                result.incrEntitiesPartiallyFixed();
                break;

            case NOT_FIXED:
                result.incrEntitiesNotFixed();
                break;

            case OK:
                result.incrEntitiesOk();
                break;
        }

        LOG.info("checkEntityState(guid={}; type={}; name={}): {}", ret.getGuid(), ret.getTypeName(), ret.getName(), ret.getState());

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== checkEntityState({}): {}", ret.getGuid(), ret);
        }

        return ret;
    }

    private void checkEntityState_Classifications(AtlasVertex entityVertex, AtlasEntityState result, boolean fixIssues) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> checkEntityState_Classifications({}, {})", result, fixIssues);
        }

        Collection<String>  traitNames                 = entityVertex.getPropertyValues(Constants.TRAIT_NAMES_PROPERTY_KEY, String.class);
        Collection<String>  propagatedTraitNames       = entityVertex.getPropertyValues(Constants.PROPAGATED_TRAIT_NAMES_PROPERTY_KEY, String.class);
        Collection<String>  traitVertexNames           = null;
        Collection<String>  propagatedTraitVertexNames = null;
        Iterable<AtlasEdge> edges                      = entityVertex.getEdges(AtlasEdgeDirection.OUT, Constants.CLASSIFICATION_LABEL);

        if (edges != null) {
            for (Iterator<AtlasEdge> iter = edges.iterator(); iter.hasNext(); ) {
                AtlasEdge               edge               = iter.next();
                Boolean                 isPropagated       = AtlasGraphUtilsV2.getEncodedProperty(edge, CLASSIFICATION_EDGE_IS_PROPAGATED_PROPERTY_KEY, Boolean.class);
                String                  classificationName = GraphHelper.getTypeName(edge.getInVertex());
                AtlasClassificationType classification     = typeRegistry.getClassificationTypeByName(classificationName);

                if (classification != null) {
                    if (isPropagated != null && isPropagated) {
                        propagatedTraitVertexNames = addToCollection(propagatedTraitVertexNames, classificationName);
                    } else {
                        traitVertexNames = addToCollection(traitVertexNames, classificationName);
                    }
                }
            }

            if (traitVertexNames == null) {
                traitVertexNames = Collections.emptyList();
            }

            if (propagatedTraitVertexNames == null) {
                propagatedTraitVertexNames = Collections.emptyList();
            }
        }

        Collection<String> traitNamesToAdd              = subtract(traitVertexNames, traitNames);
        Collection<String> traitNamesToRemove           = subtract(traitNames, traitVertexNames);
        Collection<String> propagatedTraitNamesToAdd    = subtract(propagatedTraitVertexNames, propagatedTraitNames);
        Collection<String> propagatedTraitNamesToRemove = subtract(propagatedTraitNames, propagatedTraitVertexNames);

        if (traitNamesToAdd != null || traitNamesToRemove != null || propagatedTraitNamesToAdd != null || propagatedTraitNamesToRemove != null) {
            List<String> issues = result.getIssues();

            if (issues == null) {
                issues = new ArrayList<>();

                result.setIssues(issues);
            }

            if (fixIssues) {
                if (traitNamesToAdd != null || traitNamesToRemove != null) {
                    if (traitNamesToAdd != null) {
                        issues.add("incorrect property: __traitNames has missing classifications: " + traitNamesToAdd.toString());
                    }

                    if (traitNamesToRemove != null) {
                        issues.add("incorrect property: __traitNames has unassigned classifications: " + traitNamesToRemove.toString());
                    }

                    entityVertex.removeProperty(Constants.TRAIT_NAMES_PROPERTY_KEY);
                    entityVertex.removeProperty(Constants.CLASSIFICATION_NAMES_KEY);

                    for (String classificationName : traitVertexNames) {
                        AtlasGraphUtilsV2.addEncodedProperty(entityVertex, Constants.TRAIT_NAMES_PROPERTY_KEY, classificationName);
                    }

                    entityVertex.setProperty(Constants.CLASSIFICATION_NAMES_KEY, getDelimitedClassificationNames(traitVertexNames));
                }

                if (propagatedTraitNamesToAdd != null || propagatedTraitNamesToRemove != null) {
                    if (propagatedTraitNamesToAdd != null) {
                        issues.add("incorrect property: __propagatedTraitNames has missing classifications: " + propagatedTraitNamesToAdd.toString());
                    }

                    if (propagatedTraitNamesToRemove != null) {
                        issues.add("incorrect property: __propagatedTraitNames has unassigned classifications: " + propagatedTraitNamesToRemove.toString());
                    }

                    entityVertex.removeProperty(Constants.PROPAGATED_TRAIT_NAMES_PROPERTY_KEY);
                    entityVertex.removeProperty(Constants.PROPAGATED_CLASSIFICATION_NAMES_KEY);

                    for (String classificationName : propagatedTraitVertexNames) {
                        AtlasGraphUtilsV2.addEncodedProperty(entityVertex, Constants.PROPAGATED_TRAIT_NAMES_PROPERTY_KEY, classificationName);
                    }

                    entityVertex.setProperty(Constants.PROPAGATED_CLASSIFICATION_NAMES_KEY,getDelimitedClassificationNames(propagatedTraitVertexNames));
                }

                AtlasGraphUtilsV2.setEncodedProperty(entityVertex, Constants.MODIFICATION_TIMESTAMP_PROPERTY_KEY, RequestContext.get().getRequestTime());

                result.setState(AtlasCheckStateResult.State.FIXED);
            } else {
                result.setState(AtlasCheckStateResult.State.NOT_FIXED);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== checkEntityState_Classifications({}, {})", result, fixIssues);
        }
    }

    private String getEntityName(AtlasVertex entityVertex) throws AtlasBaseException {
        String              ret              = null;
        Map<String, Object> uniqueAttributes = entityRetriever.getEntityUniqueAttribute(entityVertex);

        if (uniqueAttributes != null) {
            Object val = uniqueAttributes.get("qualifiedName");

            if (val == null) {
                for (Object attrVal : uniqueAttributes.values()) {
                    if (attrVal != null) {
                        ret = attrVal.toString();

                        break;
                    }
                }
            } else {
                ret = val.toString();
            }
        }

        return ret;
    }

    private Collection<String> addToCollection(Collection<String> list, String str) {
        if (list == null) {
            list = new ArrayList<>();
        }

        list.add(str);

        return list;
    }

    // return elements in 'col1' that are not in 'col2'
    private Collection<String> subtract(Collection<String> col1, Collection<String> col2) {
        Collection<String> ret = null;

        if (col2 == null) {
            ret = col1;
        } else if (col1 != null) {
            for (String elem : col1) {
                if (!col2.contains(elem)) {
                    if (ret == null) {
                        ret = new ArrayList<>();
                    }

                    ret.add(elem);
                }
            }
        }

        return ret;
    }
}

