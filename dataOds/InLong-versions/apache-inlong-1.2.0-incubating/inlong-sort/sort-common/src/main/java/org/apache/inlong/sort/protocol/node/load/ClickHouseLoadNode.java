/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
import org.apache.inlong.sort.protocol.enums.FilterStrategy;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@JsonTypeName("clickHouseLoad")
@Data
@NoArgsConstructor
public class ClickHouseLoadNode extends LoadNode implements Serializable {

    private static final long serialVersionUID = -1L;

    @JsonProperty("tableName")
    @Nonnull
    private String tableName;

    @JsonProperty("url")
    @Nonnull
    private String url;

    @JsonProperty("userName")
    @Nonnull
    private String userName;

    @JsonProperty("passWord")
    @Nonnull
    private String password;

    @JsonCreator
    public ClickHouseLoadNode(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @JsonProperty("fieldRelations") List<FieldRelation> fieldRelations,
            @JsonProperty("filters") List<FilterFunction> filters,
            @JsonProperty("filterStrategy") FilterStrategy filterStrategy,
            @Nullable @JsonProperty("sinkParallelism") Integer sinkParallelism,
            @JsonProperty("properties") Map<String, String> properties,
            @Nonnull @JsonProperty("tableName") String tableName,
            @Nonnull @JsonProperty("url") String url,
            @Nonnull @JsonProperty("userName") String userName,
            @Nonnull @JsonProperty("passWord") String password) {
        super(id, name, fields, fieldRelations, filters, filterStrategy, sinkParallelism, properties);
        this.tableName = Preconditions.checkNotNull(tableName, "table name is null");
        this.url = Preconditions.checkNotNull(url, "url is null");
        this.userName = Preconditions.checkNotNull(userName, "userName is null");
        this.password = Preconditions.checkNotNull(password, "password is null");
    }

    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> options = super.tableOptions();
        options.put("connector", "jdbc-inlong");
        options.put("dialect-impl", "org.apache.inlong.sort.jdbc.dialect.ClickHouseDialect");
        options.put("url", url);
        options.put("table-name", tableName);
        options.put("username", userName);
        options.put("password", password);
        return options;
    }

    @Override
    public String genTableName() {
        return tableName;
    }

    @Override
    public String getPrimaryKey() {
        return super.getPrimaryKey();
    }

    @Override
    public List<FieldInfo> getPartitionFields() {
        return super.getPartitionFields();
    }

}
