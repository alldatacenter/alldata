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

package org.apache.seatunnel.datasource.plugin.mysql.jdbc;

import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;

public class MysqlOptionRule {

    public static final Option<String> URL =
            Options.key("url")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "jdbc url, eg:"
                                    + " jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC&useUnicode=true&characterEncoding=utf-8");

    public static final Option<String> USER =
            Options.key("user").stringType().noDefaultValue().withDescription("jdbc user");

    public static final Option<String> PASSWORD =
            Options.key("password").stringType().noDefaultValue().withDescription("jdbc password");

    public static final Option<String> DATABASE =
            Options.key("database").stringType().noDefaultValue().withDescription("jdbc database");

    public static final Option<String> TABLE =
            Options.key("table").stringType().noDefaultValue().withDescription("jdbc table");

    public static final Option<DriverType> DRIVER =
            Options.key("driver")
                    .enumType(DriverType.class)
                    .defaultValue(DriverType.MYSQL)
                    .withDescription("driver");

    public enum DriverType {
        MYSQL("com.mysql.cj.jdbc.Driver"),
        ;
        private final String driverClassName;

        DriverType(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        @Override
        public String toString() {
            return driverClassName;
        }
    }
}
