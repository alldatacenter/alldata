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

package org.apache.atlas.repository.graphdb.janus.migration;

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.model.impexp.MigrationStatus;
import org.apache.atlas.repository.Constants;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class ReaderStatusManager {
    private static final Logger LOG = LoggerFactory.getLogger(ReaderStatusManager.class);

    private static final String MIGRATION_STATUS_TYPE_NAME = "__MigrationStatus";
    private static final String CURRENT_INDEX_PROPERTY     = "currentIndex";
    private static final String CURRENT_COUNTER_PROPERTY   = "currentCounter";
    private static final String OPERATION_STATUS_PROPERTY  = "operationStatus";
    private static final String START_TIME_PROPERTY        = "startTime";
    private static final String END_TIME_PROPERTY          = "endTime";
    private static final String TOTAL_COUNT_PROPERTY       = "totalCount";

    public static final String STATUS_NOT_STARTED = "NOT_STARTED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_SUCCESS     = "SUCCESS";
    public static final String STATUS_FAILED      = "FAILED";

    @VisibleForTesting
    Object migrationStatusId = null;
    private Vertex migrationStatus   = null;

    public ReaderStatusManager(Graph graph, Graph bulkLoadGraph) {
        init(graph, bulkLoadGraph);
    }

    public void init(Graph graph, Graph bulkLoadGraph) {
        migrationStatus = fetchUsingTypeName(bulkLoadGraph.traversal());
        if(migrationStatus == null) {
            createAndCommit(graph);
            migrationStatus = fetchUsingId(bulkLoadGraph.traversal());
        }

        if(migrationStatus == null) {
            migrationStatus = fetchUsingId(bulkLoadGraph.traversal());
        }
    }

    public void end(Graph bGraph, Long counter, String status) {
        migrationStatus.property(END_TIME_PROPERTY, new Date());
        migrationStatus.property(TOTAL_COUNT_PROPERTY, counter);

        update(bGraph, counter, status);
    }

    public void update(Graph graph, Long counter, boolean stageEnd) {
        migrationStatus.property(CURRENT_COUNTER_PROPERTY, counter);

        if(stageEnd) {
            migrationStatus.property(CURRENT_INDEX_PROPERTY, counter);
        }

        if(graph.features().graph().supportsTransactions()) {
            graph.tx().commit();
        }
    }

    public void update(Graph graph, Long counter, String status) {
        migrationStatus.property(OPERATION_STATUS_PROPERTY, status);
        update(graph, counter, true);
    }

    public void clear() {
        migrationStatus = null;
    }

    public long getStartIndex() {
        return (long) migrationStatus.property(CURRENT_INDEX_PROPERTY).value();
    }

    private Vertex fetchUsingId(GraphTraversalSource g) {
        return g.V(migrationStatusId).next();
    }

    private static Vertex fetchUsingTypeName(GraphTraversalSource g) {
        GraphTraversal src = g.V().has(Constants.ENTITY_TYPE_PROPERTY_KEY, MIGRATION_STATUS_TYPE_NAME);
        return src.hasNext() ? (Vertex) src.next() : null;
    }

    private void createAndCommit(Graph rGraph) {
        Vertex v = rGraph.addVertex();

        long longValue = 0L;
        v.property(Constants.ENTITY_TYPE_PROPERTY_KEY, MIGRATION_STATUS_TYPE_NAME);
        v.property(CURRENT_COUNTER_PROPERTY, longValue);
        v.property(CURRENT_INDEX_PROPERTY, longValue);
        v.property(TOTAL_COUNT_PROPERTY, longValue);
        v.property(OPERATION_STATUS_PROPERTY, STATUS_NOT_STARTED);
        v.property(START_TIME_PROPERTY, new Date());
        v.property(END_TIME_PROPERTY, new Date());

        migrationStatusId = v.id();

        if(rGraph.features().graph().supportsTransactions()) {
            rGraph.tx().commit();
        }

        LOG.info("migrationStatus vertex created! v[{}]", migrationStatusId);
    }

    public static MigrationStatus get(Graph graph) {
        MigrationStatus ms = new MigrationStatus();
        try {
            setValues(ms, fetchUsingTypeName(graph.traversal()));
        } catch (Exception ex) {
            if(LOG.isDebugEnabled()) {
                LOG.error("get: failed!", ex);
            }
        }

        return ms;
    }

    private static void setValues(MigrationStatus ms, Vertex vertex) {
        ms.setStartTime((Date) vertex.property(START_TIME_PROPERTY).value());
        ms.setEndTime((Date) vertex.property(END_TIME_PROPERTY).value());
        ms.setCurrentIndex((Long) vertex.property(CURRENT_INDEX_PROPERTY).value());
        ms.setCurrentCounter((Long) vertex.property(CURRENT_COUNTER_PROPERTY).value());
        ms.setOperationStatus((String) vertex.property(OPERATION_STATUS_PROPERTY).value());
        ms.setTotalCount((Long) vertex.property(TOTAL_COUNT_PROPERTY).value());
    }
}
