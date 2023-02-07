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

package org.apache.inlong.sort.base.dirty.sink.s3;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.factories.DynamicTableFactory.Context;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.apache.inlong.sort.base.dirty.sink.DirtySinkFactory;

import java.util.HashSet;
import java.util.Set;
import static org.apache.inlong.sort.base.Constants.DIRTY_IDENTIFIER;
import static org.apache.inlong.sort.base.Constants.DIRTY_SIDE_OUTPUT_BATCH_BYTES;
import static org.apache.inlong.sort.base.Constants.DIRTY_SIDE_OUTPUT_BATCH_INTERVAL;
import static org.apache.inlong.sort.base.Constants.DIRTY_SIDE_OUTPUT_BATCH_SIZE;
import static org.apache.inlong.sort.base.Constants.DIRTY_SIDE_OUTPUT_FIELD_DELIMITER;
import static org.apache.inlong.sort.base.Constants.DIRTY_SIDE_OUTPUT_FORMAT;
import static org.apache.inlong.sort.base.Constants.DIRTY_SIDE_OUTPUT_IGNORE_ERRORS;
import static org.apache.inlong.sort.base.Constants.DIRTY_SIDE_OUTPUT_LINE_DELIMITER;
import static org.apache.inlong.sort.base.Constants.DIRTY_SIDE_OUTPUT_LOG_ENABLE;
import static org.apache.inlong.sort.base.Constants.DIRTY_SIDE_OUTPUT_RETRIES;

/**
 * S3 dirty sink factory
 */
public class S3DirtySinkFactory implements DirtySinkFactory {

    private static final String IDENTIFIER = "s3";

    private static final ConfigOption<String> DIRTY_SIDE_OUTPUT_ENDPOINT =
            ConfigOptions.key("dirty.side-output.s3.endpoint")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The endpoint of s3");
    private static final ConfigOption<String> DIRTY_SIDE_OUTPUT_REGION =
            ConfigOptions.key("dirty.side-output.s3.region")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The region of s3");
    private static final ConfigOption<String> DIRTY_SIDE_OUTPUT_BUCKET =
            ConfigOptions.key("dirty.side-output.s3.bucket")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The bucket of s3");
    private static final ConfigOption<String> DIRTY_SIDE_OUTPUT_KEY =
            ConfigOptions.key("dirty.side-output.s3.key")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The key of s3");
    private static final ConfigOption<String> DIRTY_SIDE_OUTPUT_ACCESS_KEY_ID =
            ConfigOptions.key("dirty.side-output.s3.access-key-id")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The access key of s3");
    private static final ConfigOption<String> DIRTY_SIDE_OUTPUT_SECRET_KEY_ID =
            ConfigOptions.key("dirty.side-output.s3.secret-key-id")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The secret key of s3");

    @Override
    public <T> DirtySink<T> createDirtySink(Context context) {
        ReadableConfig config = Configuration.fromMap(context.getCatalogTable().getOptions());
        FactoryUtil.validateFactoryOptions(this, config);
        validate(config);
        return new S3DirtySink<>(getS3Options(config),
                context.getCatalogTable().getResolvedSchema().toPhysicalRowDataType());
    }

    private void validate(ReadableConfig config) {
        String identifier = config.getOptional(DIRTY_IDENTIFIER).orElse(null);
        if (identifier == null || identifier.trim().length() == 0) {
            throw new ValidationException(
                    "The option 'dirty.identifier' is not allowed to be empty.");
        }
    }

    private S3Options getS3Options(ReadableConfig config) {
        final S3Options.Builder builder = S3Options.builder()
                .setEndpoint(config.getOptional(DIRTY_SIDE_OUTPUT_ENDPOINT).orElse(null))
                .setRegion(config.getOptional(DIRTY_SIDE_OUTPUT_REGION).orElse(null))
                .setBucket(config.getOptional(DIRTY_SIDE_OUTPUT_BUCKET).orElse(null))
                .setKey(config.getOptional(DIRTY_SIDE_OUTPUT_KEY).orElse(null))
                .setBatchSize(config.get(DIRTY_SIDE_OUTPUT_BATCH_SIZE))
                .setMaxRetries(config.get(DIRTY_SIDE_OUTPUT_RETRIES))
                .setBatchIntervalMs(config.get(DIRTY_SIDE_OUTPUT_BATCH_INTERVAL))
                .setMaxBatchBytes(config.get(DIRTY_SIDE_OUTPUT_BATCH_BYTES))
                .setFormat(config.get(DIRTY_SIDE_OUTPUT_FORMAT))
                .setIgnoreSideOutputErrors(config.get(DIRTY_SIDE_OUTPUT_IGNORE_ERRORS))
                .setEnableDirtyLog(config.get(DIRTY_SIDE_OUTPUT_LOG_ENABLE))
                .setFieldDelimiter(config.get(DIRTY_SIDE_OUTPUT_FIELD_DELIMITER))
                .setLineDelimiter(config.get(DIRTY_SIDE_OUTPUT_LINE_DELIMITER))
                .setAccessKeyId(config.getOptional(DIRTY_SIDE_OUTPUT_ACCESS_KEY_ID).orElse(null))
                .setSecretKeyId(config.getOptional(DIRTY_SIDE_OUTPUT_SECRET_KEY_ID).orElse(null));
        return builder.build();
    }

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        final Set<ConfigOption<?>> options = new HashSet<>();
        options.add(DIRTY_SIDE_OUTPUT_ENDPOINT);
        options.add(DIRTY_SIDE_OUTPUT_REGION);
        options.add(DIRTY_SIDE_OUTPUT_BUCKET);
        options.add(DIRTY_SIDE_OUTPUT_KEY);
        options.add(DIRTY_IDENTIFIER);
        return options;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        final Set<ConfigOption<?>> options = new HashSet<>();
        options.add(DIRTY_SIDE_OUTPUT_BATCH_SIZE);
        options.add(DIRTY_SIDE_OUTPUT_RETRIES);
        options.add(DIRTY_SIDE_OUTPUT_BATCH_INTERVAL);
        options.add(DIRTY_SIDE_OUTPUT_BATCH_BYTES);
        options.add(DIRTY_SIDE_OUTPUT_FORMAT);
        options.add(DIRTY_SIDE_OUTPUT_IGNORE_ERRORS);
        options.add(DIRTY_SIDE_OUTPUT_LOG_ENABLE);
        options.add(DIRTY_SIDE_OUTPUT_FIELD_DELIMITER);
        options.add(DIRTY_SIDE_OUTPUT_LINE_DELIMITER);
        options.add(DIRTY_SIDE_OUTPUT_ACCESS_KEY_ID);
        options.add(DIRTY_SIDE_OUTPUT_SECRET_KEY_ID);
        return options;
    }
}
