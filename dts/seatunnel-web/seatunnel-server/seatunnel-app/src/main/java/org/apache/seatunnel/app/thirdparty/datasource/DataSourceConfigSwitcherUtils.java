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

package org.apache.seatunnel.app.thirdparty.datasource;

import org.apache.seatunnel.shade.com.typesafe.config.Config;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.app.domain.request.connector.BusinessMode;
import org.apache.seatunnel.app.domain.request.job.DataSourceOption;
import org.apache.seatunnel.app.domain.request.job.SelectTableFields;
import org.apache.seatunnel.app.domain.response.datasource.VirtualTableDetailRes;
import org.apache.seatunnel.app.dynamicforms.FormStructure;
import org.apache.seatunnel.app.thirdparty.datasource.impl.ClickhouseDataSourceConfigSwitcher;
import org.apache.seatunnel.app.thirdparty.datasource.impl.DamengDataSourceConfigSwitcher;
import org.apache.seatunnel.app.thirdparty.datasource.impl.ElasticSearchDataSourceConfigSwitcher;
import org.apache.seatunnel.app.thirdparty.datasource.impl.KafkaDataSourceConfigSwitcher;
import org.apache.seatunnel.app.thirdparty.datasource.impl.KafkaKingbaseDataSourceConfigSwitcher;
import org.apache.seatunnel.app.thirdparty.datasource.impl.KingBaseDataSourceConfigSwitcher;
import org.apache.seatunnel.app.thirdparty.datasource.impl.MysqlCDCDataSourceConfigSwitcher;
import org.apache.seatunnel.app.thirdparty.datasource.impl.MysqlDatasourceConfigSwitcher;
import org.apache.seatunnel.app.thirdparty.datasource.impl.OracleCDCDataSourceConfigSwitcher;
import org.apache.seatunnel.app.thirdparty.datasource.impl.OracleDataSourceConfigSwitcher;
import org.apache.seatunnel.app.thirdparty.datasource.impl.PostgresCDCDataSourceConfigSwitcher;
import org.apache.seatunnel.app.thirdparty.datasource.impl.PostgresqlDataSourceConfigSwitcher;
import org.apache.seatunnel.app.thirdparty.datasource.impl.RedshiftDataSourceConfigSwitcher;
import org.apache.seatunnel.app.thirdparty.datasource.impl.S3DataSourceConfigSwitcher;
import org.apache.seatunnel.app.thirdparty.datasource.impl.S3RedshiftDataSourceConfigSwitcher;
import org.apache.seatunnel.app.thirdparty.datasource.impl.SqlServerCDCDataSourceConfigSwitcher;
import org.apache.seatunnel.app.thirdparty.datasource.impl.SqlServerDataSourceConfigSwitcher;
import org.apache.seatunnel.app.thirdparty.datasource.impl.StarRocksDataSourceConfigSwitcher;
import org.apache.seatunnel.app.thirdparty.datasource.impl.TidbDataSourceConfigSwitcher;
import org.apache.seatunnel.common.constants.PluginType;
import org.apache.seatunnel.common.utils.SeaTunnelException;

import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkNotNull;

public class DataSourceConfigSwitcherUtils {

    public static FormStructure filterOptionRule(
            String datasourceName,
            String connectorName,
            OptionRule dataSourceOptionRule,
            OptionRule virtualTableOptionRule,
            PluginType pluginType,
            BusinessMode businessMode,
            OptionRule connectorOptionRule) {
        DataSourceConfigSwitcher dataSourceConfigSwitcher =
                getDataSourceConfigSwitcher(datasourceName.toUpperCase());
        return dataSourceConfigSwitcher.filterOptionRule(
                connectorName,
                dataSourceOptionRule,
                virtualTableOptionRule,
                businessMode,
                pluginType,
                connectorOptionRule,
                new ArrayList<>());
    }

    public static Config mergeDatasourceConfig(
            String datasourceName,
            Config dataSourceInstanceConfig,
            VirtualTableDetailRes virtualTableDetail,
            DataSourceOption dataSourceOption,
            SelectTableFields selectTableFields,
            BusinessMode businessMode,
            PluginType pluginType,
            Config connectorConfig) {
        DataSourceConfigSwitcher dataSourceConfigSwitcher =
                getDataSourceConfigSwitcher(datasourceName.toUpperCase());
        return dataSourceConfigSwitcher.mergeDatasourceConfig(
                dataSourceInstanceConfig,
                virtualTableDetail,
                dataSourceOption,
                selectTableFields,
                businessMode,
                pluginType,
                connectorConfig);
    }

    private static DataSourceConfigSwitcher getDataSourceConfigSwitcher(String datasourceName) {
        checkNotNull(datasourceName, "datasourceName cannot be null");
        // Use SPI
        switch (datasourceName.toUpperCase()) {
            case "JDBC-MYSQL":
                return MysqlDatasourceConfigSwitcher.INSTANCE;
            case "ELASTICSEARCH":
                return ElasticSearchDataSourceConfigSwitcher.INSTANCE;
            case "KAFKA":
                return KafkaDataSourceConfigSwitcher.getInstance();
            case "STARROCKS":
                return StarRocksDataSourceConfigSwitcher.INSTANCE;
            case "MYSQL-CDC":
                return MysqlCDCDataSourceConfigSwitcher.INSTANCE;
            case "S3-REDSHIFT":
                return S3RedshiftDataSourceConfigSwitcher.getInstance();
            case "JDBC-CLICKHOUSE":
                return ClickhouseDataSourceConfigSwitcher.getInstance();
            case "JDBC-DAMENG":
                return DamengDataSourceConfigSwitcher.getInstance();
            case "JDBC-POSTGRES":
                return PostgresqlDataSourceConfigSwitcher.getInstance();
            case "JDBC-REDSHIFT":
                return RedshiftDataSourceConfigSwitcher.getInstance();
            case "JDBC-SQLSERVER":
                return SqlServerDataSourceConfigSwitcher.getInstance();
            case "JDBC-TIDB":
                return TidbDataSourceConfigSwitcher.INSTANCE;
            case "JDBC-ORACLE":
                return OracleDataSourceConfigSwitcher.INSTANCE;
            case "S3":
                return S3DataSourceConfigSwitcher.getInstance();
            case "SQLSERVER-CDC":
                return SqlServerCDCDataSourceConfigSwitcher.INSTANCE;
            case "POSTGRES-CDC":
                return PostgresCDCDataSourceConfigSwitcher.INSTANCE;
            case "JDBC-KINGBASE":
                return KingBaseDataSourceConfigSwitcher.getInstance();
            case "ORACLE-CDC":
                return OracleCDCDataSourceConfigSwitcher.INSTANCE;
            case "KAFKA-KINGBASE":
                return KafkaKingbaseDataSourceConfigSwitcher.getInstance();

            default:
                throw new SeaTunnelException(
                        "data source : "
                                + datasourceName
                                + " is no implementation class for DataSourceConfigSwitcher");
        }
    }
}
