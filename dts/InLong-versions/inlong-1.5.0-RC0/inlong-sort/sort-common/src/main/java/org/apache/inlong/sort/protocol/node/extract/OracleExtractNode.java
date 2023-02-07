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

package org.apache.inlong.sort.protocol.node.extract;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.common.enums.MetaField;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.InlongMetric;
import org.apache.inlong.sort.protocol.Metadata;
import org.apache.inlong.sort.protocol.constant.OracleConstant;
import org.apache.inlong.sort.protocol.node.ExtractNode;
import org.apache.inlong.sort.protocol.transformation.WatermarkField;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Oracle extract node for extract data from oracle(Currently support oracle 11,12,19)
 */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("oracleExtract")
@Data
public class OracleExtractNode extends ExtractNode implements InlongMetric, Metadata, Serializable {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Nullable
    @JsonProperty("primaryKey")
    private String primaryKey;
    @Nonnull
    @JsonProperty("hostname")
    private String hostname;
    @Nonnull
    @JsonProperty("username")
    private String username;
    @Nonnull
    @JsonProperty("password")
    private String password;
    @Nonnull
    @JsonProperty("database")
    private String database;
    @Nonnull
    @JsonProperty("schemaName")
    private String schemaName;
    @Nonnull
    @JsonProperty("tableName")
    private String tableName;
    @JsonProperty(value = "port", defaultValue = "1521")
    private Integer port;
    @Nullable
    @JsonProperty("scanStartupMode")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private OracleConstant.ScanStartUpMode scanStartupMode;

    @JsonCreator
    public OracleExtractNode(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @Nullable @JsonProperty("watermark_field") WatermarkField watermarkField,
            @JsonProperty("properties") Map<String, String> properties,
            @Nullable @JsonProperty("primaryKey") String primaryKey,
            @JsonProperty("hostname") String hostname,
            @JsonProperty("username") String username,
            @JsonProperty("password") String password,
            @JsonProperty("database") String database,
            @JsonProperty("schemaName") String schemaName,
            @JsonProperty("tableName") String tableName,
            @JsonProperty(value = "port", defaultValue = "1521") Integer port,
            @Nullable @JsonProperty("scanStartupMode") OracleConstant.ScanStartUpMode scanStartupMode) {
        super(id, name, fields, watermarkField, properties);
        this.primaryKey = primaryKey;
        this.hostname = Preconditions.checkNotNull(hostname, "hostname is null");
        this.username = Preconditions.checkNotNull(username, "username is null");
        this.password = Preconditions.checkNotNull(password, "password is null");
        this.database = Preconditions.checkNotNull(database, "database is null");
        this.schemaName = Preconditions.checkNotNull(schemaName, "schemaName is null");
        this.tableName = Preconditions.checkNotNull(tableName, "tableName is null");
        this.port = port;
        this.scanStartupMode = scanStartupMode;
    }

    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> options = super.tableOptions();
        options.put(OracleConstant.CONNECTOR, OracleConstant.ORACLE_CDC);
        options.put(OracleConstant.HOSTNAME, hostname);
        options.put(OracleConstant.USERNAME, username);
        options.put(OracleConstant.PASSWORD, password);
        options.put(OracleConstant.DATABASE_NAME, database);
        options.put(OracleConstant.SCHEMA_NAME, schemaName);
        options.put(OracleConstant.TABLE_NAME, tableName);
        if (!options.containsKey(OracleConstant.TABLENAME_CASE_INSENSITIVE)) {
            // Set a default value:false to avoid retrieving unlisted tables in oracle 11g.
            // You can set null to replace it if your oracle version is more than oracle 11g.
            options.put(OracleConstant.TABLENAME_CASE_INSENSITIVE, "false");
        }
        if (!options.containsKey(OracleConstant.LOG_MINING_STRATEGY)) {
            // Set a default value:online_catalog to improve performance and reduce latency
            options.put(OracleConstant.LOG_MINING_STRATEGY, "online_catalog");
        }
        if (!options.containsKey(OracleConstant.LOG_MINING_CONTINUOUS_MINE)) {
            // Set a default value:true to improve performance and reduce latency
            options.put(OracleConstant.LOG_MINING_CONTINUOUS_MINE, "true");
        }
        if (port != null) {
            options.put(OracleConstant.PORT, port.toString());
        }
        if (scanStartupMode != null) {
            options.put(OracleConstant.SCAN_STARTUP_MODE, scanStartupMode.getValue());
        }
        return options;
    }

    @Override
    public String genTableName() {
        return String.format("node_%s", super.getId());
    }

    @Override
    public boolean isVirtual(MetaField metaField) {
        return true;
    }

    @Override
    public Set<MetaField> supportedMetaFields() {
        return EnumSet.of(MetaField.PROCESS_TIME, MetaField.TABLE_NAME, MetaField.DATABASE_NAME,
                MetaField.SCHEMA_NAME, MetaField.OP_TS, MetaField.OP_TYPE, MetaField.DATA, MetaField.DATA_BYTES,
                MetaField.DATA_CANAL, MetaField.DATA_BYTES_CANAL, MetaField.IS_DDL, MetaField.TS,
                MetaField.SQL_TYPE, MetaField.ORACLE_TYPE, MetaField.PK_NAMES);
    }

    @Override
    public String getMetadataKey(MetaField metaField) {
        String metadataKey;
        switch (metaField) {
            case TABLE_NAME:
                metadataKey = "meta.table_name";
                break;
            case DATABASE_NAME:
                metadataKey = "meta.database_name";
                break;
            case SCHEMA_NAME:
                metadataKey = "meta.schema_name";
                break;
            case OP_TS:
                metadataKey = "meta.op_ts";
                break;
            case OP_TYPE:
                metadataKey = "meta.op_type";
                break;
            case DATA:
            case DATA_BYTES:
                metadataKey = "meta.data";
                break;
            case DATA_CANAL:
            case DATA_BYTES_CANAL:
                metadataKey = "meta.data_canal";
                break;
            case IS_DDL:
                metadataKey = "meta.is_ddl";
                break;
            case TS:
                metadataKey = "meta.ts";
                break;
            case SQL_TYPE:
                metadataKey = "meta.sql_type";
                break;
            case ORACLE_TYPE:
                metadataKey = "meta.oracle_type";
                break;
            case PK_NAMES:
                metadataKey = "meta.pk_names";
                break;
            default:
                throw new UnsupportedOperationException(String.format("Unsupport meta field for %s: %s",
                        this.getClass().getSimpleName(), metaField));
        }
        return metadataKey;
    }

}
