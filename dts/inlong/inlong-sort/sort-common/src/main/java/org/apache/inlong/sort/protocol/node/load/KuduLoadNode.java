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
import org.apache.commons.lang3.StringUtils;
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
 * The load node of kudu.
 */
@JsonTypeName("kuduLoad")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KuduLoadNode extends LoadNode implements InlongMetric, Serializable {

    private static final long serialVersionUID = -1L;

    public static final String ENABLE_CODE = "true";

    private static final String DDL_ATTR_PREFIX = "ddl.";
    private static final String EXTEND_ATTR_KEY_NAME = "keyName";
    private static final String EXTEND_ATTR_VALUE_NAME = "keyValue";
    public static final String OPTION_TABLE = "table-name";
    private static final String OPTION_MASTERS = "masters";

    @JsonProperty("masters")
    @Nonnull
    private String masters;

    @JsonProperty("tableName")
    @Nonnull
    private String tableName;

    @JsonProperty("partitionKey")
    private String partitionKey;

    @JsonCreator
    public KuduLoadNode(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @JsonProperty("fieldRelations") List<FieldRelation> fieldRelations,
            @JsonProperty("filters") List<FilterFunction> filters,
            @JsonProperty("filterStrategy") FilterStrategy filterStrategy,
            @Nullable @JsonProperty("sinkParallelism") Integer sinkParallelism,
            @JsonProperty("properties") Map<String, String> properties,
            @Nonnull @JsonProperty("masters") String masters,
            @Nonnull @JsonProperty("tableName") String tableName,
            @JsonProperty("partitionKey") String partitionKey) {
        super(id, name, fields, fieldRelations, filters, filterStrategy, sinkParallelism, properties);
        this.tableName = Preconditions.checkNotNull(tableName, "table name is null");
        this.masters = Preconditions.checkNotNull(masters, "masters is null");
        this.partitionKey = partitionKey;
    }

    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> options = super.tableOptions();

        options.put(OPTION_MASTERS, masters);
        options.put(OPTION_TABLE, tableName);

        // If the extend attributes starts with .ddl,
        // it will be passed to the ddl statement of the table
        Map<String, String> properties = getProperties();
        if (properties != null) {
            properties.forEach((keyName, ddlValue) -> {
                if (StringUtils.isNotBlank(keyName) &&
                        keyName.startsWith(DDL_ATTR_PREFIX)) {
                    String ddlKeyName = keyName.substring(DDL_ATTR_PREFIX.length());
                    options.put(ddlKeyName, ddlValue);
                }
            });
        }

        options.put("connector", "kudu-inlong");

        return options;
    }

    @Override
    public String genTableName() {
        return tableName;
    }

    @Override
    public String getPrimaryKey() {
        return null;
    }

    @Override
    public List<FieldInfo> getPartitionFields() {
        return super.getPartitionFields();
    }

}
