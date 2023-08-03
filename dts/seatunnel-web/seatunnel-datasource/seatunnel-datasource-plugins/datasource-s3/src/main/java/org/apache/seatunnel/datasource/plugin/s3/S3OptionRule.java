/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.datasource.plugin.s3;

import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;
import org.apache.seatunnel.api.configuration.util.OptionRule;

import java.util.Arrays;
import java.util.Map;

public class S3OptionRule {

    public static final Option<String> ACCESS_KEY =
            Options.key("access_key")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("S3 access key");

    public static final Option<String> SECRET_KEY =
            Options.key("secret_key")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("S3 secret key");

    public static final Option<String> BUCKET =
            Options.key("bucket").stringType().noDefaultValue().withDescription("S3 bucket name");

    public static final Option<String> FS_S3A_ENDPOINT =
            Options.key("fs.s3a.endpoint")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("fs s3a endpoint");

    public static final Option<S3aAwsCredentialsProvider> S3A_AWS_CREDENTIALS_PROVIDER =
            Options.key("fs.s3a.aws.credentials.provider")
                    .enumType(S3aAwsCredentialsProvider.class)
                    .defaultValue(S3aAwsCredentialsProvider.InstanceProfileCredentialsProvider)
                    .withDescription("s3a aws credentials provider");

    public static final Option<Map<String, String>> HADOOP_S3_PROPERTIES =
            Options.key("hadoop_s3_properties")
                    .mapType()
                    .noDefaultValue()
                    .withDescription(
                            "{\n"
                                    + "fs.s3a.buffer.dir=/data/st_test/s3a\n"
                                    + "fs.s3a.fast.upload.buffer=disk\n"
                                    + "}");

    public static OptionRule optionRule() {
        return OptionRule.builder()
                .required(BUCKET, FS_S3A_ENDPOINT, S3A_AWS_CREDENTIALS_PROVIDER)
                .optional(HADOOP_S3_PROPERTIES)
                .conditional(
                        S3A_AWS_CREDENTIALS_PROVIDER,
                        S3aAwsCredentialsProvider.SimpleAWSCredentialsProvider,
                        ACCESS_KEY,
                        SECRET_KEY)
                .build();
    }

    public static final Option<String> PATH =
            Options.key("path").stringType().noDefaultValue().withDescription("S3 write path");

    public static final Option<FileFormat> TYPE =
            Options.key("file_format_type")
                    .enumType(FileFormat.class)
                    .noDefaultValue()
                    .withDescription("S3 write type");

    public static final Option<String> DELIMITER =
            Options.key("delimiter")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("S3 write delimiter");

    public static final Option<Map<String, String>> SCHEMA =
            Options.key("schema").mapType().noDefaultValue().withDescription("SeaTunnel Schema");

    public static final Option<Boolean> PARSE_PARSE_PARTITION_FROM_PATH =
            Options.key("parse_partition_from_path")
                    .booleanType()
                    .noDefaultValue()
                    .withDescription("S3 write parse_partition_from_path");

    public static final Option<String> DATE_FORMAT =
            Options.key("date_format")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("S3 write date_format");

    public static final Option<String> DATETIME_FORMAT =
            Options.key("time_format")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("S3 write time_format");

    public static final Option<String> TIME_FORMAT =
            Options.key("datetime_format")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("S3 write datetime_format");

    public static OptionRule metadataRule() {
        return OptionRule.builder()
                .required(PATH, TYPE)
                .conditional(TYPE, FileFormat.TEXT, DELIMITER)
                .conditional(TYPE, Arrays.asList(FileFormat.TEXT, FileFormat.JSON), SCHEMA)
                .optional(PARSE_PARSE_PARTITION_FROM_PATH)
                .optional(DATE_FORMAT)
                .optional(DATETIME_FORMAT)
                .optional(TIME_FORMAT)
                .build();
    }

    public enum S3aAwsCredentialsProvider {
        SimpleAWSCredentialsProvider("org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider"),

        InstanceProfileCredentialsProvider("com.amazonaws.auth.InstanceProfileCredentialsProvider");

        private String provider;

        S3aAwsCredentialsProvider(String provider) {
            this.provider = provider;
        }

        public String getProvider() {
            return provider;
        }

        @Override
        public String toString() {
            return provider;
        }
    }

    public enum FileFormat {
        CSV("csv"),
        TEXT("txt"),
        PARQUET("parquet"),
        ORC("orc"),
        JSON("json");

        private final String type;

        FileFormat(String type) {
            this.type = type;
        }
    }
}
