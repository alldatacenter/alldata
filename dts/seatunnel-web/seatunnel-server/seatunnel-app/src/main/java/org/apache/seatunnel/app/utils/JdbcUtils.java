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

package org.apache.seatunnel.app.utils;

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public class JdbcUtils {

    /**
     * Get the host from the jdbc url.
     *
     * @param url jdbc:clickhouse://localhost:8123
     * @return localhost:8123
     */
    public static String getAddressFromUrl(String url) {
        checkNotNull(url, "url can not be null");
        Pattern pattern = Pattern.compile("jdbc:clickhouse:\\/\\/(.*):([0-9]+)(.*)");
        return pattern.matcher(url).replaceAll("$1:$2");
    }

    public static String replaceDatabase(String jdbcUrl, String databaseName) {
        if (databaseName == null) {
            return jdbcUrl;
        }
        String[] split = jdbcUrl.split("\\?");
        if (split.length == 1) {
            return replaceDatabaseWithoutParameter(jdbcUrl, databaseName);
        }
        return replaceDatabaseWithoutParameter(split[0], databaseName) + "?" + split[1];
    }

    private static String replaceDatabaseWithoutParameter(String jdbcUrl, String databaseName) {
        int lastIndex = jdbcUrl.lastIndexOf(':');
        char[] chars = jdbcUrl.toCharArray();
        for (int i = lastIndex + 1; i < chars.length; i++) {
            if (chars[i] == '/') {
                return jdbcUrl.substring(0, i + 1) + databaseName;
            }
        }
        return jdbcUrl + "/" + databaseName;
    }
}
