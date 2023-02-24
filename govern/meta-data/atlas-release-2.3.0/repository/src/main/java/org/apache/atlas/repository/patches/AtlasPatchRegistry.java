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

package org.apache.atlas.repository.patches;

import org.apache.atlas.RequestContext;
import org.apache.atlas.model.patches.AtlasPatch;
import org.apache.atlas.model.patches.AtlasPatch.AtlasPatches;
import org.apache.atlas.model.patches.AtlasPatch.PatchStatus;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasGraphQuery;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.repository.store.graph.v2.AtlasTypeDefGraphStoreV2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.apache.atlas.model.patches.AtlasPatch.PatchStatus.FAILED;
import static org.apache.atlas.model.patches.AtlasPatch.PatchStatus.UNKNOWN;
import static org.apache.atlas.repository.Constants.*;
import static org.apache.atlas.repository.graphdb.AtlasGraphQuery.ComparisionOperator.EQUAL;
import static org.apache.atlas.repository.patches.AtlasPatchHandler.JAVA_PATCH_TYPE;
import static org.apache.atlas.repository.store.bootstrap.AtlasTypeDefStoreInitializer.TYPEDEF_PATCH_TYPE;
import static org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2.getEncodedProperty;
import static org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2.setEncodedProperty;
import static org.apache.atlas.repository.store.graph.v2.AtlasTypeDefGraphStoreV2.getCurrentUser;

public class AtlasPatchRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasPatchRegistry.class);

    private final Map<String, PatchStatus> patchNameStatusMap;
    private final AtlasGraph               graph;

    public AtlasPatchRegistry(AtlasGraph graph) {
        LOG.info("AtlasPatchRegistry: initializing..");

        this.graph              = graph;
        this.patchNameStatusMap = getPatchNameStatusForAllRegistered(graph);

        LOG.info("AtlasPatchRegistry: found {} patches", patchNameStatusMap.size());

        for (Map.Entry<String, PatchStatus> entry : patchNameStatusMap.entrySet()) {
            LOG.info("AtlasPatchRegistry: patchId={}, status={}", entry.getKey(), entry.getValue());
        }
    }

    public boolean isApplicable(String incomingId, String patchFile, int index) {
        String patchId = getId(incomingId, patchFile, index);

        if (MapUtils.isEmpty(patchNameStatusMap) || !patchNameStatusMap.containsKey(patchId)) {
            return true;
        }

        PatchStatus status = patchNameStatusMap.get(patchId);

        if (status == FAILED || status == UNKNOWN) {
            return true;
        }

        return false;
    }

    public PatchStatus getStatus(String id) {
        return patchNameStatusMap.get(id);
    }

    public void register(String patchId, String description, String patchType, String action, PatchStatus patchStatus) {
        createOrUpdatePatchVertex(graph, patchId, description, patchType, action, patchStatus);
    }

    public void updateStatus(String patchId, PatchStatus patchStatus) {
        try {
            AtlasVertex patchVertex = findByPatchId(patchId);

            if (patchVertex != null) {
                setEncodedProperty(patchVertex, PATCH_STATE_PROPERTY_KEY, patchStatus.toString());
                setEncodedProperty(patchVertex, MODIFICATION_TIMESTAMP_PROPERTY_KEY, RequestContext.get().getRequestTime());
                setEncodedProperty(patchVertex, MODIFIED_BY_KEY, getCurrentUser());
                setEncodedProperty(patchVertex, PATCH_STATE_PROPERTY_KEY, patchStatus.toString());
            }
        } finally {
            graph.commit();

            patchNameStatusMap.put(patchId, patchStatus);
        }
    }

    private static String getId(String incomingId, String patchFile, int index) {
        String patchId = incomingId;

        if (StringUtils.isEmpty(patchId)) {
            return String.format("%s_%s", patchFile, index);
        }

        return patchId;
    }

    public AtlasPatches getAllPatches() {
        return getAllPatches(graph);
    }

    private void createOrUpdatePatchVertex(AtlasGraph graph, String patchId, String description,
                                           String patchType, String action, PatchStatus patchStatus) {
        try {
            AtlasVertex patchVertex = findByPatchId(patchId);

            if (patchVertex == null) {
                patchVertex = graph.addVertex();
            }

            setEncodedProperty(patchVertex, PATCH_ID_PROPERTY_KEY, patchId);
            setEncodedProperty(patchVertex, PATCH_DESCRIPTION_PROPERTY_KEY, description);
            setEncodedProperty(patchVertex, PATCH_TYPE_PROPERTY_KEY, patchType);
            setEncodedProperty(patchVertex, PATCH_ACTION_PROPERTY_KEY, action);
            setEncodedProperty(patchVertex, PATCH_STATE_PROPERTY_KEY, patchStatus.toString());
            setEncodedProperty(patchVertex, TIMESTAMP_PROPERTY_KEY, RequestContext.get().getRequestTime());
            setEncodedProperty(patchVertex, MODIFICATION_TIMESTAMP_PROPERTY_KEY, RequestContext.get().getRequestTime());
            setEncodedProperty(patchVertex, CREATED_BY_KEY, AtlasTypeDefGraphStoreV2.getCurrentUser());
            setEncodedProperty(patchVertex, MODIFIED_BY_KEY, AtlasTypeDefGraphStoreV2.getCurrentUser());
        } finally {
            graph.commit();

            patchNameStatusMap.put(patchId, patchStatus);
        }
    }

    private static Map<String, PatchStatus> getPatchNameStatusForAllRegistered(AtlasGraph graph) {
        Map<String, PatchStatus> ret     = new HashMap<>();
        AtlasPatches             patches = getAllPatches(graph);

        for (AtlasPatch patch : patches.getPatches()) {
            String      patchId     = patch.getId();
            PatchStatus patchStatus = patch.getStatus();

            if (patchId != null && patchStatus != null) {
                ret.put(patchId, patchStatus);
            }
        }

        return ret;
    }

    private static AtlasPatches getAllPatches(AtlasGraph graph) {
        List<AtlasGraphQuery> orConditions = new ArrayList<>();
        List<AtlasPatch> ret = new ArrayList<>();
        AtlasGraphQuery query = graph.query();

        orConditions.add(query.createChildQuery().has(PATCH_TYPE_PROPERTY_KEY, EQUAL, TYPEDEF_PATCH_TYPE));
        orConditions.add(query.createChildQuery().has(PATCH_TYPE_PROPERTY_KEY, EQUAL, JAVA_PATCH_TYPE));

        query.or(orConditions);

        try {
            Iterator<AtlasVertex> results = query.vertices().iterator();

            while (results != null && results.hasNext()) {
                AtlasVertex patchVertex = results.next();
                AtlasPatch patch = toAtlasPatch(patchVertex);
                ret.add(patch);
            }

            if (CollectionUtils.isNotEmpty(ret)) {
                Collections.sort(ret, Comparator.comparing(AtlasPatch::getId));
            }
        } catch (Throwable t) {
            LOG.warn("getAllPatches(): Returned empty result!");
        } finally {
            graph.commit();
        }

        return new AtlasPatches(ret);
    }


    private static AtlasPatch toAtlasPatch(AtlasVertex vertex) {
        AtlasPatch ret = new AtlasPatch();

        ret.setId(getEncodedProperty(vertex, PATCH_ID_PROPERTY_KEY, String.class));
        ret.setDescription(getEncodedProperty(vertex, PATCH_DESCRIPTION_PROPERTY_KEY, String.class));
        ret.setType(getEncodedProperty(vertex, PATCH_TYPE_PROPERTY_KEY, String.class));
        ret.setAction(getEncodedProperty(vertex, PATCH_ACTION_PROPERTY_KEY, String.class));
        ret.setCreatedBy(getEncodedProperty(vertex, CREATED_BY_KEY, String.class));
        ret.setUpdatedBy(getEncodedProperty(vertex, MODIFIED_BY_KEY, String.class));
        ret.setCreatedTime(getEncodedProperty(vertex, TIMESTAMP_PROPERTY_KEY, Long.class));
        ret.setUpdatedTime(getEncodedProperty(vertex, MODIFICATION_TIMESTAMP_PROPERTY_KEY, Long.class));
        ret.setStatus(getPatchStatus(vertex));

        return ret;
    }

    public AtlasVertex findByPatchId(String patchId) {
        AtlasGraphQuery       query   = graph.query().has(Constants.PATCH_ID_PROPERTY_KEY, patchId);
        Iterator<AtlasVertex> results = query.vertices().iterator();

        return results.hasNext() ? results.next() : null;
    }

    private static PatchStatus getPatchStatus(AtlasVertex vertex) {
        String patchStatus = AtlasGraphUtilsV2.getEncodedProperty(vertex, PATCH_STATE_PROPERTY_KEY, String.class);

        return patchStatus != null ? PatchStatus.valueOf(patchStatus) : UNKNOWN;
    }
}
