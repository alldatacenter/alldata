/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.inlong.sort.iceberg.flink.actions;

import com.qcloud.dlc.common.Constants;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.util.PropertyUtil;
import org.apache.inlong.sort.iceberg.flink.FlinkCatalogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.inlong.sort.iceberg.flink.CompactTableProperties.ACTION_AUTO_COMPACT_OPTIONS;
import static org.apache.inlong.sort.iceberg.flink.CompactTableProperties.COMPACT_INTERVAL;
import static org.apache.inlong.sort.iceberg.flink.CompactTableProperties.COMPACT_INTERVAL_DEFAULT;
import static org.apache.inlong.sort.iceberg.flink.CompactTableProperties.COMPACT_PREFIX;
import static org.apache.inlong.sort.iceberg.flink.CompactTableProperties.COMPACT_RESOUCE_POOL;
import static org.apache.inlong.sort.iceberg.flink.CompactTableProperties.COMPACT_RESOUCE_POOL_DEFAULT;
import static org.apache.inlong.sort.iceberg.flink.FlinkDynamicTableFactory.CATALOG_DATABASE;
import static org.apache.inlong.sort.iceberg.flink.FlinkDynamicTableFactory.CATALOG_TABLE;

public class SyncRewriteDataFilesActionOption implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(SyncRewriteDataFilesAction.class);

    private Map<String, String> properties;

    public static final String URL = "url";

    public static final String URL_REGION = "region";
    public static final String URL_REGION_DEFAULT = "ap-beijing";

    public static final String URL_DEFAULT_DATABASE = "database_name";
    public static final String URL_DEFAULT_DATABASE_DEFAULT = "default";

    public static final String URL_ENDPOINT = "endpoint";
    public static final String URL_ENDPOINT_DEFAULT = "dlc.tencentcloudapi.com";

    public static final String URL_TASK_TYPE = "task_type";
    public static final String URL_TASK_TYPE_DEFAULT = "SparkSQLTask";

    public static final String URL_DATA_SOURCE = "datasource_connection_name";
    public static final String URL_DATA_SOURCE_DEFAULT = "DataLakeCatalog";

    public static final String URL_DATA_RESOURCE_NAME = "data_engine_name";

    public static final String AUTH_SECRET_ID = "secret_id";
    public static final String AUTH_SECRET_KEY = "secret_key";

    public static final String REWRITE_DB_NAME = "db_name";
    public static final String REWRITE_TABLE_NAME = "table_name";

    public SyncRewriteDataFilesActionOption(Map<String, String> tableProperties) {
        Preconditions.checkNotNull(CATALOG_DATABASE.key());
        Preconditions.checkNotNull(CATALOG_TABLE.key());
        Preconditions.checkNotNull(Constants.DLC_SECRET_ID_CONF);
        Preconditions.checkNotNull(Constants.DLC_SECRET_KEY_CONF);
        this.properties = new HashMap<>();
        Optional.ofNullable(tableProperties.get("qcloud.dlc.jdbc.url")).ifPresent(v -> properties.put(URL, v));
        properties.put(URL_REGION, tableProperties.get(Constants.DLC_REGION_CONF));
        properties.put(AUTH_SECRET_ID, tableProperties.get(Constants.DLC_SECRET_ID_CONF));
        properties.put(AUTH_SECRET_KEY, tableProperties.get(Constants.DLC_SECRET_KEY_CONF));
        properties.put(URL_DEFAULT_DATABASE, tableProperties.get(FlinkCatalogFactory.DEFAULT_DATABASE));
        properties.put(REWRITE_DB_NAME, tableProperties.get(CATALOG_DATABASE.key()));
        properties.put(REWRITE_TABLE_NAME, tableProperties.get(CATALOG_TABLE.key()));
    }

    public void option(String name, String value) {
        properties.put(name, value);
    }

    public void options(Map<String, String> newOptions) {
        properties.putAll(newOptions);
    }

    public String url() {
        String jdbcPrefix = "jdbc:dlc:";
        String endpoint;
        Map<String, String> urlParams = new HashMap<>();
        if (properties.get(URL) != null) {
            String url = properties.get(URL);
            int splitPoint = url.indexOf("?") == -1 ? url.length() : url.indexOf("?");
            endpoint = url.substring(jdbcPrefix.length(), splitPoint);
            Stream.of(url.substring(splitPoint + 1).split("&"))
                    .forEach(kv -> {
                        String[] param = kv.split("=");
                        if (param.length == 2) {
                            urlParams.put(param[0], param[1]);
                        }
                    });
            Optional.ofNullable(properties.get(COMPACT_RESOUCE_POOL))
                    .ifPresent(v -> urlParams.put(URL_DATA_RESOURCE_NAME, v));
        } else {
            endpoint = properties.getOrDefault(URL_ENDPOINT, URL_ENDPOINT_DEFAULT);
            urlParams.put(URL_TASK_TYPE, properties.getOrDefault(URL_TASK_TYPE, URL_TASK_TYPE_DEFAULT));
            urlParams.put(URL_DEFAULT_DATABASE,
                    properties.getOrDefault(URL_DEFAULT_DATABASE, URL_DEFAULT_DATABASE_DEFAULT));
            urlParams.put(URL_DATA_SOURCE, properties.getOrDefault(URL_DATA_SOURCE, URL_DATA_SOURCE_DEFAULT));
            urlParams.put(URL_REGION, properties.getOrDefault(URL_REGION, URL_REGION_DEFAULT));
            urlParams.put(URL_DATA_RESOURCE_NAME,
                    properties.getOrDefault(COMPACT_RESOUCE_POOL, COMPACT_RESOUCE_POOL_DEFAULT));

        }
        List<String> urlParamsList =
                urlParams.entrySet().stream().map(kv -> kv.getKey() + "=" + kv.getValue()).collect(Collectors.toList());
        return jdbcPrefix + endpoint + "?" + String.join("&", urlParamsList);

    }

    public String secretId() {
        return properties.get(AUTH_SECRET_ID);
    }

    public String secretKey() {
        return properties.get(AUTH_SECRET_KEY);
    }

    public String rewriteSql() {
        String dbName = properties.get(REWRITE_DB_NAME);
        String tableName = properties.get(REWRITE_TABLE_NAME);
        Preconditions.checkNotNull(dbName);
        Preconditions.checkNotNull(tableName);
        String wholeTableName = String.format("%s.%s", dbName, tableName);
        String rewriteOptions = String.join(",",
                ACTION_AUTO_COMPACT_OPTIONS.stream()
                    .filter(properties::containsKey)
                    .map(k -> String.format("'%s', '%s'", k.substring(COMPACT_PREFIX.length()), properties.get(k)))
                    .collect(Collectors.toList()));
        String rewriteTableSql;
        if (rewriteOptions.isEmpty()) {
            rewriteTableSql = String.format(
                    "CALL `DataLakeCatalog`.`system`.rewrite_data_files (`table` => '%s')",
                    wholeTableName);
        } else {
            rewriteTableSql =
                    String.format(
                            "CALL `DataLakeCatalog`.`system`.rewrite_data_files"
                                    + "(`table` => '%s', options => map(%s))",
                            wholeTableName, rewriteOptions);
        }
        return rewriteTableSql;
    }

    public int interval() {
        return PropertyUtil.propertyAsInt(properties, COMPACT_INTERVAL, COMPACT_INTERVAL_DEFAULT);
    }
}
