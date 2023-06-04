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
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.InlongMetric;
import org.apache.inlong.sort.protocol.constant.Constant;
import org.apache.inlong.sort.protocol.constant.StarRocksConstant;
import org.apache.inlong.sort.protocol.enums.FilterStrategy;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.node.format.Format;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;

/**
 * starrocks load node using flink-connector-starrocks-1.13.5_2.11
 */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("starRocksLoad")
@JsonInclude(Include.NON_NULL)
@Data
@NoArgsConstructor
public class StarRocksLoadNode extends LoadNode implements InlongMetric, Serializable {

    private static final long serialVersionUID = -8002903269814211382L;

    @JsonProperty("jdbc-url")
    @Nonnull
    private String jdbcUrl;

    @JsonProperty("load-url")
    @Nonnull
    private String loadUrl;

    @JsonProperty("username")
    @Nonnull
    private String username;

    @JsonProperty("password")
    @Nonnull
    private String password;

    @JsonProperty("database-name")
    @Nullable
    private String databaseName;

    @JsonProperty("table-name")
    private String tableName;

    @JsonProperty("primaryKey")
    private String primaryKey;

    @Nullable
    @JsonProperty("sinkMultipleEnable")
    private Boolean sinkMultipleEnable = false;

    @Nullable
    @JsonProperty("sinkMultipleFormat")
    private Format sinkMultipleFormat;

    @Nullable
    @JsonProperty("databasePattern")
    private String databasePattern;

    @Nullable
    @JsonProperty("tablePattern")
    private String tablePattern;

    @JsonCreator
    public StarRocksLoadNode(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @JsonProperty("fieldRelations") List<FieldRelation> fieldRelations,
            @JsonProperty("filters") List<FilterFunction> filters,
            @JsonProperty("filterStrategy") FilterStrategy filterStrategy,
            @Nullable @JsonProperty("sinkParallelism") Integer sinkParallelism,
            @JsonProperty("properties") Map<String, String> properties,
            @Nonnull @JsonProperty("jdbc-url") String jdbcUrl,
            @Nonnull @JsonProperty("load-url") String loadUrl,
            @Nonnull @JsonProperty("username") String userName,
            @Nonnull @JsonProperty("password") String password,
            @Nullable @JsonProperty("database-name") String databaseName,
            @JsonProperty("table-name") String tableName,
            @JsonProperty("primaryKey") String primaryKey,
            @Nullable @JsonProperty(value = "sinkMultipleEnable", defaultValue = "false") Boolean sinkMultipleEnable,
            @Nullable @JsonProperty("sinkMultipleFormat") Format sinkMultipleFormat,
            @Nullable @JsonProperty("databasePattern") String databasePattern,
            @Nullable @JsonProperty("tablePattern") String tablePattern) {
        super(id, name, fields, fieldRelations, filters, filterStrategy, sinkParallelism, properties);
        this.jdbcUrl = Preconditions.checkNotNull(jdbcUrl, "jdbc-url is null");
        this.loadUrl = Preconditions.checkNotNull(loadUrl, "load-url is null");
        this.username = Preconditions.checkNotNull(userName, "username is null");
        this.password = Preconditions.checkNotNull(password, "password is null");
        this.databaseName = Preconditions.checkNotNull(databaseName, "database-name is null");
        this.tableName = Preconditions.checkNotNull(tableName, "table-name is null");
        this.primaryKey = primaryKey;
        this.sinkMultipleEnable = sinkMultipleEnable;
        if (sinkMultipleEnable != null && sinkMultipleEnable) {
            this.databasePattern = Preconditions.checkNotNull(databasePattern, "databasePattern is null");
            this.tablePattern = Preconditions.checkNotNull(tablePattern, "tablePattern is null");
            this.sinkMultipleFormat = Preconditions.checkNotNull(sinkMultipleFormat, "sinkMultipleFormat is null");
        }
    }

    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> options = super.tableOptions();
        if (getProperties() != null && !getProperties().isEmpty()) {
            options.putAll(getProperties());
        }
        options.put(StarRocksConstant.CONNECTOR, "starrocks-inlong");
        options.put(StarRocksConstant.JDBC_URL, jdbcUrl);
        options.put(StarRocksConstant.LOAD_URL, loadUrl);
        options.put(StarRocksConstant.USERNAME, username);
        options.put(StarRocksConstant.PASSWORD, password);
        options.put(StarRocksConstant.DATABASE_NAME, databaseName);
        options.put(StarRocksConstant.TABLE_NAME, tableName);
        if (sinkMultipleEnable != null && sinkMultipleEnable) {
            options.put(Constant.SINK_MULTIPLE_ENABLE, sinkMultipleEnable.toString());
            options.put(Constant.SINK_MULTIPLE_FORMAT,
                    Objects.requireNonNull(sinkMultipleFormat).identifier());
            options.put(Constant.SINK_MULTIPLE_DATABASE_PATTERN, databasePattern);
            options.put(Constant.SINK_MULTIPLE_TABLE_PATTERN, tablePattern);
        } else {
            options.put(Constant.SINK_MULTIPLE_ENABLE, "false");
        }
        options.put(StarRocksConstant.FORMAT, "json");
        options.put(StarRocksConstant.STRIP_OUTER_ARRAY, "true");
        return options;
    }

    @Override
    public String genTableName() {
        return String.format("table_%s", super.getId());
    }

    @Override
    public String getPrimaryKey() {
        return primaryKey;
    }
}
