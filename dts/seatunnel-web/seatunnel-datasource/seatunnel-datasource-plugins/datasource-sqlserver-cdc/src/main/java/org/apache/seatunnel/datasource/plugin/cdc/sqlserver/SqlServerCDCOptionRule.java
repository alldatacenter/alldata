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

package org.apache.seatunnel.datasource.plugin.cdc.sqlserver;

import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;
import org.apache.seatunnel.api.configuration.util.OptionRule;

public class SqlServerCDCOptionRule {

    public static final Option<String> BASE_URL =
            Options.key("base-url")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "URL has to be without database, like \"jdbc:sqlserver://localhost:1433;databaseName=test/\"");

    public static final Option<String> USERNAME =
            Options.key("username")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "Name of the database to use when connecting to the database server.");

    public static final Option<String> PASSWORD =
            Options.key("password")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Password to use when connecting to the database server.");

    public static final Option<String> DATABASE_NAME =
            Options.key("database-name")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Database name of the database to monitor.");

    public static final Option<String> TABLE_NAME =
            Options.key("table-name")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Table name of the database to monitor.");
    public static final Option<String> SERVER_TIME_ZONE =
            Options.key("server-time-zone")
                    .stringType()
                    .defaultValue("UTC")
                    .withDescription("The session time zone in database server.");

    public static OptionRule optionRule() {
        return OptionRule.builder()
                .required(USERNAME, PASSWORD, BASE_URL)
                .optional(SERVER_TIME_ZONE)
                .build();
    }

    public static OptionRule metadataRule() {
        return OptionRule.builder().required(DATABASE_NAME, TABLE_NAME).build();
    }
}
