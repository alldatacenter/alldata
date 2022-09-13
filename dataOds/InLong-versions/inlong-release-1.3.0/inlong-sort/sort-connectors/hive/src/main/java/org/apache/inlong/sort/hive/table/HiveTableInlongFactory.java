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

package org.apache.inlong.sort.hive.table;

import com.google.common.base.Preconditions;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connectors.hive.HiveLookupTableSource;
import org.apache.flink.connectors.hive.HiveTableSource;
import org.apache.flink.table.catalog.CatalogTable;
import org.apache.flink.table.catalog.hive.HiveCatalog;
import org.apache.flink.table.catalog.hive.factories.HiveCatalogFactoryOptions;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.table.factories.DynamicTableSinkFactory;
import org.apache.flink.table.factories.DynamicTableSourceFactory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.filesystem.FileSystemOptions;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.mapred.JobConf;
import org.apache.inlong.sort.hive.HiveTableSink;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.apache.flink.table.catalog.hive.factories.HiveCatalogFactoryOptions.DEFAULT_DATABASE;
import static org.apache.flink.table.catalog.hive.factories.HiveCatalogFactoryOptions.HADOOP_CONF_DIR;
import static org.apache.flink.table.catalog.hive.factories.HiveCatalogFactoryOptions.HIVE_CONF_DIR;
import static org.apache.flink.table.catalog.hive.factories.HiveCatalogFactoryOptions.HIVE_VERSION;
import static org.apache.flink.table.factories.FactoryUtil.PROPERTY_VERSION;
import static org.apache.flink.table.filesystem.FileSystemOptions.STREAMING_SOURCE_ENABLE;
import static org.apache.flink.table.filesystem.FileSystemOptions.STREAMING_SOURCE_PARTITION_INCLUDE;
import static org.apache.inlong.sort.base.Constants.INLONG_AUDIT;
import static org.apache.inlong.sort.base.Constants.INLONG_METRIC;
import static org.apache.inlong.sort.hive.HiveOptions.HIVE_DATABASE;

/**
 * DynamicTableSourceFactory for hive table source
 */
public class HiveTableInlongFactory implements DynamicTableSourceFactory, DynamicTableSinkFactory {

    private final HiveConf hiveConf;

    public HiveTableInlongFactory() {
        this.hiveConf = new HiveConf();
    }

    public HiveTableInlongFactory(HiveConf hiveConf) {
        this.hiveConf = hiveConf;
    }

    @Override
    public String factoryIdentifier() {
        return HiveCatalogFactoryOptions.IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        final Set<ConfigOption<?>> options = new HashSet<>();
        options.add(HIVE_DATABASE);
        options.add(HIVE_VERSION);
        options.add(DEFAULT_DATABASE);
        options.add(PROPERTY_VERSION);
        options.add(HIVE_CONF_DIR);
        options.add(HADOOP_CONF_DIR);
        options.add(INLONG_METRIC);
        options.add(INLONG_AUDIT);
        return options;
    }

    @Override
    public DynamicTableSink createDynamicTableSink(Context context) {
        final boolean isHiveTable = HiveCatalog.isHiveTable(context.getCatalogTable().getOptions());
        Map<String, String> options = context.getCatalogTable().getOptions();
        // temporary table doesn't have the IS_GENERIC flag but we still consider it generic
        if (isHiveTable) {
            updateHiveConf(options);
            //  new HiveValidator().validate(properties);
                        Integer configuredParallelism =
                                Configuration.fromMap(context.getCatalogTable().getOptions())
                                        .get(FileSystemOptions.SINK_PARALLELISM);
            final String inlongMetric = context.getCatalogTable().getOptions()
                    .getOrDefault(INLONG_METRIC.key(), INLONG_METRIC.defaultValue());
            final String auditHostAndPorts = context.getCatalogTable().getOptions()
                    .getOrDefault(INLONG_AUDIT.key(), INLONG_AUDIT.defaultValue());

            return new HiveTableSink(
                    context.getConfiguration(),
                    new JobConf(hiveConf),
                    context.getObjectIdentifier(),
                    context.getCatalogTable(),
                    configuredParallelism,
                    inlongMetric,
                    auditHostAndPorts);
        } else {
            return FactoryUtil.createTableSink(
                    null, // we already in the factory of catalog
                    context.getObjectIdentifier(),
                    context.getCatalogTable(),
                    context.getConfiguration(),
                    context.getClassLoader(),
                    context.isTemporary());
        }
    }

    @Override
    public DynamicTableSource createDynamicTableSource(Context context) {
        Map<String, String> options = context.getCatalogTable().getOptions();
        final DescriptorProperties properties = new DescriptorProperties();
        properties.putProperties(options);

        final boolean isHiveTable = HiveCatalog.isHiveTable(context.getCatalogTable().getOptions());

        if (!isHiveTable) {
            return FactoryUtil.createTableSource(
                    null,
                    context.getObjectIdentifier(),
                    context.getCatalogTable(),
                    context.getConfiguration(),
                    context.getClassLoader(),
                    context.isTemporary());
        }
        final CatalogTable catalogTable = Preconditions.checkNotNull(context.getCatalogTable());
        boolean isStreamingSource =
                Boolean.parseBoolean(
                        catalogTable
                                .getOptions()
                                .getOrDefault(
                                        STREAMING_SOURCE_ENABLE.key(),
                                        STREAMING_SOURCE_ENABLE.defaultValue().toString()));
        boolean includeAllPartition =
                STREAMING_SOURCE_PARTITION_INCLUDE
                        .defaultValue()
                        .equals(
                                catalogTable
                                        .getOptions()
                                        .getOrDefault(
                                                STREAMING_SOURCE_PARTITION_INCLUDE.key(),
                                                STREAMING_SOURCE_PARTITION_INCLUDE
                                                        .defaultValue()));
        // hive table source that has not lookup ability
        if (isStreamingSource && includeAllPartition) {
            updateHiveConf(options);
            return new HiveTableSource(
                    new JobConf(hiveConf),
                    context.getConfiguration(),
                    context.getObjectIdentifier().toObjectPath(),
                    catalogTable);
        } else {
            updateHiveConf(options);
            // hive table source that has scan and lookup ability
            return new HiveLookupTableSource(
                    new JobConf(hiveConf),
                    context.getConfiguration(),
                    context.getObjectIdentifier().toObjectPath(),
                    catalogTable);
        }
    }

    private void updateHiveConf(Map<String, String> properties) {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            hiveConf.set(entry.getKey(), entry.getValue());
        }
    }
}
