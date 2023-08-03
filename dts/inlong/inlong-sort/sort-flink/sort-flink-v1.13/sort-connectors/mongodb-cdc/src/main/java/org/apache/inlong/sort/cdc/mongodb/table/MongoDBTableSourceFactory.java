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

package org.apache.inlong.sort.cdc.mongodb.table;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.api.config.TableConfigOptions;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.catalog.UniqueConstraint;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.factories.DynamicTableSourceFactory;
import org.apache.flink.table.factories.FactoryUtil;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

import static com.ververica.cdc.connectors.base.options.SourceOptions.CHUNK_META_GROUP_SIZE;
import static com.ververica.cdc.connectors.mongodb.source.config.MongoDBSourceOptions.BATCH_SIZE;
import static com.ververica.cdc.connectors.mongodb.source.config.MongoDBSourceOptions.COLLECTION;
import static com.ververica.cdc.connectors.mongodb.source.config.MongoDBSourceOptions.CONNECTION_OPTIONS;
import static com.ververica.cdc.connectors.mongodb.source.config.MongoDBSourceOptions.COPY_EXISTING;
import static com.ververica.cdc.connectors.mongodb.source.config.MongoDBSourceOptions.COPY_EXISTING_QUEUE_SIZE;
import static com.ververica.cdc.connectors.mongodb.source.config.MongoDBSourceOptions.DATABASE;
import static com.ververica.cdc.connectors.mongodb.source.config.MongoDBSourceOptions.HEARTBEAT_INTERVAL_MILLIS;
import static com.ververica.cdc.connectors.mongodb.source.config.MongoDBSourceOptions.HOSTS;
import static com.ververica.cdc.connectors.mongodb.source.config.MongoDBSourceOptions.PASSWORD;
import static com.ververica.cdc.connectors.mongodb.source.config.MongoDBSourceOptions.POLL_AWAIT_TIME_MILLIS;
import static com.ververica.cdc.connectors.mongodb.source.config.MongoDBSourceOptions.POLL_MAX_BATCH_SIZE;
import static com.ververica.cdc.connectors.mongodb.source.config.MongoDBSourceOptions.SCAN_INCREMENTAL_SNAPSHOT_CHUNK_SIZE_MB;
import static com.ververica.cdc.connectors.mongodb.source.config.MongoDBSourceOptions.SCAN_INCREMENTAL_SNAPSHOT_ENABLED;
import static com.ververica.cdc.connectors.mongodb.source.config.MongoDBSourceOptions.USERNAME;
import static org.apache.flink.util.Preconditions.checkArgument;
import static org.apache.inlong.sort.base.Constants.AUDIT_KEYS;
import static org.apache.inlong.sort.base.Constants.INLONG_AUDIT;
import static org.apache.inlong.sort.base.Constants.INLONG_METRIC;
import static org.apache.inlong.sort.base.Constants.SOURCE_MULTIPLE_ENABLE;

/**
 * Factory for creating configured instance of {@link MongoDBTableSource}.
 */
public class MongoDBTableSourceFactory implements DynamicTableSourceFactory {

    private static final String IDENTIFIER = "mongodb-cdc-inlong";

    private static final String DOCUMENT_ID_FIELD = "_id";

    public static final ConfigOption<String> ROW_KINDS_FILTERED =
            ConfigOptions.key("row-kinds-filtered")
                    .stringType()
                    .defaultValue("+I&-U&+U&-D&-T&-K&+R&+B")
                    .withDescription("row kinds to be filtered,"
                            + " here filtered means keep the data of certain row kind"
                            + "the format follows rowKind1&rowKind2, supported row kinds are "
                            + "\"+I\" represents INSERT.\n"
                            + "\"-U\" represents UPDATE_BEFORE.\n"
                            + "\"+U\" represents UPDATE_AFTER.\n"
                            + "\"-D\" represents DELETE.");

    public static final ConfigOption<Boolean> CHANGELOG_NORMALIZE_ENABLED =
            ConfigOptions.key("changelog.normalize.enabled")
                    .booleanType()
                    .defaultValue(Boolean.TRUE)
                    .withDescription("MongoDB's Change Stream lacks the -U message, "
                            + "so it needs to be converted to Flink UPSERT changelog using "
                            + "the Changelog Normalize operator. The default value is true. (For scenarios that do not "
                            + "require the -U message, this operator can be disabled.) \n");

    @Override
    public DynamicTableSource createDynamicTableSource(Context context) {
        final FactoryUtil.TableFactoryHelper helper =
                FactoryUtil.createTableFactoryHelper(this, context);
        helper.validate();

        final ReadableConfig config = helper.getOptions();

        final String hosts = config.get(HOSTS);
        final String connectionOptions = config.getOptional(CONNECTION_OPTIONS).orElse(null);

        final String username = config.getOptional(USERNAME).orElse(null);
        final String password = config.getOptional(PASSWORD).orElse(null);

        final String database = config.getOptional(DATABASE).orElse(null);
        final String collection = config.getOptional(COLLECTION).orElse(null);

        Integer batchSize = config.get(BATCH_SIZE);
        final Integer pollMaxBatchSize = config.get(POLL_MAX_BATCH_SIZE);
        final Integer pollAwaitTimeMillis = config.get(POLL_AWAIT_TIME_MILLIS);

        final Integer heartbeatIntervalMillis = config.get(HEARTBEAT_INTERVAL_MILLIS);

        final Boolean copyExisting = config.get(COPY_EXISTING);
        final Integer copyExistingQueueSize = config.getOptional(COPY_EXISTING_QUEUE_SIZE).orElse(null);

        final String zoneId = context.getConfiguration().get(TableConfigOptions.LOCAL_TIME_ZONE);
        final ZoneId localTimeZone =
                TableConfigOptions.LOCAL_TIME_ZONE.defaultValue().equals(zoneId)
                        ? ZoneId.systemDefault()
                        : ZoneId.of(zoneId);

        boolean enableParallelRead = config.get(SCAN_INCREMENTAL_SNAPSHOT_ENABLED);

        int splitSizeMB = config.get(SCAN_INCREMENTAL_SNAPSHOT_CHUNK_SIZE_MB);
        int splitMetaGroupSize = config.get(CHUNK_META_GROUP_SIZE);

        final String inlongMetric = config.getOptional(INLONG_METRIC).orElse(null);
        final String inlongAudit = config.get(INLONG_AUDIT);
        final Boolean sourceMultipleEnable = config.get(SOURCE_MULTIPLE_ENABLE);
        ResolvedSchema physicalSchema = context.getCatalogTable().getResolvedSchema();
        if (!sourceMultipleEnable) {
            checkArgument(physicalSchema.getPrimaryKey().isPresent(), "Primary key must be present");
            checkPrimaryKey(physicalSchema.getPrimaryKey().get(), "Primary key must be _id field");
        }
        final String rowKindFiltered = config.get(ROW_KINDS_FILTERED).isEmpty()
                ? ROW_KINDS_FILTERED.defaultValue()
                : config.get(ROW_KINDS_FILTERED);
        Boolean changelogNormalizeEnabled = config.get(CHANGELOG_NORMALIZE_ENABLED);

        return new MongoDBTableSource(
                physicalSchema,
                hosts,
                username,
                password,
                database,
                collection,
                connectionOptions,
                copyExisting,
                copyExistingQueueSize,
                batchSize,
                pollMaxBatchSize,
                pollAwaitTimeMillis,
                heartbeatIntervalMillis,
                localTimeZone,
                enableParallelRead,
                splitMetaGroupSize,
                splitSizeMB,
                inlongMetric,
                inlongAudit,
                rowKindFiltered,
                sourceMultipleEnable,
                changelogNormalizeEnabled);
    }

    private void checkPrimaryKey(UniqueConstraint pk, String message) {
        checkArgument(
                pk.getColumns().size() == 1 && pk.getColumns().contains(DOCUMENT_ID_FIELD),
                message);
    }

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        Set<ConfigOption<?>> options = new HashSet<>();
        options.add(HOSTS);
        return options;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        Set<ConfigOption<?>> options = new HashSet<>();
        options.add(USERNAME);
        options.add(PASSWORD);
        options.add(CONNECTION_OPTIONS);
        options.add(DATABASE);
        options.add(COLLECTION);
        options.add(COPY_EXISTING);
        options.add(COPY_EXISTING_QUEUE_SIZE);
        options.add(BATCH_SIZE);
        options.add(POLL_MAX_BATCH_SIZE);
        options.add(POLL_AWAIT_TIME_MILLIS);
        options.add(HEARTBEAT_INTERVAL_MILLIS);
        options.add(ROW_KINDS_FILTERED);
        options.add(SOURCE_MULTIPLE_ENABLE);
        options.add(INLONG_METRIC);
        options.add(INLONG_AUDIT);
        options.add(AUDIT_KEYS);
        options.add(SCAN_INCREMENTAL_SNAPSHOT_ENABLED);
        options.add(SCAN_INCREMENTAL_SNAPSHOT_CHUNK_SIZE_MB);
        options.add(CHUNK_META_GROUP_SIZE);
        options.add(CHANGELOG_NORMALIZE_ENABLED);
        return options;
    }
}
