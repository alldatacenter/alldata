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
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.node.ExtractNode;
import org.apache.inlong.sort.protocol.transformation.WatermarkField;

/**
 * Mysql extract node using debezium engine
 */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("mysqlExtract")
@Data
public class MySqlExtractNode extends ExtractNode implements Serializable {

    private static final long serialVersionUID = -5521981462461235277L;

    @JsonInclude(Include.NON_NULL)
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
    @JsonInclude(Include.NON_NULL)
    @JsonProperty("port")
    private Integer port;
    @JsonInclude(Include.NON_NULL)
    @JsonProperty("serverId")
    private Integer serverId;
    @JsonInclude(Include.NON_NULL)
    @JsonProperty("incrementalSnapshotEnabled")
    private Boolean incrementalSnapshotEnabled;
    @JsonInclude(Include.NON_NULL)
    @JsonProperty("serverTimeZone")
    private String serverTimeZone;

    @JsonCreator
    public MySqlExtractNode(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @Nullable @JsonProperty("watermarkField") WatermarkField waterMarkField,
            @JsonProperty("properties") Map<String, String> properties,
            @JsonProperty("primaryKey") String primaryKey,
            @JsonProperty("tableNames") @Nonnull List<String> tableNames,
            @JsonProperty("hostname") String hostname,
            @JsonProperty("username") String username,
            @JsonProperty("password") String password,
            @JsonProperty("database") String database,
            @JsonProperty("port") Integer port,
            @JsonProperty("serverId") Integer serverId,
            @JsonProperty("incrementalSnapshotEnabled") Boolean incrementalSnapshotEnabled,
            @JsonProperty("serverTimeZone") String serverTimeZone) {
        super(id, name, fields, waterMarkField, properties);
        this.tableNames = Preconditions.checkNotNull(tableNames, "tableNames is null");
        Preconditions.checkState(!tableNames.isEmpty(), "tableNames is empty");
        this.hostname = Preconditions.checkNotNull(hostname, "hostname is null");
        this.username = Preconditions.checkNotNull(username, "username is null");
        this.password = Preconditions.checkNotNull(password, "password is null");
        this.database = Preconditions.checkNotNull(database, "database is null");
        this.primaryKey = primaryKey;
        this.port = port;
        this.serverId = serverId;
        this.incrementalSnapshotEnabled = incrementalSnapshotEnabled;
        this.serverTimeZone = serverTimeZone;
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
        options.put("connector", "mysql-cdc-inlong");
        options.put("hostname", hostname);
        options.put("username", username);
        options.put("password", password);
        options.put("database-name", database);
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
        String formatTable = tableNames.size() == 1 ? tableNames.get(0) :
                String.format("(%s)", StringUtils.join(tableNames, "|"));
        options.put("table-name", String.format("%s", formatTable));
        return options;
    }
}
