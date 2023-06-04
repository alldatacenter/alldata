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
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.LookupOptions;
import org.apache.inlong.sort.protocol.Metadata;
import org.apache.inlong.sort.protocol.constant.RedisConstant;
import org.apache.inlong.sort.protocol.enums.RedisCommand;
import org.apache.inlong.sort.protocol.enums.RedisMode;
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
 * Redis extract node for extract data from redis
 */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("redisExtract")
@JsonInclude(Include.NON_NULL)
@Data
public class RedisExtractNode extends ExtractNode implements Metadata, Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * The redis deploy mode connect to redis see also {@link RedisMode}
     */
    @Nonnull
    @JsonProperty("redisMode")
    private RedisMode redisMode;
    /**
     * The redis command connect to redis see also {@link RedisCommand}
     */
    @Nonnull
    @JsonProperty("command")
    private RedisCommand command;
    /**
     * The cluster node infos connect to redis only used for {@link RedisMode#CLUSTER}
     */
    @Nullable
    @JsonProperty("clusterNodes")
    private String clusterNodes;
    /**
     * The master name connect to redis only used for {@link RedisMode#SENTINEL}
     */
    @Nullable
    @JsonProperty("masterName")
    private String masterName;
    /**
     * The sentinels connect to redis info used for {@link RedisMode#SENTINEL}
     */
    @Nullable
    @JsonProperty("sentinelsInfo")
    private String sentinelsInfo;
    /**
     * The host connect to redis only used for {@link RedisMode#STANDALONE}
     */
    @Nullable
    @JsonProperty("host")
    private String host;
    /**
     * The port connect to redis only used for {@link RedisMode#STANDALONE}
     */
    @Nullable
    @JsonProperty("port")
    private Integer port;
    /**
     * The password connect to redis
     */
    @Nullable
    @JsonProperty("password")
    private String password;
    /**
     * The additional key connect to redis only used for [Hash|Sorted-Set] data type
     */
    @Nullable
    @JsonProperty("additionalKey")
    private String additionalKey;
    /**
     * The database connect to redis used for {@link RedisMode#STANDALONE} and {@link RedisMode#SENTINEL}
     */
    @Nullable
    @JsonProperty("database")
    private Integer database;
    /**
     * The timeout connect to redis
     */
    @Nullable
    @JsonProperty("timeout")
    private Integer timeout;
    /**
     * The soTimeout connect to redis
     */
    @Nullable
    @JsonProperty("soTimeout")
    private Integer soTimeout;
    /**
     * The maxTotal connect to redis
     */
    @Nullable
    @JsonProperty("maxTotal")
    private Integer maxTotal;
    /**
     * The maxIdle connect to redis
     */
    @Nullable
    @JsonProperty("maxIdle")
    private Integer maxIdle;
    /**
     * The minIdle connect to redis
     */
    @Nullable
    @JsonProperty("minIdle")
    private Integer minIdle;
    @Nullable
    @JsonProperty("primaryKey")
    private String primaryKey;
    /**
     * The lookup options for connector redis see also {@link LookupOptions}
     */
    @Nullable
    @JsonProperty("lookupOptions")
    private LookupOptions lookupOptions;

    /**
     * RedisExtract constructor only used for {@link RedisMode#STANDALONE}
     *
     * @param id The id of extract node
     * @param name The name of extract node
     * @param fields The fields of extract node
     * @param watermarkField The watermark field of extract node
     * @param properties The custom properties of extract node
     * @param primaryKey The primary key of extract node
     * @param command The redis command connect to redis
     * @param host The host connect to redis only used for {@link RedisMode#STANDALONE}
     * @param port The port connect to redis only used for {@link RedisMode#STANDALONE}
     * @param password The password connect to redis
     * @param additionalKey The additional key connect to redis only used for [Hash|Sorted-Set] data type
     * @param database The database connect to redis
     *         used for {@link RedisMode#STANDALONE} and {@link RedisMode#SENTINEL}
     * @param timeout The timeout connect to redis
     * @param soTimeout The soTimeout connect to redis
     * @param maxTotal The maxTotal connect to redis
     * @param maxIdle The maxIdle connect to redis
     * @param minIdle The minIdle connect to redis
     * @param lookupOptions The lookup options for connector redis
     */
    public RedisExtractNode(
            @Nonnull @JsonProperty("id") String id,
            @Nonnull @JsonProperty("name") String name,
            @Nonnull @JsonProperty("fields") List<FieldInfo> fields,
            @Nullable @JsonProperty("watermarkField") WatermarkField watermarkField,
            @Nullable @JsonProperty("properties") Map<String, String> properties,
            @Nullable @JsonProperty("primaryKey") String primaryKey,
            @Nonnull @JsonProperty("command") RedisCommand command,
            @Nullable @JsonProperty("host") String host,
            @Nullable @JsonProperty("port") Integer port,
            @Nullable @JsonProperty("password") String password,
            @Nullable @JsonProperty("additionalKey") String additionalKey,
            @Nullable @JsonProperty("database") Integer database,
            @Nullable @JsonProperty("timeout") Integer timeout,
            @Nullable @JsonProperty("soTimeout") Integer soTimeout,
            @Nullable @JsonProperty("maxTotal") Integer maxTotal,
            @Nullable @JsonProperty("maxIdle") Integer maxIdle,
            @Nullable @JsonProperty("minIdle") Integer minIdle,
            @Nullable @JsonProperty("lookupOptions") LookupOptions lookupOptions) {
        this(id, name, fields, watermarkField, properties, primaryKey, RedisMode.STANDALONE, command,
                null, null, null, host, port, password,
                additionalKey, database, timeout, soTimeout, maxTotal, maxIdle,
                minIdle, lookupOptions);
    }

    /**
     * RedisExtract constructor only used for {@link RedisMode#CLUSTER}
     *
     * @param id The id of extract node
     * @param name The name of extract node
     * @param fields The fields of extract node
     * @param watermarkField The watermark field of extract node
     * @param properties The custom properties of extract node
     * @param primaryKey The primary key of extract node
     * @param command The redis command connect to redis
     * @param clusterNodes The cluster node infos connect to redis only used for {@link RedisMode#CLUSTER}
     * @param password The password connect to redis
     * @param additionalKey The additional key connect to redis only used for [Hash|Sorted-Set] data type
     * @param database The database connect to redis
     *         used for {@link RedisMode#STANDALONE} and {@link RedisMode#SENTINEL}
     * @param timeout The timeout connect to redis
     * @param soTimeout The soTimeout connect to redis
     * @param maxTotal The maxTotal connect to redis
     * @param maxIdle The maxIdle connect to redis
     * @param minIdle The minIdle connect to redis
     * @param lookupOptions The lookup options for connector redis
     */
    public RedisExtractNode(
            @Nonnull @JsonProperty("id") String id,
            @Nonnull @JsonProperty("name") String name,
            @Nonnull @JsonProperty("fields") List<FieldInfo> fields,
            @Nullable @JsonProperty("watermarkField") WatermarkField watermarkField,
            @Nullable @JsonProperty("properties") Map<String, String> properties,
            @Nullable @JsonProperty("primaryKey") String primaryKey,
            @Nonnull @JsonProperty("command") RedisCommand command,
            @Nullable @JsonProperty("clusterNodes") String clusterNodes,
            @Nullable @JsonProperty("password") String password,
            @Nullable @JsonProperty("additionalKey") String additionalKey,
            @Nullable @JsonProperty("database") Integer database,
            @Nullable @JsonProperty("timeout") Integer timeout,
            @Nullable @JsonProperty("soTimeout") Integer soTimeout,
            @Nullable @JsonProperty("maxTotal") Integer maxTotal,
            @Nullable @JsonProperty("maxIdle") Integer maxIdle,
            @Nullable @JsonProperty("minIdle") Integer minIdle,
            @Nullable @JsonProperty("lookupOptions") LookupOptions lookupOptions) {
        this(id, name, fields, watermarkField, properties, primaryKey, RedisMode.CLUSTER, command,
                null, null, null, null, null, password, additionalKey,
                database, timeout, soTimeout, maxTotal, maxIdle, minIdle, lookupOptions);
    }

    /**
     * RedisExtract constructor only used for {@link RedisMode#SENTINEL}
     *
     * @param id The id of extract node
     * @param name The name of extract node
     * @param fields The fields of extract node
     * @param watermarkField The watermark field of extract node
     * @param properties The custom properties of extract node
     * @param primaryKey The primary key of extract node
     * @param command The redis command connect to redis
     * @param masterName The master name connect to redis only used for {@link RedisMode#SENTINEL}
     * @param sentinelsInfo The sentinels connect to redis info only used for {@link RedisMode#SENTINEL}
     * @param password The password connect to redis
     * @param additionalKey The additional key connect to redis only used for [Hash|Sorted-Set] data type
     * @param database The database connect to redis
     *         used for {@link RedisMode#STANDALONE} and {@link RedisMode#SENTINEL}
     * @param timeout The timeout connect to redis
     * @param soTimeout The soTimeout connect to redis
     * @param maxTotal The maxTotal connect to redis
     * @param maxIdle The maxIdle connect to redis
     * @param minIdle The minIdle connect to redis
     * @param lookupOptions The lookup options for connector redis
     */
    public RedisExtractNode(
            @Nonnull @JsonProperty("id") String id,
            @Nonnull @JsonProperty("name") String name,
            @Nonnull @JsonProperty("fields") List<FieldInfo> fields,
            @Nullable @JsonProperty("watermarkField") WatermarkField watermarkField,
            @Nullable @JsonProperty("properties") Map<String, String> properties,
            @Nullable @JsonProperty("primaryKey") String primaryKey,
            @Nonnull @JsonProperty("command") RedisCommand command,
            @Nullable @JsonProperty("masterName") String masterName,
            @Nullable @JsonProperty("sentinelsInfo") String sentinelsInfo,
            @Nullable @JsonProperty("password") String password,
            @Nullable @JsonProperty("additionalKey") String additionalKey,
            @Nullable @JsonProperty("database") Integer database,
            @Nullable @JsonProperty("timeout") Integer timeout,
            @Nullable @JsonProperty("soTimeout") Integer soTimeout,
            @Nullable @JsonProperty("maxTotal") Integer maxTotal,
            @Nullable @JsonProperty("maxIdle") Integer maxIdle,
            @Nullable @JsonProperty("minIdle") Integer minIdle,
            @Nullable @JsonProperty("lookupOptions") LookupOptions lookupOptions) {
        this(id, name, fields, watermarkField, properties, primaryKey, RedisMode.SENTINEL, command,
                null, masterName, sentinelsInfo, null, null, password, additionalKey,
                database, timeout, soTimeout, maxTotal, maxIdle, minIdle, lookupOptions);
    }

    /**
     * RedisExtract base constructor
     *
     * @param id The id of extract node
     * @param name The name of extract node
     * @param fields The fields of extract node
     * @param watermarkField The watermark field of extract node
     * @param properties The custom properties of extract node
     * @param primaryKey The primary key of extract node
     * @param redisMode The redis deploy mode connect to redis
     * @param command The redis command connect to redis
     * @param clusterNodes The cluster node infos connect to redis only used for {@link RedisMode#CLUSTER}
     * @param masterName The master name connect to redis only used for {@link RedisMode#SENTINEL}
     * @param sentinelsInfo The sentinels connect to redis info used for {@link RedisMode#SENTINEL}
     * @param host The host connect to redis only used for {@link RedisMode#STANDALONE}
     * @param port The port connect to redis only used for {@link RedisMode#STANDALONE}
     * @param password The password connect to redis
     * @param additionalKey The additional key connect to redis only used for [Hash|Sorted-Set] data type
     * @param database The database connect to redis
     *         used for {@link RedisMode#STANDALONE} and {@link RedisMode#SENTINEL}
     * @param timeout The timeout connect to redis
     * @param soTimeout The soTimeout connect to redis
     * @param maxTotal The maxTotal connect to redis
     * @param maxIdle The maxIdle connect to redis
     * @param minIdle The minIdle connect to redis
     * @param lookupOptions The lookup options for connector redis
     */
    @JsonCreator
    public RedisExtractNode(
            @Nonnull @JsonProperty("id") String id,
            @Nonnull @JsonProperty("name") String name,
            @Nonnull @JsonProperty("fields") List<FieldInfo> fields,
            @Nullable @JsonProperty("watermarkField") WatermarkField watermarkField,
            @Nullable @JsonProperty("properties") Map<String, String> properties,
            @Nullable @JsonProperty("primaryKey") String primaryKey,
            @Nonnull @JsonProperty("redisMode") RedisMode redisMode,
            @Nonnull @JsonProperty("command") RedisCommand command,
            @Nullable @JsonProperty("clusterNodes") String clusterNodes,
            @Nullable @JsonProperty("masterName") String masterName,
            @Nullable @JsonProperty("sentinelsInfo") String sentinelsInfo,
            @Nullable @JsonProperty("host") String host,
            @Nullable @JsonProperty("port") Integer port,
            @Nullable @JsonProperty("password") String password,
            @Nullable @JsonProperty("additionalKey") String additionalKey,
            @Nullable @JsonProperty("database") Integer database,
            @Nullable @JsonProperty("timeout") Integer timeout,
            @Nullable @JsonProperty("soTimeout") Integer soTimeout,
            @Nullable @JsonProperty("maxTotal") Integer maxTotal,
            @Nullable @JsonProperty("maxIdle") Integer maxIdle,
            @Nullable @JsonProperty("minIdle") Integer minIdle,
            @Nullable @JsonProperty("lookupOptions") LookupOptions lookupOptions) {
        super(id, name, fields, watermarkField, properties);
        this.redisMode = Preconditions.checkNotNull(redisMode, "redisMode is null");
        this.command = Preconditions.checkNotNull(command, "command is null");
        this.clusterNodes = clusterNodes;
        this.masterName = masterName;
        this.sentinelsInfo = sentinelsInfo;
        this.host = host;
        this.port = port;
        this.primaryKey = primaryKey;
        this.password = password;
        this.additionalKey = additionalKey;
        this.database = database;
        this.timeout = timeout;
        this.soTimeout = soTimeout;
        this.maxTotal = maxTotal;
        this.maxIdle = maxIdle;
        this.minIdle = minIdle;
        this.lookupOptions = lookupOptions;
    }

    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> options = super.tableOptions();
        options.put(RedisConstant.CONNECTOR_KEY, RedisConstant.CONNECTOR);
        options.put(RedisConstant.COMMAND, command.getValue());
        options.put(RedisConstant.REDIS_MODE, redisMode.getValue());
        switch (redisMode) {
            case CLUSTER:
                options.put(RedisConstant.CLUSTER_NODES,
                        Preconditions.checkNotNull(clusterNodes, "clusterNodes is null"));
                break;
            case SENTINEL:
                options.put(RedisConstant.MASTER_NAME, Preconditions.checkNotNull(masterName,
                        "masterName is null"));
                options.put(RedisConstant.SENTINELS_INFO,
                        Preconditions.checkNotNull(sentinelsInfo, "sentinelsInfo is null"));
                if (database != null) {
                    options.put(RedisConstant.DATABASE, database.toString());
                }
                break;
            case STANDALONE:
                options.put(RedisConstant.HOST, Preconditions.checkNotNull(host, "host is null"));
                if (port != null) {
                    options.put(RedisConstant.PORT, port.toString());
                }
                if (database != null) {
                    options.put(RedisConstant.DATABASE, database.toString());
                }
                break;
            default:
        }
        if (command == RedisCommand.HGET || command == RedisCommand.ZREVRANK || command == RedisCommand.ZSCORE) {
            options.put(RedisConstant.ADDITIONAL_KEY,
                    Preconditions.checkNotNull(additionalKey, "additionalKey is null"));
        }
        if (password != null) {
            options.put(RedisConstant.PASSWORD, password);
        }
        if (timeout != null) {
            options.put(RedisConstant.TIMEOUT, timeout.toString());
        }
        if (soTimeout != null) {
            options.put(RedisConstant.SOTIMEOUT, soTimeout.toString());
        }
        if (maxTotal != null) {
            options.put(RedisConstant.MAXTOTAL, maxTotal.toString());
        }
        if (maxIdle != null) {
            options.put(RedisConstant.MAXIDLE, maxIdle.toString());
        }
        if (minIdle != null) {
            options.put(RedisConstant.MINIDLE, minIdle.toString());
        }
        if (lookupOptions != null) {
            if (lookupOptions.getLookupCacheMaxRows() != null) {
                options.put(RedisConstant.LOOKUP_CACHE_MAX_ROWS, lookupOptions.getLookupCacheMaxRows().toString());
            }
            if (lookupOptions.getLookupCacheTtl() != null) {
                options.put(RedisConstant.LOOKUP_CACHE_TTL, lookupOptions.getLookupCacheTtl().toString());
            }
            if (lookupOptions.getLookupMaxRetries() != null) {
                options.put(RedisConstant.LOOKUP_MAX_RETRIES, lookupOptions.getLookupMaxRetries().toString());
            }
        }
        return options;
    }

    @Override
    public String genTableName() {
        return String.format("table_%s", getId());
    }

    @Override
    public boolean isVirtual(MetaField metaField) {
        return false;
    }

    @Override
    public Set<MetaField> supportedMetaFields() {
        return EnumSet.of(MetaField.PROCESS_TIME);
    }
}
