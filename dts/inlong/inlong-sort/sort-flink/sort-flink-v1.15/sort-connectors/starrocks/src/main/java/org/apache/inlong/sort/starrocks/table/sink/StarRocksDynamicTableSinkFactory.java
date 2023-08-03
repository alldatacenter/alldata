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

package org.apache.inlong.sort.starrocks.table.sink;

import com.starrocks.connector.flink.table.sink.StarRocksDynamicTableSink;
import com.starrocks.connector.flink.table.sink.StarRocksSinkOptions;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.factories.DynamicTableSinkFactory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.utils.TableSchemaUtils;

import java.util.HashSet;
import java.util.Set;

import static org.apache.inlong.sort.base.Constants.AUDIT_KEYS;
import static org.apache.inlong.sort.base.Constants.DIRTY_PREFIX;
import static org.apache.inlong.sort.base.Constants.INLONG_AUDIT;
import static org.apache.inlong.sort.base.Constants.INLONG_METRIC;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_DATABASE_PATTERN;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_ENABLE;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_FORMAT;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_SCHEMA_UPDATE_POLICY;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_TABLE_PATTERN;

public class StarRocksDynamicTableSinkFactory implements DynamicTableSinkFactory {

    @Override
    public DynamicTableSink createDynamicTableSink(Context context) {
        final FactoryUtil.TableFactoryHelper helper = FactoryUtil.createTableFactoryHelper(this, context);
        helper.validateExcept(StarRocksSinkOptions.SINK_PROPERTIES_PREFIX, DIRTY_PREFIX);
        ReadableConfig options = helper.getOptions();
        // validate some special properties
        StarRocksSinkOptions sinkOptions = new StarRocksSinkOptions(options, context.getCatalogTable().getOptions());
        sinkOptions.enableUpsertDelete();
        TableSchema physicalSchema = TableSchemaUtils.getPhysicalSchema(context.getCatalogTable().getSchema());

        return new StarRocksDynamicTableSink(sinkOptions,
                physicalSchema);
    }

    @Override
    public String factoryIdentifier() {
        return "starrocks-inlong";
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        Set<ConfigOption<?>> requiredOptions = new HashSet<>();
        requiredOptions.add(StarRocksSinkOptions.JDBC_URL);
        requiredOptions.add(StarRocksSinkOptions.LOAD_URL);
        requiredOptions.add(StarRocksSinkOptions.DATABASE_NAME);
        requiredOptions.add(StarRocksSinkOptions.TABLE_NAME);
        requiredOptions.add(StarRocksSinkOptions.USERNAME);
        requiredOptions.add(StarRocksSinkOptions.PASSWORD);
        return requiredOptions;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        Set<ConfigOption<?>> optionalOptions = new HashSet<>();
        optionalOptions.add(StarRocksSinkOptions.SINK_BATCH_MAX_SIZE);
        optionalOptions.add(StarRocksSinkOptions.SINK_BATCH_MAX_ROWS);
        optionalOptions.add(StarRocksSinkOptions.SINK_BATCH_FLUSH_INTERVAL);
        optionalOptions.add(StarRocksSinkOptions.SINK_MAX_RETRIES);
        optionalOptions.add(StarRocksSinkOptions.SINK_SEMANTIC);
        optionalOptions.add(StarRocksSinkOptions.SINK_BATCH_OFFER_TIMEOUT);
        optionalOptions.add(StarRocksSinkOptions.SINK_PARALLELISM);
        optionalOptions.add(StarRocksSinkOptions.SINK_LABEL_PREFIX);
        optionalOptions.add(StarRocksSinkOptions.SINK_CONNECT_TIMEOUT);
        optionalOptions.add(SINK_MULTIPLE_FORMAT);
        optionalOptions.add(SINK_MULTIPLE_DATABASE_PATTERN);
        optionalOptions.add(SINK_MULTIPLE_TABLE_PATTERN);
        optionalOptions.add(SINK_MULTIPLE_ENABLE);
        optionalOptions.add(SINK_MULTIPLE_SCHEMA_UPDATE_POLICY);
        optionalOptions.add(INLONG_METRIC);
        optionalOptions.add(INLONG_AUDIT);
        optionalOptions.add(AUDIT_KEYS);

        return optionalOptions;
    }

}
