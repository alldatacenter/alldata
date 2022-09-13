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
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.common.enums.MetaField;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.InlongMetric;
import org.apache.inlong.sort.protocol.Metadata;
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
 * SqlServer extract node using debezium engine.
 */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("sqlserverExtract")
@Data
public class SqlServerExtractNode extends ExtractNode implements InlongMetric, Metadata, Serializable {

    private static final long serialVersionUID = 5096171152872086083L;

    @JsonProperty("hostname")
    @Nonnull
    private String hostname;

    @JsonProperty("port")
    @Nonnull
    private Integer port;

    @JsonProperty("username")
    @Nonnull
    private String username;

    @JsonProperty("password")
    @Nonnull
    private String password;

    @JsonProperty("database")
    @Nonnull
    private String database;

    @JsonProperty(value = "schemaName", defaultValue = "dbo")
    @Nonnull
    private String schemaName;

    @JsonProperty("tableName")
    @Nonnull
    private String tableName;

    @JsonProperty("serverTimeZone")
    private String serverTimeZone;

    @JsonProperty("primaryKey")
    private String primaryKey;

    @JsonCreator
    public SqlServerExtractNode(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @Nullable @JsonProperty("watermarkField") WatermarkField waterMarkField,
            @JsonProperty("properties") Map<String, String> properties,
            @JsonProperty("primaryKey") String primaryKey,
            @JsonProperty("hostname") @Nonnull String hostname,
            @JsonProperty(value = "port", defaultValue = "1433") Integer port,
            @JsonProperty("username") @Nonnull String username,
            @JsonProperty("password") @Nonnull String password,
            @JsonProperty("database") @Nonnull String database,
            @JsonProperty(value = "schemaName", defaultValue = "dbo") String schemaName,
            @JsonProperty("tableName") @Nonnull String tableName,
            @JsonProperty("serverTimeZone") String serverTimeZone) {
        super(id, name, fields, waterMarkField, properties);
        this.tableName = Preconditions.checkNotNull(tableName, "tableName is null");
        this.hostname = Preconditions.checkNotNull(hostname, "hostname is null");
        this.username = Preconditions.checkNotNull(username, "username is null");
        this.password = Preconditions.checkNotNull(password, "password is null");
        this.database = Preconditions.checkNotNull(database, "database is null");
        this.schemaName = Preconditions.checkNotNull(schemaName, "schema is null");
        this.primaryKey = primaryKey;
        this.port = port;
        this.serverTimeZone = serverTimeZone;
    }

    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> map = super.tableOptions();
        map.put("connector", "sqlserver-cdc-inlong");
        map.put("hostname", hostname);
        map.put("port", port.toString());
        map.put("username", username);
        map.put("password", password);
        map.put("database-name", database);
        map.put("schema-name", schemaName);
        map.put("table-name", tableName);
        if (null != serverTimeZone) {
            map.put("server-time-zone", serverTimeZone);
        }
        return map;
    }

    @Override
    public String genTableName() {
        return String.format("table_%s", super.getId());
    }

    @Override
    public boolean isVirtual(MetaField metaField) {
        return true;
    }

    @Override
    public Set<MetaField> supportedMetaFields() {
        return EnumSet.of(MetaField.PROCESS_TIME, MetaField.TABLE_NAME, MetaField.DATABASE_NAME,
                MetaField.SCHEMA_NAME, MetaField.OP_TS);
    }
}
