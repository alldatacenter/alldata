/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.metastore.hive;


import org.apache.griffin.core.config.CacheConfig;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Table;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.when;


@RunWith(SpringRunner.class)
public class HiveMetastoreServiceJDBCImplTest {

    @TestConfiguration
    @EnableCaching
    public static class HiveMetaStoreServiceConfiguration extends CacheConfig {
        @Bean("hiveMetaStoreServiceJdbcImpl")
        public HiveMetaStoreServiceJdbcImpl serviceJDBC() {
            return new HiveMetaStoreServiceJdbcImpl();
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("jdbcHive");
        }
    }

    private HiveMetaStoreServiceJdbcImpl serviceJdbc = new HiveMetaStoreServiceJdbcImpl();

    @Mock
    private Connection conn;

    @Mock
    private Statement stmt;

    @Mock
    private ResultSet rs;

    @Before
    public void setUp() throws SQLException {
        serviceJdbc.setConn(conn);
        serviceJdbc.setHiveClassName("org.apache.hive.jdbc.HiveDriver");
        serviceJdbc.setNeedKerberos("true");
        serviceJdbc.setKeytabPath("/path/to/keytab");
        serviceJdbc.setKeytabUser("user");
    }

    @Test
    public void testGetComment() {
        String colStr = "`session_date` string COMMENT 'this is session date'";
        String comment = serviceJdbc.getComment(colStr);
        assert (comment.equals("this is session date"));

        colStr = "`session_date` string COMMENT ''";
        comment = serviceJdbc.getComment(colStr);
        Assert.assertTrue(comment.isEmpty());
    }

    @Test
    public void testgetAllDatabases() throws SQLException {
        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(true).thenReturn(false);
        when(rs.getString(anyInt())).thenReturn("default");

        Iterable<String> res = serviceJdbc.getAllDatabases();
        for (String s : res) {
            Assert.assertEquals(s, "default");
            break;
        }
    }

    @Test
    public void testGetAllTableNames() throws SQLException {
        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(rs.getString(anyInt())).thenReturn("session_data").thenReturn("session_summary");

        Iterable<String> res = serviceJdbc.getAllTableNames("default");
        StringBuilder sb = new StringBuilder();
        for (String s : res) {
            sb.append(s).append(",");
        }
        Assert.assertEquals(sb.toString(), "session_data,session_summary,");
    }

    @Test
    public void testGetTable() throws SQLException {
        String meta = "CREATE EXTERNAL TABLE `default.session_data`(  `session_date` string COMMENT 'this is session date',   `site_id` int COMMENT '',   `guid` string COMMENT '',   `user_id` string COMMENT '')COMMENT 'session_data for session team' PARTITIONED BY (   `dt` string,   `place` int) ROW FORMAT SERDE   'org.apache.hadoop.hive.serde2.avro.AvroSerDe' STORED AS INPUTFORMAT   'org.apache.hadoop.hive.ql.io.avro.AvroContainerInputFormat' OUTPUTFORMAT   'org.apache.hadoop.hive.ql.io.avro.AvroContainerOutputFormat' LOCATION 'hdfs://localhost/session/common/session_data'TBLPROPERTIES (  'COLUMN_STATS_ACCURATE'='false',   'avro.schema.url'='hdfs://localhost/griffin/session/avro/session-data-1.0.avsc',   'transient_lastDdlTime'='1535651637')";
        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(true).thenReturn(false);
        when(rs.getString(anyInt())).thenReturn(meta);

        Table res = serviceJdbc.getTable("default", "session_data");

        assert (res.getDbName().equals("default"));
        assert (res.getTableName().equals("session_data"));
        assert (res.getSd().getLocation().equals("hdfs://localhost/session/common/session_data"));
        List<FieldSchema> fieldSchemas = res.getSd().getCols();
        for (FieldSchema fieldSchema : fieldSchemas) {
            Assert.assertEquals(fieldSchema.getName(),"session_date");
            Assert.assertEquals(fieldSchema.getType(),"string");
            Assert.assertEquals(fieldSchema.getComment(),"this is session date");
            break;
        }
    }
}