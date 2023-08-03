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

package org.apache.seatunnel.app.thirdparty.datasource.impl;

import org.apache.seatunnel.shade.com.typesafe.config.Config;
import org.apache.seatunnel.shade.com.typesafe.config.ConfigFactory;
import org.apache.seatunnel.shade.com.typesafe.config.ConfigValueFactory;

import org.apache.seatunnel.app.domain.request.connector.BusinessMode;
import org.apache.seatunnel.app.domain.request.job.DataSourceOption;
import org.apache.seatunnel.app.domain.request.job.SelectTableFields;
import org.apache.seatunnel.app.domain.response.datasource.VirtualTableDetailRes;
import org.apache.seatunnel.common.constants.PluginType;

public class TidbDataSourceConfigSwitcher extends BaseJdbcDataSourceConfigSwitcher {
    public static final TidbDataSourceConfigSwitcher INSTANCE = new TidbDataSourceConfigSwitcher();

    private static final String FACTORY = "factory";

    private static final String CATALOG = "catalog";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String BASE_URL = "base-url";

    private static final String TABLE_NAMES = "table-names";

    private static final String DATABASE_NAMES = "database-names";

    private TidbDataSourceConfigSwitcher() {}

    @Override
    public Config mergeDatasourceConfig(
            Config dataSourceInstanceConfig,
            VirtualTableDetailRes virtualTableDetail,
            DataSourceOption dataSourceOption,
            SelectTableFields selectTableFields,
            BusinessMode businessMode,
            PluginType pluginType,
            Config connectorConfig) {
        // Add TiDB catalog options
        Config config = ConfigFactory.empty();
        config = config.withValue(FACTORY, ConfigValueFactory.fromAnyRef("TiDB"));
        config =
                config.withValue(
                        USERNAME,
                        ConfigValueFactory.fromAnyRef(dataSourceInstanceConfig.getString("user")));
        config =
                config.withValue(
                        PASSWORD,
                        ConfigValueFactory.fromAnyRef(
                                dataSourceInstanceConfig.getString(PASSWORD)));
        config =
                config.withValue(
                        BASE_URL,
                        ConfigValueFactory.fromAnyRef(dataSourceInstanceConfig.getString("url")));
        connectorConfig = connectorConfig.withValue(CATALOG, config.root());
        return super.mergeDatasourceConfig(
                dataSourceInstanceConfig,
                virtualTableDetail,
                dataSourceOption,
                selectTableFields,
                businessMode,
                pluginType,
                connectorConfig);
    }
}
