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

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


@Service
@Qualifier(value = "jdbcSvc")
@CacheConfig(cacheNames = "jdbcHive", keyGenerator = "cacheKeyGenerator")
public class HiveMetaStoreServiceJdbcImpl implements HiveMetaStoreService {

    private static final Logger LOGGER = LoggerFactory
        .getLogger(HiveMetaStoreService.class);

    private static final String SHOW_TABLES_IN = "show tables in ";

    private static final String SHOW_DATABASE = "show databases";

    private static final String SHOW_CREATE_TABLE = "show create table ";

    @Value("${hive.jdbc.className}")
    private String hiveClassName;

    @Value("${hive.jdbc.url}")
    private String hiveUrl;

    @Value("${hive.need.kerberos}")
    private String needKerberos;

    @Value("${hive.keytab.user}")
    private String keytabUser;

    @Value("${hive.keytab.path}")
    private String keytabPath;

    private Connection conn;

    public void setConn(Connection conn) {
        this.conn = conn;
    }

    public void setHiveClassName(String hiveClassName) {
        this.hiveClassName = hiveClassName;
    }

    public void setNeedKerberos(String needKerberos) {
        this.needKerberos = needKerberos;
    }

    public void setKeytabUser(String keytabUser) {
        this.keytabUser = keytabUser;
    }

    public void setKeytabPath(String keytabPath) {
        this.keytabPath = keytabPath;
    }

    @PostConstruct
    public void init() {
        if (needKerberos != null && needKerberos.equalsIgnoreCase("true")) {
            LOGGER.info("Hive need Kerberos Auth.");

            Configuration conf = new Configuration();
            conf.set("hadoop.security.authentication", "Kerberos");
            UserGroupInformation.setConfiguration(conf);
            try {
                UserGroupInformation.loginUserFromKeytab(keytabUser, keytabPath);
            } catch (IOException e) {
                LOGGER.error("Register Kerberos has error. {}", e.getMessage());
            }
        }
    }

    @Override
    @Cacheable(unless = "#result==null")
    public Iterable<String> getAllDatabases() {
        return queryHiveString(SHOW_DATABASE);
    }

    @Override
    @Cacheable(unless = "#result==null")
    public Iterable<String> getAllTableNames(String dbName) {
        return queryHiveString(SHOW_TABLES_IN + dbName);
    }

    @Override
    @Cacheable(unless = "#result==null")
    public Map<String, List<String>> getAllTableNames() {
        // If there has a lots of databases in Hive, this method will lead to Griffin crash
        Map<String, List<String>> res = new HashMap<>();
        for (String dbName : getAllDatabases()) {
            List<String> list = (List<String>) queryHiveString(SHOW_TABLES_IN + dbName);
            res.put(dbName, list);
        }
        return res;
    }

    @Override
    public List<Table> getAllTable(String db) {
        return null;
    }

    @Override
    public Map<String, List<Table>> getAllTable() {
        return null;
    }

    @Override
    @Cacheable(unless = "#result==null")
    public Table getTable(String dbName, String tableName) {
        Table result = new Table();
        result.setDbName(dbName);
        result.setTableName(tableName);

        String sql = SHOW_CREATE_TABLE + dbName + "." + tableName;
        Statement stmt = null;
        ResultSet rs = null;
        StringBuilder sb = new StringBuilder();

        try {
            Class.forName(hiveClassName);
            if (conn == null) {
                conn = DriverManager.getConnection(hiveUrl);
            }
            LOGGER.info("got connection");

            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String s = rs.getString(1);
                sb.append(s);
            }
            String location = getLocation(sb.toString());
            List<FieldSchema> cols = getColums(sb.toString());
            StorageDescriptor sd = new StorageDescriptor();
            sd.setLocation(location);
            sd.setCols(cols);
            result.setSd(sd);
        } catch (Exception e) {
            LOGGER.error("Query Hive Table metadata has error. {}", e.getMessage());
        } finally {
            closeConnection(stmt, rs);
        }
        return result;
    }

    @Scheduled(fixedRateString =
        "${cache.evict.hive.fixedRate.in.milliseconds}")
    @CacheEvict(
        cacheNames = "jdbcHive",
        allEntries = true,
        beforeInvocation = true)
    public void evictHiveCache() {
        LOGGER.info("Evict hive cache");
    }

    /**
     * Query Hive for Show tables or show databases, which will return List of String
     *
     * @param sql sql string
     * @return
     */
    private Iterable<String> queryHiveString(String sql) {
        List<String> res = new ArrayList<>();
        Statement stmt = null;
        ResultSet rs = null;

        try {
            Class.forName(hiveClassName);
            if (conn == null) {
                conn = DriverManager.getConnection(hiveUrl);
            }
            LOGGER.info("got connection");
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                res.add(rs.getString(1));
            }
        } catch (Exception e) {
            LOGGER.error("Query Hive JDBC has error, {}", e.getMessage());
        } finally {
            closeConnection(stmt, rs);
        }
        return res;
    }


    private void closeConnection(Statement stmt, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
                conn = null;
            }
        } catch (SQLException e) {
            LOGGER.error("Close JDBC connection has problem. {}", e.getMessage());
        }
    }

    /**
     * Get the Hive table location from hive table metadata string
     *
     * @param tableMetadata hive table metadata string
     * @return Hive table location
     */
    public String getLocation(String tableMetadata) {
        tableMetadata = tableMetadata.toLowerCase();
        int index = tableMetadata.indexOf("location");
        if (index == -1) {
            return "";
        }

        int start = tableMetadata.indexOf("\'", index);
        int end = tableMetadata.indexOf("\'", start + 1);

        if (start == -1 || end == -1) {
            return "";
        }

        return tableMetadata.substring(start + 1, end);
    }

    /**
     * Get the Hive table schema: column name, column type, column comment
     * The input String looks like following:
     * <p>
     * CREATE TABLE `employee`(
     * `eid` int,
     * `name` string,
     * `salary` string,
     * `destination` string)
     * COMMENT 'Employee details'
     * ROW FORMAT SERDE
     * 'org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe'
     * WITH SERDEPROPERTIES (
     * 'field.delim'='\t',
     * 'line.delim'='\n',
     * 'serialization.format'='\t')
     * STORED AS INPUTFORMAT
     * 'org.apache.hadoop.mapred.TextInputFormat'
     * OUTPUTFORMAT
     * 'org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat'
     * LOCATION
     * 'file:/user/hive/warehouse/employee'
     * TBLPROPERTIES (
     * 'bucketing_version'='2',
     * 'transient_lastDdlTime'='1562086077')
     *
     * @param tableMetadata hive table metadata string
     * @return List of FieldSchema
     */
    public List<FieldSchema> getColums(String tableMetadata) {
        List<FieldSchema> res = new ArrayList<>();
        int start = tableMetadata.indexOf("(") + 1; // index of the first '('
        int end = tableMetadata.indexOf(")", start); // index of the first ')'
        String[] colsArr = tableMetadata.substring(start, end).split(",");
        for (String colStr : colsArr) {
            colStr = colStr.trim();
            String[] parts = colStr.split(" ");
            String colName = parts[0].trim().substring(1, parts[0].trim().length() - 1);
            String colType = parts[1].trim();
            String comment = getComment(colStr);
            FieldSchema schema = new FieldSchema(colName, colType, comment);
            res.add(schema);
        }
        return res;
    }

    /**
     * Parse one column string
     * <p>
     * Input example:
     * `merch_date` string COMMENT 'this is merch process date'
     *
     * @param colStr column string
     * @return
     */
    public String getComment(String colStr) {
        String pattern = "'([^\"|^\']|\"|\')*'";
        Matcher m = Pattern.compile(pattern).matcher(colStr.toLowerCase());
        if (m.find()) {
            String text = m.group();
            String result = text.substring(1, text.length() - 1);
            if (!result.isEmpty()) {
                LOGGER.info("Found value: " + result);
            }
            return result;
        } else {
            LOGGER.info("NO MATCH");
            return "";
        }
    }
}