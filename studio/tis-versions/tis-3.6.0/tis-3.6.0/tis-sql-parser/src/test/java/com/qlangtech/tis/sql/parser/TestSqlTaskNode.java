/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.sql.parser;

import com.google.common.collect.Sets;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.sql.parser.meta.NodeType;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.TableTupleCreator;
import junit.framework.TestCase;
import org.easymock.EasyMock;

import java.util.List;
import java.util.Set;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestSqlTaskNode extends TestCase {

    public void testDumpSqlReflect() {
        final String sql = "select A,b,c as cc,d,e as ee from A";
        Set<String> cols = Sets.newHashSet("a", "b", "c", "d", "e");
        List<ColumnMetaData> rows = SqlTaskNode.reflectTableCols(sql);
        for (ColumnMetaData r : rows) {
            assertTrue(r.getKey() + " must contain in set", cols.contains(r.getKey()));
        }
        assertEquals(5, rows.size());
    }

    public void testParseWithSingleDumpNode() {

        EntityName exportName = EntityName.parse("employees.employee");
        //, NodeType nodetype, IDumpNodeMapContext dumpNodesContext
        IDumpNodeMapContext dumpNodesContext = EasyMock.createMock("dumpNodesContext", IDumpNodeMapContext.class);

        EasyMock.expect(dumpNodesContext.nullableMatch(exportName.getTabName())).andReturn(exportName);

        SqlTaskNode taskNode = new SqlTaskNode(exportName, NodeType.DUMP, dumpNodesContext);
        taskNode.setContent("SELECT e.a,e.b,e.c FROM employee e");
        EasyMock.replay(dumpNodesContext);
        TableTupleCreator tupleCreator = taskNode.parse(true);
        assertNotNull("tupleCreator can not be null", tupleCreator);
        EasyMock.verify(dumpNodesContext);
    }

    public void testDumpNodes() {
        // Map<String, List<TableTupleCreator>> dumpNodes = SqlTaskNode.dumpNodes;
        //
        // List<TableTupleCreator> tables = dumpNodes.get("totalpayinfo");
        // Assert.assertEquals(1, tables.size());
        //
        // Assert.assertEquals("order", tables.get(0).getEntityName().getDbname());
        //
        // tables = dumpNodes.get("payinfo");
        // Assert.assertEquals(1, tables.size());
        //
        // Assert.assertEquals("order", tables.get(0).getEntityName().getDbname());
    }
}
