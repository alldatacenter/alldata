/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.formats.json.canal;

import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
public class CanalJson {

    private List<Map<String, Object>> data;
    private long es;
    private String table;
    private String type;
    private String database;
    private long ts;
    private String sql;
    private Map<String, String> mysqlType;
    private Map<String, Integer> sqlType;

    private boolean isDdl;
    private List<String> pkNames;
    private String schema;
    private Map<String, String> oracleType;

    public List<Map<String, Object>> getData() {
        return data;
    }

    public void setData(List<Map<String, Object>> data) {
        this.data = data;
    }

    public long getEs() {
        return es;
    }

    public void setEs(long es) {
        this.es = es;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public Map<String, String> getMysqlType() {
        return mysqlType;
    }

    public void setMysqlType(Map<String, String> mysqlType) {
        this.mysqlType = mysqlType;
    }

    public void setDdl(boolean ddl) {
        isDdl = ddl;
    }

    public List<String> getPkNames() {
        return pkNames;
    }

    public void setPkNames(List<String> pkNames) {
        this.pkNames = pkNames;
    }

    public Map<String, Integer> getSqlType() {
        return sqlType;
    }

    public void setSqlType(Map<String, Integer> sqlType) {
        this.sqlType = sqlType;
    }

    public boolean isDdl() {
        return isDdl;
    }

    public Map<String, String> getOracleType() {
        return oracleType;
    }

    public void setOracleType(Map<String, String> oracleType) {
        this.oracleType = oracleType;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

}
