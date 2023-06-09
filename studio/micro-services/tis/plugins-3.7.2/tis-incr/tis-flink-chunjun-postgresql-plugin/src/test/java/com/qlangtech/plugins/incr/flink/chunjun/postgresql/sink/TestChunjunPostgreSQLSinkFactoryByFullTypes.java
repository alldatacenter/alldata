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

package com.qlangtech.plugins.incr.flink.chunjun.postgresql.sink;

import com.google.common.collect.Lists;
import com.qlangtech.plugins.incr.flink.sink.TestFlinkSinkExecutorByMySQLFullTypes;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.plugin.datax.DataXPostgresqlWriter;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsWriter;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.postgresql.PGDataSourceFactory;
import com.qlangtech.tis.plugins.incr.flink.connector.ChunjunSinkFactory;
import com.qlangtech.tis.plugins.incr.flink.connector.UpdateMode;
import com.qlangtech.tis.plugins.incr.flink.connector.impl.UpsertType;
import com.ververica.cdc.connectors.postgres.PostgresTestBase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.*;
import java.util.ArrayList;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-22 20:35
 **/
public class TestChunjunPostgreSQLSinkFactoryByFullTypes extends TestFlinkSinkExecutorByMySQLFullTypes {
    static BasicDataSourceFactory pgDSFactory;

    @BeforeClass
    public static void initialize() throws Exception {
        PostgresTestBase.startContainers();
        pgDSFactory = (BasicDataSourceFactory) PostgresTestBase.createPgSourceFactory(new TargetResName(dataXName));
    }

    @Override
    protected CMeta createUpdateTime() {
        CMeta cm = super.createUpdateTime();
        cm.setPk(false);
        return cm;
    }

    @Override
    protected BasicDataSourceFactory getDsFactory() {
        return pgDSFactory;
    }

    @Override
    protected UpdateMode createIncrMode() {
        UpsertType upsert = new UpsertType();
        //  updateMode.updateKey = Lists.newArrayList(colId, updateTime);
        return upsert;
    }

    @Override
    protected ArrayList<String> getUniqueKey() {
        return Lists.newArrayList(colId);
    }

    @Override
    protected ChunjunSinkFactory getSinkFactory() {
        return new ChunjunPostgreSQLSinkFactory();
    }

    @Override
    protected BasicDataXRdbmsWriter createDataXWriter() {

        DataXPostgresqlWriter pgDataXWriter = new DataXPostgresqlWriter() {
            @Override
            public PGDataSourceFactory getDataSourceFactory() {
                return (PGDataSourceFactory) pgDSFactory;
            }
        };
        pgDataXWriter.autoCreateTable = true;
        // pgDataXWriter.generateCreateDDL()
        return pgDataXWriter;
    }

    @Test
    public void testBitCol() {

        pgDSFactory.visitFirstConnection((c) -> {
            Connection conn = c.getConnection();
            Statement statement = conn.createStatement();

            statement.execute("CREATE TABLE \"public\".\"full_types_bit\"\n" +
                    "(\n" +
                    "    \"id\"              INTEGER PRIMARY KEY,\n" +
                    "    \"bit1_c\"          BIT,\n" +
                    "    \"tiny1_c\"         BIT,\n" +
                    "    \"boolean_c\"       BIT\n" +
                    ")");
            statement.close();
            // PG中如果是bit类型 比较特殊设置
            org.postgresql.util.PGobject obj = new org.postgresql.util.PGobject();
            obj.setType("bit");
            obj.setValue("1");
            PreparedStatement s = conn.prepareStatement("insert into \"public\".\"full_types_bit\"(id,bit1_c)values(?,?)");
            s.setInt(1, 1);
            s.setObject(2, obj);

            s.executeUpdate();

            Statement s2 = conn.createStatement();
            ResultSet resultSet = s2.executeQuery("select * from \"public\".\"full_types_bit\"");
            while (resultSet.next()) {
                System.out.println(resultSet.getInt(1));
                System.out.println(resultSet.getByte(2));
            }

        });
    }

    @Test
    @Override
    public void testSinkSync() throws Exception {
        super.testSinkSync();
    }

    @Override
    protected void assertResultSetFromStore(ResultSet resultSet) throws SQLException {
        Assert.assertEquals(2, resultSet.getInt("id"));
    }
}
