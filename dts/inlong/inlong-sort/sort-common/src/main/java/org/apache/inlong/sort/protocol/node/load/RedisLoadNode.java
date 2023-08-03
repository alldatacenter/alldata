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

import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.InlongMetric;
import org.apache.inlong.sort.protocol.enums.FilterStrategy;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.node.format.Format;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * The load node of redis.
 */
@JsonTypeName("redisLoad")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RedisLoadNode extends LoadNode implements InlongMetric, Serializable {

    private static final long serialVersionUID = -1L;

    public static final String ENABLE_CODE = "true";

    private static final String EXTEND_ATTR_KEY_NAME = "keyName";
    private static final String EXTEND_ATTR_VALUE_NAME = "keyValue";
    public static final String DATA_TYPE = "data-type";
    public static final String REDIS_MODE = "redis-mode";
    public static final String SCHEMA_MAPPING_MODE = "schema-mapping-mode";
    public static final String CLUSTER_MODE_STANDALONE = "standalone";
    public static final String VALUE_HOST = "host";
    public static final String VALUE_PORT = "port";
    public static final String PASSWORD = "password";
    public static final String CLUSTER_MODE_CLUSTER = "cluster";
    public static final String CLUSTER_NODES = "cluster-nodes";
    public static final String CLUSTER_PASSWORD = "cluster.password";
    public static final String MASTER_NAME = "master.name";
    public static final String SENTINELS_INFO = "sentinels.info";
    public static final String SENTINELS_PASSWORD = "sentinels.password";
    public static final String DATABASE = "database";
    public static final String MAX_IDLE = "maxIdle";
    public static final String SINK_MAX_RETRIES = "sink.max-retries";
    public static final String MAX_TOTAL = "maxTotal";
    public static final String MIN_IDLE = "minIdle";
    public static final String SO_TIMEOUT = "soTimeout";
    public static final String TIMEOUT = "timeout";
    public static final String EXPIRE_TIME = "expire-time";
    public static final String CONNECTOR_KEY = "connector";
    public static final String CONNECTOR_REDIS_INLONG = "redis-inlong";
    private Format format;
    private String clusterMode;
    private String dataType;
    private String schemaMapMode;
    private String host;
    private Integer port;
    private String clusterNodes;
    private String masterName;
    private String sentinelsInfo;
    private Integer database;
    private String password;
    private Integer ttl;
    private Integer timeout;
    private Integer soTimeout;
    private Integer maxTotal;
    private Integer maxIdle;
    private Integer minIdle;
    private Integer maxRetries;

    /**
     * The prefix of ddl attr parsed from frontend advanced properties.
     */
    public static final String DDL_ATTR_PREFIX = "ddl.";

    @JsonCreator
    public RedisLoadNode(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @JsonProperty("fieldRelations") List<FieldRelation> fieldRelations,
            @JsonProperty("filters") List<FilterFunction> filters,
            @JsonProperty("filterStrategy") FilterStrategy filterStrategy,
            @Nullable @JsonProperty("sinkParallelism") Integer sinkParallelism,
            @JsonProperty("properties") Map<String, String> properties,
            @JsonProperty("clusterMode") String clusterMode,
            @JsonProperty("dataType") String dataType,
            @JsonProperty("schemaMapMode") String schemaMapMode,
            @Nullable @JsonProperty("host") String host,
            @Nullable @JsonProperty("port") Integer port,
            @Nullable @JsonProperty("clusterNodes") String clusterNodes,
            @Nullable @JsonProperty("masterName") String masterName,
            @Nullable @JsonProperty("sentinelsInfo") String sentinelsInfo,
            @Nullable @JsonProperty("database") Integer database,
            @Nullable @JsonProperty("password") String password,
            @Nullable @JsonProperty("ttl") Integer ttl,
            @Nonnull @JsonProperty("format") Format format,
            @Nullable @JsonProperty("timeout") Integer timeout,
            @Nullable @JsonProperty("soTimeout") Integer soTimeout,
            @Nullable @JsonProperty("maxTotal") Integer maxTotal,
            @Nullable @JsonProperty("maxIdle") Integer maxIdle,
            @Nullable @JsonProperty("minIdle") Integer minIdle,
            @Nullable @JsonProperty("maxRetries") Integer maxRetries) {
        super(id, name, fields, fieldRelations, filters, filterStrategy, sinkParallelism, properties);

        this.clusterMode = clusterMode;
        this.dataType = dataType;
        this.schemaMapMode = schemaMapMode;
        this.host = host;
        this.port = port;
        this.clusterNodes = clusterNodes;
        this.masterName = masterName;
        this.sentinelsInfo = sentinelsInfo;
        this.database = database;
        this.password = password;
        this.ttl = ttl;

        this.format = Preconditions.checkNotNull(format, "format is null");

        this.timeout = timeout;
        this.soTimeout = soTimeout;
        this.maxTotal = maxTotal;
        this.maxIdle = maxIdle;
        this.minIdle = minIdle;
        this.maxRetries = maxRetries;
    }

    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> options = super.tableOptions();

        options.put(DATA_TYPE, dataType);
        options.put(REDIS_MODE, clusterMode);
        options.put(SCHEMA_MAPPING_MODE, schemaMapMode);
        if (CLUSTER_MODE_STANDALONE.equals(clusterMode)) {
            options.put(VALUE_HOST, host);
            options.put(VALUE_PORT, String.valueOf(port));
            if (StringUtils.isNotBlank(password)) {
                options.put(PASSWORD, password);
            }
        } else if (CLUSTER_MODE_CLUSTER.equals(clusterMode)) {
            options.put(CLUSTER_NODES, clusterNodes);
            if (StringUtils.isNotBlank(password)) {
                options.put(CLUSTER_PASSWORD, password);
            }
        } else {
            options.put(MASTER_NAME, masterName);
            options.put(SENTINELS_INFO, sentinelsInfo);
            if (StringUtils.isNotBlank(password)) {
                options.put(SENTINELS_PASSWORD, password);
            }
        }
        if (database != null) {
            options.put(DATABASE, String.valueOf(database));
        }
        if (maxIdle != null) {
            options.put(MAX_IDLE, String.valueOf(maxIdle));
        }
        if (maxRetries != null) {
            options.put(SINK_MAX_RETRIES, String.valueOf(maxRetries));
        }

        if (maxTotal != null) {
            options.put(MAX_TOTAL, String.valueOf(maxTotal));
        }
        if (minIdle != null) {
            options.put(MIN_IDLE, String.valueOf(minIdle));
        }
        if (soTimeout != null) {
            options.put(SO_TIMEOUT, String.valueOf(soTimeout));
        }
        if (timeout != null) {
            options.put(TIMEOUT, String.valueOf(timeout));
        }
        if (ttl != null) {
            options.put(EXPIRE_TIME, ttl + "s");
        }

        options.putAll(format.generateOptions(false));

        // If the extend attributes starts with .ddl,
        // it will be passed to the ddl statement of the table
        Map<String, String> properties = getProperties();
        if (properties != null) {
            properties.forEach((keyName, ddlValue) -> {
                if (StringUtils.isNoneBlank(keyName) && keyName.startsWith(DDL_ATTR_PREFIX)) {
                    String ddlKeyName = keyName.substring(DDL_ATTR_PREFIX.length());
                    options.put(ddlKeyName, ddlValue);
                }
            });
        }

        options.put(CONNECTOR_KEY, CONNECTOR_REDIS_INLONG);

        return options;
    }

    @Override
    public String genTableName() {
        return getName();
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
