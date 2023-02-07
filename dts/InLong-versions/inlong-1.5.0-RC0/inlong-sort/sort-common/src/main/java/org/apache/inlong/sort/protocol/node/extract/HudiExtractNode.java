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

import static org.apache.inlong.sort.protocol.constant.HudiConstant.CONNECTOR;
import static org.apache.inlong.sort.protocol.constant.HudiConstant.CONNECTOR_KEY;
import static org.apache.inlong.sort.protocol.constant.HudiConstant.DDL_ATTR_PREFIX;
import static org.apache.inlong.sort.protocol.constant.HudiConstant.ENABLE_CODE;
import static org.apache.inlong.sort.protocol.constant.HudiConstant.EXTEND_ATTR_KEY_NAME;
import static org.apache.inlong.sort.protocol.constant.HudiConstant.EXTEND_ATTR_VALUE_NAME;
import static org.apache.inlong.sort.protocol.constant.HudiConstant.HUDI_OPTION_DATABASE_NAME;
import static org.apache.inlong.sort.protocol.constant.HudiConstant.HUDI_OPTION_DEFAULT_PATH;
import static org.apache.inlong.sort.protocol.constant.HudiConstant.HUDI_OPTION_HIVE_SYNC_DB;
import static org.apache.inlong.sort.protocol.constant.HudiConstant.HUDI_OPTION_HIVE_SYNC_ENABLED;
import static org.apache.inlong.sort.protocol.constant.HudiConstant.HUDI_OPTION_HIVE_SYNC_METASTORE_URIS;
import static org.apache.inlong.sort.protocol.constant.HudiConstant.HUDI_OPTION_HIVE_SYNC_MODE;
import static org.apache.inlong.sort.protocol.constant.HudiConstant.HUDI_OPTION_HIVE_SYNC_MODE_HMS_VALUE;
import static org.apache.inlong.sort.protocol.constant.HudiConstant.HUDI_OPTION_HIVE_SYNC_TABLE;
import static org.apache.inlong.sort.protocol.constant.HudiConstant.HUDI_OPTION_TABLE_NAME;
import static org.apache.inlong.sort.protocol.constant.HudiConstant.READ_AS_STREAMING;
import static org.apache.inlong.sort.protocol.constant.HudiConstant.READ_START_COMMIT;
import static org.apache.inlong.sort.protocol.constant.HudiConstant.READ_STREAMING_CHECK_INTERVAL;
import static org.apache.inlong.sort.protocol.constant.HudiConstant.READ_STREAMING_SKIP_COMPACT;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.constant.HudiConstant.CatalogType;
import org.apache.inlong.sort.protocol.node.ExtractNode;
import org.apache.inlong.sort.protocol.transformation.WatermarkField;

/**
 * Hudi extract node for extract data from hudi
 */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("hudiExtract")
@JsonInclude(Include.NON_NULL)
@Data
public class HudiExtractNode extends ExtractNode implements Serializable {

    @JsonProperty("tableName")
    @Nonnull
    private String tableName;

    @JsonProperty("dbName")
    @Nonnull
    private String dbName;

    @JsonProperty("catalogType")
    private CatalogType catalogType;

    @JsonProperty("uri")
    private String uri;

    @JsonProperty("warehouse")
    private String warehouse;

    @JsonProperty("readStreamingSkipCompaction")
    private boolean readStreamingSkipCompaction;

    @JsonProperty("readStartCommit")
    private String readStartCommit;

    @JsonProperty("extList")
    private List<HashMap<String, String>> extList;

    private static final long serialVersionUID = 1L;
    private int checkIntervalInMinus;

    public HudiExtractNode(
            @Nonnull @JsonProperty("id") String id,
            @Nonnull @JsonProperty("name") String name,
            @Nonnull @JsonProperty("fields") List<FieldInfo> fields,
            @Nullable @JsonProperty("watermarkField") WatermarkField watermarkField,
            @Nullable @JsonProperty("uri") String uri,
            @Nullable @JsonProperty("warehouse") String warehouse,
            @Nonnull @JsonProperty("dbName") String dbName,
            @Nonnull @JsonProperty("tableName") String tableName,
            @JsonProperty("catalogType") CatalogType catalogType,
            @JsonProperty("checkIntervalInMinus") int checkIntervalInMinus,
            @JsonProperty("readStreamingSkipCompaction") boolean readStreamingSkipCompaction,
            @JsonProperty("readStartCommit") String readStartCommit,
            @Nullable @JsonProperty("properties") Map<String, String> properties,
            @JsonProperty("extList") List<HashMap<String, String>> extList) {
        super(id, name, fields, watermarkField, properties);

        this.tableName = Preconditions.checkNotNull(tableName, "table name is null");
        this.dbName = Preconditions.checkNotNull(dbName, "db name is null");
        this.catalogType = catalogType == null ? CatalogType.HIVE : catalogType;
        this.uri = uri;
        this.warehouse = warehouse;
        this.readStreamingSkipCompaction = readStreamingSkipCompaction;
        this.readStartCommit = readStartCommit;
        this.extList = extList;
        this.checkIntervalInMinus = checkIntervalInMinus;
    }

    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> options = super.tableOptions();

        options.put(READ_AS_STREAMING, ENABLE_CODE);
        options.put(READ_STREAMING_CHECK_INTERVAL, String.valueOf(checkIntervalInMinus));

        // Synchronization to Metastore is enabled by default,
        // which can be modified in the front-end configuration
        options.put(HUDI_OPTION_HIVE_SYNC_ENABLED, ENABLE_CODE);
        options.put(HUDI_OPTION_HIVE_SYNC_MODE, HUDI_OPTION_HIVE_SYNC_MODE_HMS_VALUE);
        options.put(HUDI_OPTION_HIVE_SYNC_DB, dbName);
        options.put(HUDI_OPTION_HIVE_SYNC_TABLE, tableName);
        options.put(HUDI_OPTION_HIVE_SYNC_METASTORE_URIS, uri);

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

        // read options
        options.put(READ_START_COMMIT, String.valueOf(readStreamingSkipCompaction));
        options.put(READ_STREAMING_SKIP_COMPACT, readStartCommit);

        options.put(HUDI_OPTION_DATABASE_NAME, dbName);
        options.put(HUDI_OPTION_TABLE_NAME, tableName);
        options.put(CONNECTOR_KEY, CONNECTOR);

        return options;
    }

    @Override
    public String genTableName() {
        return String.format("hudi_table_%s", getId());
    }
}
