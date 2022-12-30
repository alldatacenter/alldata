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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * elasticSearch load node
 */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("elasticsearchLoadNode")
@Data
@NoArgsConstructor
public class ElasticsearchLoadNode extends LoadNode implements InlongMetric, Serializable {

    private static final long serialVersionUID = -1L;

    @JsonProperty("index")
    @Nonnull
    private String index;

    @JsonProperty("hosts")
    @Nonnull
    private String hosts;

    @JsonProperty("username")
    @Nonnull
    private String username;

    @JsonProperty("password")
    @Nonnull
    private String password;

    @JsonProperty("documentType")
    private String documentType;

    @JsonProperty("primaryKey")
    private String primaryKey;

    @JsonProperty("version")
    private int version;

    @JsonCreator
    public ElasticsearchLoadNode(@JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("fields") List<FieldInfo> fields,
        @JsonProperty("fieldRelations") List<FieldRelation> fieldRelationShips,
        @JsonProperty("filters") List<FilterFunction> filters,
        @JsonProperty("filterStrategy") FilterStrategy filterStrategy,
        @Nullable @JsonProperty("sinkParallelism") Integer sinkParallelism,
        @JsonProperty("properties") Map<String, String> properties,
        @Nonnull @JsonProperty("index") String index,
        @Nonnull @JsonProperty("hosts") String hosts,
        @Nonnull @JsonProperty("username") String username,
        @Nonnull @JsonProperty("password") String password,
        @Nonnull @JsonProperty("documentType") String documentType,
        @Nonnull @JsonProperty("primaryKey") String primaryKey,
        @JsonProperty("version") int version) {
        super(id, name, fields, fieldRelationShips, filters, filterStrategy, sinkParallelism, properties);
        this.password = Preconditions.checkNotNull(password, "password is null");
        this.username = Preconditions.checkNotNull(username, "username is null");
        this.hosts = Preconditions.checkNotNull(hosts, "hosts is null");
        this.index = Preconditions.checkNotNull(index, "index is null");
        this.documentType = documentType;
        this.primaryKey = primaryKey;
        this.version = version;
    }

    /**
     * if you want to set field routing, set the routing.field-name
     */
    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> options = super.tableOptions();
        options.put("connector", "elasticsearch-7-inlong");
        if (version == 6) {
            options.put("connector", "elasticsearch-6-inlong");
            options.put("document-type", documentType);
        }
        options.put("hosts", hosts);
        options.put("index", index);
        options.put("password", password);
        options.put("username", username);
        return options;
    }

    @Override
    public String genTableName() {
        return "node_" + super.getId() + "_" + index;
    }

    @Override
    public String getPrimaryKey() {
        return primaryKey;
    }

}
