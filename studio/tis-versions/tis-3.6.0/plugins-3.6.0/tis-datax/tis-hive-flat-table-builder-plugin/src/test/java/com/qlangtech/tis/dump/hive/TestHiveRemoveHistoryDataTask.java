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

package com.qlangtech.tis.dump.hive;

import com.google.common.collect.Sets;
import com.qlangtech.tis.fs.FSHistoryFileUtils;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.plugin.datax.MREngine;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import junit.framework.TestCase;
import org.easymock.EasyMock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-29 15:07
 **/
public class TestHiveRemoveHistoryDataTask extends TestCase {

    public void testDropHistoryHiveTable() throws Exception {
        String dbName = "testdb";
        String tabName = "order";
        ITISFileSystem fileSystem = EasyMock.createMock("fileSystem", ITISFileSystem.class);
        Connection hiveConn = EasyMock.createMock("hiveConn", Connection.class);

        Statement showDBStatment = EasyMock.createMock("showDBStatment", Statement.class);
        ResultSet resultSet = EasyMock.createMock("resultSet", ResultSet.class);


        EasyMock.expect(showDBStatment.executeQuery("show databases")).andReturn(resultSet);
        EasyMock.expect(resultSet.next()).andReturn(true);
        EasyMock.expect(resultSet.getString(1)).andReturn(dbName);
        EasyMock.expect(resultSet.next()).andReturn(false);

        EasyMock.expect(hiveConn.createStatement()).andReturn(showDBStatment);
        showDBStatment.close();
        resultSet.close();


        // 查询表
        Statement showTabsStatement = EasyMock.createMock("showTabsStatement", Statement.class);
        EasyMock.expect(hiveConn.createStatement()).andReturn(showTabsStatement);
        ResultSet tabsResult = EasyMock.createMock("tabsResult", ResultSet.class);
        EasyMock.expect(tabsResult.next()).andReturn(true);
        EasyMock.expect(tabsResult.getString(1)).andReturn(tabName);
        EasyMock.expect(tabsResult.next()).andReturn(false);
        tabsResult.close();
        EasyMock.expect(showTabsStatement.executeQuery("show tables in testdb")).andReturn(tabsResult);
        showTabsStatement.close();

        String minPt1 = "20210529142016";
        String minPt2 = "20210529152016";

        String retainPt1 = "20210529162016";
        String retainPt2 = "20210529172016";
        Set<String> removePts = Sets.newHashSet(minPt1, minPt2);

        Statement showPartitionsStatement = EasyMock.createMock("showPartitionsStatement", Statement.class);
        ResultSet showPartitionsResult = EasyMock.createMock("showPartitionsResultSet", ResultSet.class);
        EasyMock.expect(hiveConn.createStatement()).andReturn(showPartitionsStatement);
        EasyMock.expect(showPartitionsStatement.executeQuery("show partitions testdb.order")).andReturn(showPartitionsResult);
        EasyMock.expect(showPartitionsResult.next()).andReturn(true);
        EasyMock.expect(showPartitionsResult.getString(1)).andReturn("pt=" + minPt1);

        EasyMock.expect(showPartitionsResult.next()).andReturn(true);
        EasyMock.expect(showPartitionsResult.getString(1)).andReturn("pt=" + minPt2);

        EasyMock.expect(showPartitionsResult.next()).andReturn(true);
        EasyMock.expect(showPartitionsResult.getString(1)).andReturn("pt=" + retainPt1);

        EasyMock.expect(showPartitionsResult.next()).andReturn(true);
        EasyMock.expect(showPartitionsResult.getString(1)).andReturn("pt=" + retainPt2);
        EasyMock.expect(showPartitionsResult.next()).andReturn(false);
        showPartitionsResult.close();
        showPartitionsStatement.close();

        //====================================
        Statement dropPtStatement2 = EasyMock.createMock("dropStatement_" + minPt2, Statement.class);
        EasyMock.expect(dropPtStatement2.execute("alter table testdb.order drop partition (  pt = '" + minPt2 + "' )")).andReturn(true);
        EasyMock.expect(hiveConn.createStatement()).andReturn(dropPtStatement2);
        dropPtStatement2.close();


        Statement dropPtStatement1 = EasyMock.createMock("dropStatement_" + minPt1, Statement.class);
        EasyMock.expect(dropPtStatement1.execute("alter table testdb.order drop partition (  pt = '" + minPt1 + "' )")).andReturn(true);
        EasyMock.expect(hiveConn.createStatement()).andReturn(dropPtStatement1);
        dropPtStatement1.close();


        int partitionRetainNum = 2;
        EntityName tabOrder = EntityName.create(dbName, tabName);
        //hiveConn.close();
        EasyMock.replay(fileSystem, hiveConn, showDBStatment, resultSet, showTabsStatement, tabsResult, showPartitionsStatement, showPartitionsResult, dropPtStatement1, dropPtStatement2);
        List<FSHistoryFileUtils.PathInfo> deletePts =
                (new HiveRemoveHistoryDataTask(fileSystem, MREngine.HIVE)).dropHistoryHiveTable(tabOrder, hiveConn, partitionRetainNum);

        assertEquals(2, deletePts.size());
        deletePts.forEach((pt) -> {
            assertTrue("removed partition shall exist:" + pt.getPathName(), removePts.contains(pt.getPathName()));
        });
        EasyMock.verify(fileSystem, hiveConn, showDBStatment, resultSet, showTabsStatement, tabsResult, showPartitionsStatement, showPartitionsResult, dropPtStatement1, dropPtStatement2);
    }
}
