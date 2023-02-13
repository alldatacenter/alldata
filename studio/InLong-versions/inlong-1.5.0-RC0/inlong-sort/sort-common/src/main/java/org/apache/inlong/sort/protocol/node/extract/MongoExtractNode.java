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
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.common.enums.MetaField;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.InlongMetric;
import org.apache.inlong.sort.protocol.Metadata;
import org.apache.inlong.sort.protocol.node.ExtractNode;
import org.apache.inlong.sort.protocol.transformation.WatermarkField;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extract node for mongo, note that mongo should work in replicaSet mode
 */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("MongoExtract")
@Data
public class MongoExtractNode extends ExtractNode implements InlongMetric, Metadata, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * the primary key must be "_id"
     *
     * @see <a href="https://ververica.github.io/flink-cdc-connectors/release-2.1/content/connectors/mongodb-cdc.html#">MongoDB CDC Connector</a>
     */
    private static final String ID = "_id";

    @JsonInclude(Include.NON_NULL)
    @JsonProperty("primaryKey")
    private String primaryKey;
    @JsonProperty("hostname")
    private String hosts;
    @JsonProperty("username")
    private String username;
    @JsonProperty("password")
    private String password;
    @JsonProperty("database")
    private String database;
    @JsonProperty("collection")
    private String collection;

    @JsonCreator
    public MongoExtractNode(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @Nullable @JsonProperty("watermarkField") WatermarkField waterMarkField,
            @JsonProperty("properties") Map<String, String> properties,
            @JsonProperty("collection") @Nonnull String collection,
            @JsonProperty("hostname") String hostname,
            @JsonProperty("username") String username,
            @JsonProperty("password") String password,
            @JsonProperty("database") String database) {
        super(id, name, fields, waterMarkField, properties);
        if (fields.stream().noneMatch(m -> m.getName().equals(ID))) {
            List<FieldInfo> allFields = new ArrayList<>(fields);
            allFields.add(new FieldInfo(ID, new StringFormatInfo()));
            this.setFields(allFields);
        }
        this.collection = Preconditions.checkNotNull(collection, "collection is null");
        this.hosts = Preconditions.checkNotNull(hostname, "hostname is null");
        this.username = Preconditions.checkNotNull(username, "username is null");
        this.password = Preconditions.checkNotNull(password, "password is null");
        this.database = Preconditions.checkNotNull(database, "database is null");
        this.primaryKey = ID;
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
        options.put("connector", "mongodb-cdc-inlong");
        options.put("hosts", hosts);
        options.put("username", username);
        options.put("password", password);
        options.put("database", database);
        options.put("collection", collection);
        return options;
    }

    @Override
    public String getMetadataKey(MetaField metaField) {
        String metadataKey;
        switch (metaField) {
            case TABLE_NAME:
                metadataKey = "table_name";
                break;
            case COLLECTION_NAME:
                metadataKey = "collection_name";
                break;
            case SCHEMA_NAME:
                metadataKey = "schema_name";
                break;
            case DATABASE_NAME:
                metadataKey = "database_name";
                break;
            case OP_TS:
                metadataKey = "op_ts";
                break;
            case DATA_DEBEZIUM:
            case DATA_BYTES_DEBEZIUM:
                metadataKey = "meta.data_debezium";
                break;
            case DATA_CANAL:
            case DATA_BYTES_CANAL:
                metadataKey = "meta.data_canal";
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
        return EnumSet.of(MetaField.PROCESS_TIME, MetaField.COLLECTION_NAME,
                MetaField.DATABASE_NAME, MetaField.OP_TS,
                MetaField.DATA_DEBEZIUM, MetaField.DATA_BYTES_DEBEZIUM,
                MetaField.DATA_CANAL, MetaField.DATA_BYTES_CANAL);
    }
}
