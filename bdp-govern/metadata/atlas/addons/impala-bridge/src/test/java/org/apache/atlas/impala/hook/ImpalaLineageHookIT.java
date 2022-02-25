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

package org.apache.atlas.impala.hook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.atlas.impala.ImpalaLineageITBase;
import org.apache.atlas.impala.hook.events.BaseImpalaEvent;
import org.apache.atlas.impala.model.ImpalaDependencyType;
import org.apache.atlas.impala.model.ImpalaVertexType;
import org.apache.atlas.impala.model.LineageEdge;
import org.apache.atlas.impala.model.ImpalaQuery;
import org.apache.atlas.impala.model.LineageVertex;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.atlas.impala.hook.events.BaseImpalaEvent.ATTRIBUTE_DDL_QUERIES;
import static org.testng.Assert.assertFalse;

public class ImpalaLineageHookIT extends ImpalaLineageITBase {
    private static final Logger LOG = LoggerFactory.getLogger(ImpalaLineageHookIT.class);
    private static ImpalaLineageHook impalaHook;

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();
        impalaHook = new ImpalaLineageHook();
    }

    @AfterClass
    public void testClean() {
        impalaHook = null;
    }

    @Test
    public void testCreateView() throws Exception {
        // first trigger HMS hook to create related entities
        String dbName = createDatabase();
        assertDatabaseIsRegistered(dbName);

        String tableName = createTable(dbName, "(id string, count int)");
        String viewName = createTable(dbName, "(count int, id string)");

        // then process lineage record to push lineage to Atlas
        ImpalaQuery queryObj = new ImpalaQuery();
        List<LineageEdge> edges = new ArrayList<>();
        List<LineageVertex> vertices = new ArrayList<>();

        queryObj.setQueryText("create view " + viewName + " as select count, id from " + tableName);
        queryObj.setQueryId("3a441d0c130962f8:7f634aec00000000");
        queryObj.setHash("64ff0425ccdfaada53e3f2fd76f566f7");
        queryObj.setUser("admin");
        queryObj.setTimestamp((long)1554750072);
        queryObj.setEndTime((long)1554750554);

        LineageEdge edge1 = new LineageEdge();
        edge1.setSources( Arrays.asList((long)1));
        edge1.setTargets( Arrays.asList((long)0));
        edge1.setEdgeType(ImpalaDependencyType.PROJECTION);
        edges.add(edge1);

        LineageEdge edge2 = new LineageEdge();
        edge2.setSources( Arrays.asList((long)3));
        edge2.setTargets( Arrays.asList((long)2));
        edge2.setEdgeType(ImpalaDependencyType.PROJECTION);
        edges.add(edge2);

        queryObj.setEdges(edges);

        LineageVertex vertex1 = new LineageVertex();
        vertex1.setId((long)0);
        vertex1.setVertexType(ImpalaVertexType.COLUMN);
        vertex1.setVertexId(viewName + ".count");
        vertices.add(vertex1);

        LineageVertex vertex2 = new LineageVertex();
        vertex2.setId((long)1);
        vertex2.setVertexType(ImpalaVertexType.COLUMN);
        vertex2.setVertexId(tableName + ".count");
        vertices.add(vertex2);

        LineageVertex vertex3 = new LineageVertex();
        vertex3.setId((long)2);
        vertex3.setVertexType(ImpalaVertexType.COLUMN);
        vertex3.setVertexId(viewName + ".id");
        vertices.add(vertex3);

        LineageVertex vertex4 = new LineageVertex();
        vertex4.setId((long)3);
        vertex4.setVertexType(ImpalaVertexType.COLUMN);
        vertex4.setVertexId(tableName + ".id");
        vertices.add(vertex4);

        LineageVertex vertex5 = new LineageVertex();
        vertex5.setId((long)4);
        vertex5.setVertexType(ImpalaVertexType.TABLE);
        vertex5.setVertexId(viewName);
        vertex5.setCreateTime(System.currentTimeMillis() / 1000);
        vertices.add(vertex5);

        LineageVertex vertex6 = new LineageVertex();
        vertex6.setId((long)5);
        vertex6.setVertexType(ImpalaVertexType.TABLE);
        vertex6.setVertexId(tableName);
        vertex6.setCreateTime(System.currentTimeMillis() / 1000);
        vertices.add(vertex6);

        queryObj.setVertices(vertices);

        try {
            impalaHook.process(queryObj);
            String createTime = new Long(BaseImpalaEvent.getTableCreateTime(vertex5)).toString();
            String processQFName =
                vertex5.getVertexId() + AtlasImpalaHookContext.QNAME_SEP_METADATA_NAMESPACE +
                    CLUSTER_NAME + AtlasImpalaHookContext.QNAME_SEP_PROCESS + createTime;

            processQFName = processQFName.toLowerCase();

            // check process and process execution entities
            AtlasEntity processEntity1 = validateProcess(processQFName, queryObj.getQueryText());
            AtlasEntity processExecutionEntity1 = validateProcessExecution(processEntity1, queryObj.getQueryText());
            AtlasObjectId process1 = toAtlasObjectId(processExecutionEntity1.getRelationshipAttribute(
                BaseImpalaEvent.ATTRIBUTE_PROCESS));
            Assert.assertEquals(process1.getGuid(), processEntity1.getGuid());
            Assert.assertEquals(numberOfProcessExecutions(processEntity1), 1);

            // check DDL entity
            String viewId = assertTableIsRegistered(viewName);
            AtlasEntity entity  = atlasClientV2.getEntityByGuid(viewId).getEntity();
            List        ddlQueries = (List) entity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

            assertNotNull(ddlQueries);
            assertEquals(ddlQueries.size(), 1);
        } catch (Exception ex) {
            LOG.error("process create_view failed: ", ex);
            assertFalse(true);
        }
    }
}
