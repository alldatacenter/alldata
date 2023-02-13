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

package org.apache.inlong.sort.protocol.node.load;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.InlongMetric;
import org.apache.inlong.sort.protocol.enums.FilterStrategy;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Sqlserver load node
 */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("sqlServerLoad")
@Data
@NoArgsConstructor
public class SqlServerLoadNode extends LoadNode implements InlongMetric, Serializable {

    private static final long serialVersionUID = 3083735489161823965L;

    /**
     * jdbc:sqlserver://host:port;databaseName=database
     */
    @JsonProperty("url")
    private String url;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @JsonProperty(value = "schemaName", defaultValue = "dbo")
    private String schemaName;

    @JsonProperty("tableName")
    private String tableName;

    @JsonProperty("primaryKey")
    private String primaryKey;

    @JsonCreator
    public SqlServerLoadNode(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @JsonProperty("fieldRelations") List<FieldRelation> fieldRelations,
            @JsonProperty("filters") List<FilterFunction> filters,
            @JsonProperty("filterStrategy") FilterStrategy filterStrategy,
            @JsonProperty("sinkParallelism") Integer sinkParallelism,
            @JsonProperty("properties") Map<String, String> properties,
            @JsonProperty("url") String url,
            @JsonProperty("username") String username,
            @JsonProperty("password") String password,
            @JsonProperty(value = "schemaName", defaultValue = "dbo") String schemaName,
            @JsonProperty("tableName") String tableName,
            @JsonProperty("primaryKey") String primaryKey) {
        super(id, name, fields, fieldRelations, filters, filterStrategy, sinkParallelism, properties);
        this.url = Preconditions.checkNotNull(url, "sqlserver url is null");
        this.username = Preconditions.checkNotNull(username, "sqlserver user name is null");
        this.password = Preconditions.checkNotNull(password, "sqlserver password is null");
        this.schemaName = schemaName;
        this.tableName = Preconditions.checkNotNull(tableName, "sqlserver table is null");
        this.primaryKey = primaryKey;

    }

    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> options = super.tableOptions();
        options.put("connector", "jdbc-inlong");
        options.put("url", url);
        options.put("username", username);
        options.put("password", password);
        options.put("table-name", schemaName + "." + tableName);
        return options;
    }

    @Override
    public String getPrimaryKey() {
        return primaryKey;
    }

    @Override
    public String genTableName() {
        return String.format("table_%s", super.getId());
    }
}
