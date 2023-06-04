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

package org.apache.inlong.sort.cdc.oracle.source.config;

import com.ververica.cdc.connectors.base.options.StartupOptions;
import io.debezium.config.Configuration;
import io.debezium.connector.oracle.OracleConnectorConfig;
import io.debezium.relational.RelationalTableFilters;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.annotation.Nullable;
import org.apache.inlong.sort.base.Constants;
import org.apache.inlong.sort.cdc.base.config.JdbcSourceConfig;

/**
 * Describes the connection information of the Oracle database and the configuration information for
 * performing snapshotting and streaming reading, such as splitSize.
 * Copy from com.ververica:flink-connector-oracle-cdc:2.3.0
 */
public class OracleSourceConfig extends JdbcSourceConfig {

    private static final long serialVersionUID = 1L;

    @Nullable
    private String url;

    public OracleSourceConfig(
            StartupOptions startupOptions,
            List<String> databaseList,
            List<String> tableList,
            int splitSize,
            int splitMetaGroupSize,
            double distributionFactorUpper,
            double distributionFactorLower,
            boolean includeSchemaChanges,
            Properties dbzProperties,
            Configuration dbzConfiguration,
            String driverClassName,
            @Nullable String url,
            String hostname,
            int port,
            String username,
            String password,
            int fetchSize,
            String serverTimeZone,
            Duration connectTimeout,
            int connectMaxRetries,
            int connectionPoolSize,
            String chunkKeyColumn,
            String inlongMetric,
            String inlongAudit) {
        super(
                startupOptions,
                databaseList,
                tableList,
                splitSize,
                splitMetaGroupSize,
                distributionFactorUpper,
                distributionFactorLower,
                includeSchemaChanges,
                dbzProperties,
                dbzConfiguration,
                driverClassName,
                hostname,
                port,
                username,
                password,
                fetchSize,
                serverTimeZone,
                connectTimeout,
                connectMaxRetries,
                connectionPoolSize,
                chunkKeyColumn,
                inlongMetric,
                inlongAudit);
        this.url = url;
    }

    @Override
    public OracleConnectorConfig getDbzConnectorConfig() {
        return new OracleConnectorConfig(getDbzConfiguration());
    }

    public Configuration getOriginDbzConnectorConfig() {
        return super.getDbzConfiguration();
    }

    public RelationalTableFilters getTableFilters() {
        return getDbzConnectorConfig().getTableFilters();
    }

    @Nullable
    public String getUrl() {
        return url;
    }

    @Override
    public List<String> getMetricLabelList() {
        return Arrays.asList(Constants.DATABASE_NAME, Constants.SCHEMA_NAME, Constants.TABLE_NAME);
    }
}
