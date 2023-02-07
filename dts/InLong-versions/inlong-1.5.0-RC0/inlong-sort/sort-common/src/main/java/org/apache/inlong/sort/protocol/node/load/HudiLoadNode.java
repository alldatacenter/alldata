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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.InlongMetric;
import org.apache.inlong.sort.protocol.constant.HudiConstant.CatalogType;
import org.apache.inlong.sort.protocol.enums.FilterStrategy;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;

/**
 * The load node of hudi.
 */
@JsonTypeName("hudiLoad")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HudiLoadNode extends LoadNode implements InlongMetric, Serializable {

    private static final long serialVersionUID = -1L;

    public static final String ENABLE_CODE = "true";

    private static final String HUDI_OPTION_HIVE_SYNC_ENABLED = "hive_sync.enabled";
    private static final String HUDI_OPTION_HIVE_SYNC_DB = "hive_sync.db";
    private static final String HUDI_OPTION_HIVE_SYNC_TABLE = "hive_sync.table";
    private static final String HUDI_OPTION_HIVE_SYNC_FILE_FORMAT = "hive_sync.file_format";
    private static final String HUDI_OPTION_HIVE_SYNC_MODE = "hive_sync.mode";
    private static final String HUDI_OPTION_HIVE_SYNC_MODE_HMS_VALUE = "hms";
    private static final String HUDI_OPTION_HIVE_SYNC_METASTORE_URIS = "hive_sync.metastore.uris";
    private static final String HUDI_OPTION_DEFAULT_PATH = "path";
    private static final String HUDI_OPTION_DATABASE_NAME = "hoodie.database.name";
    private static final String HUDI_OPTION_TABLE_NAME = "hoodie.table.name";
    private static final String HUDI_OPTION_RECORD_KEY_FIELD_NAME = "hoodie.datasource.write.recordkey.field";
    private static final String HUDI_OPTION_PARTITION_PATH_FIELD_NAME = "hoodie.datasource.write.partitionpath.field";
    private static final String DDL_ATTR_PREFIX = "ddl.";
    private static final String EXTEND_ATTR_KEY_NAME = "keyName";
    private static final String EXTEND_ATTR_VALUE_NAME = "keyValue";

    @JsonProperty("tableName")
    @Nonnull
    private String tableName;

    @JsonProperty("dbName")
    @Nonnull
    private String dbName;

    @JsonProperty("primaryKey")
    private String primaryKey;

    @JsonProperty("catalogType")
    private CatalogType catalogType;

    @JsonProperty("uri")
    private String uri;

    @JsonProperty("warehouse")
    private String warehouse;

    @JsonProperty("extList")
    private List<HashMap<String, String>> extList;

    @JsonProperty("partitionKey")
    private String partitionKey;

    @JsonCreator
    public HudiLoadNode(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @JsonProperty("fieldRelations") List<FieldRelation> fieldRelations,
            @JsonProperty("filters") List<FilterFunction> filters,
            @JsonProperty("filterStrategy") FilterStrategy filterStrategy,
            @Nullable @JsonProperty("sinkParallelism") Integer sinkParallelism,
            @JsonProperty("properties") Map<String, String> properties,
            @Nonnull @JsonProperty("dbName") String dbName,
            @Nonnull @JsonProperty("tableName") String tableName,
            @JsonProperty("primaryKey") String primaryKey,
            @JsonProperty("catalogType") CatalogType catalogType,
            @JsonProperty("uri") String uri,
            @JsonProperty("warehouse") String warehouse,
            @JsonProperty("extList") List<HashMap<String, String>> extList,
            @JsonProperty("partitionKey") String partitionKey) {
        super(id, name, fields, fieldRelations, filters, filterStrategy, sinkParallelism, properties);
        this.tableName = Preconditions.checkNotNull(tableName, "table name is null");
        this.dbName = Preconditions.checkNotNull(dbName, "db name is null");
        this.primaryKey = primaryKey;
        this.catalogType = catalogType == null ? CatalogType.HIVE : catalogType;
        this.uri = uri;
        this.warehouse = warehouse;
        this.extList = extList;
        this.partitionKey = partitionKey;
    }

    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> options = super.tableOptions();

        // Synchronization to Metastore is enabled by default,
        // which can be modified in the front-end configuration
        options.put(HUDI_OPTION_HIVE_SYNC_ENABLED, ENABLE_CODE);
        options.put(HUDI_OPTION_HIVE_SYNC_MODE, HUDI_OPTION_HIVE_SYNC_MODE_HMS_VALUE);
        options.put(HUDI_OPTION_HIVE_SYNC_DB, dbName);
        options.put(HUDI_OPTION_HIVE_SYNC_TABLE, tableName);
        options.put(HUDI_OPTION_HIVE_SYNC_METASTORE_URIS, uri);

        // partition field
        if (StringUtils.isNoneBlank(partitionKey)) {
            options.put(HUDI_OPTION_PARTITION_PATH_FIELD_NAME, partitionKey);
        }

        // If the extend attributes starts with .ddl,
        // it will be passed to the ddl statement of the table
        extList.forEach(ext -> {
            String keyName = ext.get(EXTEND_ATTR_KEY_NAME);
            if (StringUtils.isNoneBlank(keyName) &&
                    keyName.startsWith(DDL_ATTR_PREFIX)) {
                String ddlKeyName = keyName.substring(DDL_ATTR_PREFIX.length());
                String ddlValue = ext.get(EXTEND_ATTR_VALUE_NAME);
                options.put(ddlKeyName, ddlValue);
            }
        });

        String path = String.format("%s/%s.db/%s", warehouse, dbName, tableName);
        options.put(HUDI_OPTION_DEFAULT_PATH, path);

        options.put(HUDI_OPTION_DATABASE_NAME, dbName);
        options.put(HUDI_OPTION_TABLE_NAME, tableName);
        options.put(HUDI_OPTION_RECORD_KEY_FIELD_NAME, primaryKey);
        options.put("connector", "hudi-inlong");

        return options;
    }

    @Override
    public String genTableName() {
        return tableName;
    }

    @Override
    public String getPrimaryKey() {
        return primaryKey;
    }

    @Override
    public List<FieldInfo> getPartitionFields() {
        return super.getPartitionFields();
    }

}
