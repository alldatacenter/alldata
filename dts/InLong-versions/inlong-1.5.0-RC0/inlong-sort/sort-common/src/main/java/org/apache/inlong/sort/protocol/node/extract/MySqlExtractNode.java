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
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.common.enums.MetaField;
import org.apache.inlong.common.enums.RowKindEnum;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.InlongMetric;
import org.apache.inlong.sort.protocol.Metadata;
import org.apache.inlong.sort.protocol.enums.ExtractMode;
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
 * Mysql extract node using debezium engine
 */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("mysqlExtract")
@JsonInclude(Include.NON_NULL)
@Data
public class MySqlExtractNode extends ExtractNode implements Metadata, InlongMetric, Serializable {

    private static final long serialVersionUID = -5521981462461235277L;

    @JsonProperty("primaryKey")
    private String primaryKey;
    @JsonProperty("tableNames")
    @Nonnull
    private List<String> tableNames;
    @JsonProperty("hostname")
    private String hostname;
    @JsonProperty("username")
    private String username;
    @JsonProperty("password")
    private String password;
    @JsonProperty("database")
    private String database;
    @JsonProperty("port")
    private Integer port;
    @JsonProperty("serverId")
    private Integer serverId;
    @JsonProperty("incrementalSnapshotEnabled")
    private Boolean incrementalSnapshotEnabled;
    @JsonProperty("serverTimeZone")
    private String serverTimeZone;
    @Nonnull
    @JsonProperty("extractMode")
    private ExtractMode extractMode;
    @JsonProperty("url")
    private String url;
    @JsonProperty("rowKindsFiltered")
    private List<RowKindEnum> rowKindsFiltered;

    /**
     * Constructor only used for {@link ExtractMode#CDC}
     *
     * @param id The id of node
     * @param name The name of node
     * @param fields The field list of node
     * @param watermarkField The watermark field of node only used for {@link ExtractMode#CDC}
     * @param properties The properties connect to mysql
     * @param primaryKey The primark key connect to mysql
     * @param tableNames The table names connect to mysql
     * @param hostname The hostname connect to mysql only used for {@link ExtractMode#CDC}
     * @param username The username connect to mysql
     * @param password The password connect to mysql
     * @param database The database connect to mysql only used for {@link ExtractMode#CDC}
     * @param port The port connect to mysql only used for {@link ExtractMode#CDC}
     * @param serverId The server id connect to mysql only used for {@link ExtractMode#CDC}
     * @param incrementalSnapshotEnabled The incremental snapshot enabled connect to mysql
     *         only used for {@link ExtractMode#CDC}
     * @param serverTimeZone The server time zone connect to mysql only used for {@link ExtractMode#CDC}
     */
    public MySqlExtractNode(@Nonnull @JsonProperty("id") String id,
            @Nonnull @JsonProperty("name") String name,
            @Nonnull @JsonProperty("fields") List<FieldInfo> fields,
            @Nullable @JsonProperty("watermarkField") WatermarkField watermarkField,
            @Nullable @JsonProperty("properties") Map<String, String> properties,
            @Nullable @JsonProperty("primaryKey") String primaryKey,
            @Nonnull @JsonProperty("tableNames") List<String> tableNames,
            @Nonnull @JsonProperty("hostname") String hostname,
            @Nonnull @JsonProperty("username") String username,
            @Nonnull @JsonProperty("password") String password,
            @Nonnull @JsonProperty("database") String database,
            @Nullable @JsonProperty("port") Integer port,
            @Nullable @JsonProperty("serverId") Integer serverId,
            @Nullable @JsonProperty("incrementalSnapshotEnabled") Boolean incrementalSnapshotEnabled,
            @Nullable @JsonProperty("serverTimeZone") String serverTimeZone) {
        this(id, name, fields, watermarkField, properties, primaryKey, tableNames, hostname, username, password,
                database, port, serverId, incrementalSnapshotEnabled, serverTimeZone, ExtractMode.CDC, null, null);
    }

    /**
     * Constructor only used for {@link ExtractMode#SCAN}
     *
     * @param id The id of node
     * @param name The name of node
     * @param fields The field list of node
     * @param properties The properties connect to mysql
     * @param primaryKey The primark key connect to mysql
     * @param tableNames The table names connect to mysql
     * @param username The username connect to mysql
     * @param password The password connect to mysql
     * @param url The url connect to mysql only used for {@link ExtractMode#SCAN}
     */
    public MySqlExtractNode(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @Nullable @JsonProperty("properties") Map<String, String> properties,
            @Nullable @JsonProperty("primaryKey") String primaryKey,
            @JsonProperty("tableNames") @Nonnull List<String> tableNames,
            @JsonProperty("username") String username,
            @JsonProperty("password") String password,
            @Nullable @JsonProperty("url") String url) {
        this(id, name, fields, null, properties, primaryKey, tableNames, null, username,
                password, null, null, null, null, null,
                ExtractMode.SCAN, url, null);
    }

    /**
     * Base constructor
     *
     * @param id The id of node
     * @param name The name of node
     * @param fields The field list of node
     * @param watermarkField The watermark field of node only used for {@link ExtractMode#CDC}
     * @param properties The properties connect to mysql
     * @param primaryKey The primark key connect to mysql
     * @param tableNames The table names connect to mysql
     * @param hostname The hostname connect to mysql only used for {@link ExtractMode#CDC}
     * @param username The username connect to mysql
     * @param password The password connect to mysql
     * @param database The database connect to mysql only used for {@link ExtractMode#CDC}
     * @param port The port connect to mysql only used for {@link ExtractMode#CDC}
     * @param serverId The server id connect to mysql only used for {@link ExtractMode#CDC}
     * @param incrementalSnapshotEnabled The incremental snapshot enabled connect to mysql
     *         only used for {@link ExtractMode#CDC}
     * @param serverTimeZone The server time zone connect to mysql only used for {@link ExtractMode#CDC}
     * @param extractMode The extract mode connect mysql {@link ExtractMode}
     * @param url The url connect to mysql only used for {@link ExtractMode#SCAN}
     */
    @JsonCreator
    public MySqlExtractNode(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @Nullable @JsonProperty("watermarkField") WatermarkField watermarkField,
            @Nullable @JsonProperty("properties") Map<String, String> properties,
            @Nullable @JsonProperty("primaryKey") String primaryKey,
            @JsonProperty("tableNames") @Nonnull List<String> tableNames,
            @Nullable @JsonProperty("hostname") String hostname,
            @JsonProperty("username") String username,
            @JsonProperty("password") String password,
            @Nullable @JsonProperty("database") String database,
            @Nullable @JsonProperty("port") Integer port,
            @Nullable @JsonProperty("serverId") Integer serverId,
            @Nullable @JsonProperty("incrementalSnapshotEnabled") Boolean incrementalSnapshotEnabled,
            @Nullable @JsonProperty("serverTimeZone") String serverTimeZone,
            @Nonnull @JsonProperty("extractMode") ExtractMode extractMode,
            @Nullable @JsonProperty("url") String url,
            @Nullable @JsonProperty("rowKindsFiltered") List<RowKindEnum> rowKindsFiltered) {
        super(id, name, fields, watermarkField, properties);
        this.tableNames = Preconditions.checkNotNull(tableNames, "tableNames is null");
        Preconditions.checkState(!tableNames.isEmpty(), "tableNames is empty");
        if (extractMode == ExtractMode.SCAN) {
            this.hostname = hostname;
            this.database = database;
            this.url = Preconditions.checkNotNull(url, "url is null");
        } else {
            extractMode = ExtractMode.CDC;
            this.hostname = Preconditions.checkNotNull(hostname, "hostname is null");
            this.database = Preconditions.checkNotNull(database, "database is null");
        }
        this.username = Preconditions.checkNotNull(username, "username is null");
        this.password = Preconditions.checkNotNull(password, "password is null");
        this.primaryKey = primaryKey;
        this.port = port;
        this.serverId = serverId;
        this.incrementalSnapshotEnabled = incrementalSnapshotEnabled;
        this.serverTimeZone = serverTimeZone;
        this.extractMode = extractMode;
        this.rowKindsFiltered = rowKindsFiltered;
    }

    @Override
    public String genTableName() {
        return String.format("table_%s", super.getId());
    }

    @Override
    public String getPrimaryKey() {
        return primaryKey;
    }

    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> options = super.tableOptions();
        if (extractMode == ExtractMode.CDC) {
            options.put("connector", "mysql-cdc-inlong");
            options.put("hostname", hostname);
            options.put("database-name", database);
            if (rowKindsFiltered != null) {
                List<String> rowKinds = rowKindsFiltered.stream().map(RowKindEnum::shortString)
                        .collect(Collectors.toList());
                options.put("row-kinds-filtered", StringUtils.join(rowKinds, "&"));
            }
            if (port != null) {
                options.put("port", port.toString());
            }
            if (serverId != null) {
                options.put("server-id", serverId.toString());
            }
            if (incrementalSnapshotEnabled != null) {
                options.put("scan.incremental.snapshot.enabled", incrementalSnapshotEnabled.toString());
            }
            if (serverTimeZone != null) {
                options.put("server-time-zone", serverTimeZone);
            }
        } else {
            options.put("connector", "jdbc-inlong");
            options.put("url", url);
            Preconditions.checkState(tableNames.size() == 1,
                    "Only support one table for scan extract mode");
        }
        options.put("username", username);
        options.put("password", password);
        String formatTable =
                tableNames.size() == 1 ? tableNames.get(0) : String.format("(%s)", StringUtils.join(tableNames, "|"));
        options.put("table-name", String.format("%s", formatTable));
        return options;
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
            case OP_TS:
                metadataKey = "meta.op_ts";
                break;
            case OP_TYPE:
                metadataKey = "meta.op_type";
                break;
            case DATA:
            case DATA_BYTES:
            case DATA_CANAL:
            case DATA_BYTES_CANAL:
                metadataKey = "meta.data_canal";
                break;
            case DATA_DEBEZIUM:
            case DATA_BYTES_DEBEZIUM:
                metadataKey = "meta.data_debezium";
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
            case MYSQL_TYPE:
                metadataKey = "meta.mysql_type";
                break;
            case PK_NAMES:
                metadataKey = "meta.pk_names";
                break;
            case BATCH_ID:
                metadataKey = "meta.batch_id";
                break;
            case UPDATE_BEFORE:
                metadataKey = "meta.update_before";
                break;
            default:
                throw new UnsupportedOperationException(String.format("Unsupport meta field for %s: %s",
                        this.getClass().getSimpleName(), metaField));
        }
        return metadataKey;
    }

    @Override
    public boolean isVirtual(MetaField metaField) {
        return true;
    }

    @Override
    public Set<MetaField> supportedMetaFields() {
        return EnumSet.of(MetaField.PROCESS_TIME, MetaField.TABLE_NAME, MetaField.DATA_CANAL,
                MetaField.DATABASE_NAME, MetaField.OP_TYPE, MetaField.OP_TS, MetaField.IS_DDL,
                MetaField.TS, MetaField.SQL_TYPE, MetaField.MYSQL_TYPE, MetaField.PK_NAMES,
                MetaField.BATCH_ID, MetaField.UPDATE_BEFORE, MetaField.DATA_BYTES_DEBEZIUM,
                MetaField.DATA_DEBEZIUM, MetaField.DATA_BYTES_CANAL, MetaField.DATA, MetaField.DATA_BYTES);
    }
}
