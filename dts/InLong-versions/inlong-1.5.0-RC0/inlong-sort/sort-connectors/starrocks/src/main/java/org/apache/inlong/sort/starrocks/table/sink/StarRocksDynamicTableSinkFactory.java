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

import static org.apache.inlong.sort.base.Constants.DIRTY_PREFIX;
import static org.apache.inlong.sort.base.Constants.INLONG_AUDIT;
import static org.apache.inlong.sort.base.Constants.INLONG_METRIC;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_DATABASE_PATTERN;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_ENABLE;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_FORMAT;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_SCHEMA_UPDATE_POLICY;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_TABLE_PATTERN;

import com.starrocks.connector.flink.table.sink.StarRocksSinkOptions;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.factories.DynamicTableSinkFactory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.VarBinaryType;
import org.apache.flink.table.utils.TableSchemaUtils;
import org.apache.inlong.sort.base.dirty.DirtyOptions;
import org.apache.inlong.sort.base.dirty.DirtySinkHelper;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.apache.inlong.sort.base.dirty.utils.DirtySinkFactoryUtils;
import org.apache.inlong.sort.base.format.DynamicSchemaFormatFactory;
import org.apache.inlong.sort.base.sink.SchemaUpdateExceptionPolicy;

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
        boolean multipleSink = helper.getOptions().get(SINK_MULTIPLE_ENABLE);
        String sinkMultipleFormat = helper.getOptions().getOptional(SINK_MULTIPLE_FORMAT).orElse(null);
        String databasePattern = helper.getOptions().getOptional(SINK_MULTIPLE_DATABASE_PATTERN).orElse(null);
        String tablePattern = helper.getOptions().getOptional(SINK_MULTIPLE_TABLE_PATTERN).orElse(null);
        SchemaUpdateExceptionPolicy schemaUpdatePolicy = helper.getOptions().get(SINK_MULTIPLE_SCHEMA_UPDATE_POLICY);
        String inlongMetric = helper.getOptions().getOptional(INLONG_METRIC).orElse(INLONG_METRIC.defaultValue());
        String auditHostAndPorts = helper.getOptions().getOptional(INLONG_AUDIT).orElse(INLONG_AUDIT.defaultValue());

        // Build the dirty data side-output
        final DirtyOptions dirtyOptions = DirtyOptions.fromConfig(helper.getOptions());
        final DirtySink<Object> dirtySink = DirtySinkFactoryUtils.createDirtySink(context, dirtyOptions);
        final DirtySinkHelper<Object> dirtySinkHelper = new DirtySinkHelper<>(dirtyOptions, dirtySink);

        validateSinkMultiple(physicalSchema.toPhysicalRowDataType(),
                multipleSink,
                sinkMultipleFormat,
                databasePattern,
                tablePattern);

        return new StarRocksDynamicTableSink(sinkOptions,
                physicalSchema,
                multipleSink,
                sinkMultipleFormat,
                databasePattern,
                tablePattern,
                inlongMetric,
                auditHostAndPorts,
                schemaUpdatePolicy,
                dirtySinkHelper);
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

        return optionalOptions;
    }

    private void validateSinkMultiple(DataType physicalDataType, boolean multipleSink, String sinkMultipleFormat,
            String databasePattern, String tablePattern) {
        if (multipleSink) {
            if (StringUtils.isBlank(databasePattern)) {
                throw new ValidationException("The option 'sink.multiple.database-pattern'"
                        + " is not allowed blank when the option 'sink.multiple.enable' is 'true'");
            }
            if (StringUtils.isBlank(tablePattern)) {
                throw new ValidationException("The option 'sink.multiple.table-pattern' "
                        + "is not allowed blank when the option 'sink.multiple.enable' is 'true'");
            }
            if (StringUtils.isBlank(sinkMultipleFormat)) {
                throw new ValidationException("The option 'sink.multiple.format' "
                        + "is not allowed blank when the option 'sink.multiple.enable' is 'true'");
            }
            DynamicSchemaFormatFactory.getFormat(sinkMultipleFormat);
            Set<String> supportFormats = DynamicSchemaFormatFactory.SUPPORT_FORMATS.keySet();
            if (!supportFormats.contains(sinkMultipleFormat)) {
                throw new ValidationException(
                        String.format("Unsupported value '%s' for '%s'. " + "Supported values are %s.",
                                sinkMultipleFormat, SINK_MULTIPLE_FORMAT.key(), supportFormats));
            }
            if (physicalDataType.getLogicalType() instanceof VarBinaryType) {
                throw new ValidationException("Only supports 'BYTES' or 'VARBINARY(n)' of PhysicalDataType "
                        + "when the option 'sink.multiple.enable' is 'true'");
            }
        }
    }

}
