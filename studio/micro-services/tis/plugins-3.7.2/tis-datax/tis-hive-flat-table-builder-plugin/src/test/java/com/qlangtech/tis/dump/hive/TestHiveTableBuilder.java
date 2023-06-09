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

package com.qlangtech.tis.dump.hive;

import com.beust.jcommander.internal.Lists;
import com.qlangtech.tis.datax.TimeFormat;
import com.qlangtech.tis.hive.HdfsFileType;
import com.qlangtech.tis.hive.HdfsFormat;
import com.qlangtech.tis.hive.HiveColumn;
import com.qlangtech.tis.hive.TestHiveInsertFromSelectParser;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.ds.DataSourceMeta;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.junit.Assert;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-04-22 11:28
 **/
public class TestHiveTableBuilder extends TestCase {

    public void testCreateHiveTableAndBindPartition() throws Exception {
        HiveTableBuilder hiveTableBuilder
                = new HiveTableBuilder(TimeFormat.yyyyMMddHHmmss.format(TimeFormat.getCurrentTimeStamp())
                , new HdfsFormat("c", HdfsFileType.ORC));
        List<ColumnMetaData> cols = TestHiveInsertFromSelectParser.createPayInfoCols();
        DataSourceMeta sourceMeta = EasyMock.mock("sourceMeta", DataSourceMeta.class);
        EasyMock.expect(sourceMeta.getEscapeChar()).andReturn(Optional.of("`")).anyTimes();
        for (ColumnMetaData c : cols) {
            EasyMock.expect(sourceMeta.getEscapedEntity(c.getName())).andReturn("`" + c.getName() + "`");
            EasyMock.expect(sourceMeta.removeEscapeChar(c.getName())).andReturn(c.getName());
        }

        DataSourceMeta.JDBCConnection conn = EasyMock.mock("conn", DataSourceMeta.JDBCConnection.class);
        Connection connection = EasyMock.mock("connection", Connection.class);

        Statement statement = EasyMock.mock("statement", Statement.class);
        statement.close();
        EasyMock.expectLastCall().anyTimes();

        EasyMock.expect(statement.execute("CREATE DATABASE IF NOT EXISTS order")).andReturn(true);
        // create table
        EasyMock.expect(statement.execute(EasyMock.anyString())).andReturn(true);

        EasyMock.expect(connection.createStatement()).andReturn(statement).anyTimes();

        EasyMock.expect(conn.getConnection()).andReturn(connection).anyTimes();
        EntityName table = EntityName.parse("order.orderinfo");


        List<HiveColumn> hcs = Lists.newArrayList();
        HiveColumn hc = null;
        for (ColumnMetaData col : cols) {
            hc = new HiveColumn();
            hc.setName(col.getName());
            hc.setDataType(col.getType());
            hc.setIndex(col.getIndex());
            hcs.add(hc);
        }

        EasyMock.replay(sourceMeta, conn, connection, statement);
        StringBuffer hiveSQl = hiveTableBuilder.createHiveTableAndBindPartition(sourceMeta, conn, table, hcs, (sql) -> {
        });

        Assert.assertEquals("CREATE EXTERNAL TABLE IF NOT EXISTS `order`.`orderinfo` (\n" +
                "  `totalpay_id`           STRING,\n" +
                "  `kindpay`               INT,\n" +
                "  `fee`                   DECIMAL(20,0),\n" +
                "  `is_enterprise_card_pay` STRING,\n" +
                "  `pay_customer_ids`      STRING\n" +
                ") COMMENT 'tis_tmp_order.orderinfo' PARTITIONED BY(pt string,pmod string)   STORED AS ORC", hiveSQl.toString());

        EasyMock.verify(sourceMeta, conn, connection, statement);
    }
}
