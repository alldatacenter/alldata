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
import org.apache.seatunnel.shade.com.typesafe.config.ConfigValueFactory;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.app.domain.request.connector.BusinessMode;
import org.apache.seatunnel.app.domain.request.job.DataSourceOption;
import org.apache.seatunnel.app.domain.request.job.SelectTableFields;
import org.apache.seatunnel.app.domain.response.datasource.VirtualTableDetailRes;
import org.apache.seatunnel.app.dynamicforms.FormStructure;
import org.apache.seatunnel.app.thirdparty.datasource.AbstractDataSourceConfigSwitcher;
import org.apache.seatunnel.common.constants.PluginType;
import org.apache.seatunnel.datasource.plugin.s3.S3OptionRule;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class S3DataSourceConfigSwitcher extends AbstractDataSourceConfigSwitcher {

    private S3DataSourceConfigSwitcher() {}

    private static final S3DataSourceConfigSwitcher INSTANCE = new S3DataSourceConfigSwitcher();

    @Override
    public FormStructure filterOptionRule(
            String connectorName,
            OptionRule dataSourceOptionRule,
            OptionRule virtualTableOptionRule,
            BusinessMode businessMode,
            PluginType pluginType,
            OptionRule connectorOptionRule,
            List<String> excludedKeys) {
        excludedKeys.add(S3OptionRule.PATH.key());
        if (PluginType.SOURCE.equals(pluginType)) {
            excludedKeys.add(S3OptionRule.SCHEMA.key());
        }

        return super.filterOptionRule(
                connectorName,
                dataSourceOptionRule,
                virtualTableOptionRule,
                businessMode,
                pluginType,
                connectorOptionRule,
                excludedKeys);
    }

    @Override
    public Config mergeDatasourceConfig(
            Config dataSourceInstanceConfig,
            VirtualTableDetailRes virtualTableDetail,
            DataSourceOption dataSourceOption,
            SelectTableFields selectTableFields,
            BusinessMode businessMode,
            PluginType pluginType,
            Config connectorConfig) {
        if (PluginType.SOURCE.equals(pluginType)) {
            connectorConfig =
                    connectorConfig
                            .withValue(
                                    S3OptionRule.SCHEMA.key(),
                                    KafkaKingbaseDataSourceConfigSwitcher.SchemaGenerator
                                            .generateSchemaBySelectTableFields(
                                                    virtualTableDetail, selectTableFields)
                                            .root())
                            .withValue(
                                    S3OptionRule.PATH.key(),
                                    ConfigValueFactory.fromAnyRef(
                                            virtualTableDetail
                                                    .getDatasourceProperties()
                                                    .get(S3OptionRule.PATH.key())))
                            .withValue(
                                    S3OptionRule.TYPE.key(),
                                    ConfigValueFactory.fromAnyRef(
                                            virtualTableDetail
                                                    .getDatasourceProperties()
                                                    .get(S3OptionRule.TYPE.key())))
                            .withValue(
                                    S3OptionRule.PARSE_PARSE_PARTITION_FROM_PATH.key(),
                                    ConfigValueFactory.fromAnyRef(
                                            virtualTableDetail
                                                    .getDatasourceProperties()
                                                    .get(
                                                            S3OptionRule
                                                                    .PARSE_PARSE_PARTITION_FROM_PATH
                                                                    .key())))
                            .withValue(
                                    S3OptionRule.DATE_FORMAT.key(),
                                    ConfigValueFactory.fromAnyRef(
                                            virtualTableDetail
                                                    .getDatasourceProperties()
                                                    .get(S3OptionRule.DATE_FORMAT.key())))
                            .withValue(
                                    S3OptionRule.DATETIME_FORMAT.key(),
                                    ConfigValueFactory.fromAnyRef(
                                            virtualTableDetail
                                                    .getDatasourceProperties()
                                                    .get(S3OptionRule.DATETIME_FORMAT.key())))
                            .withValue(
                                    S3OptionRule.TIME_FORMAT.key(),
                                    ConfigValueFactory.fromAnyRef(
                                            virtualTableDetail
                                                    .getDatasourceProperties()
                                                    .get(S3OptionRule.TIME_FORMAT.key())));
        } else if (PluginType.SINK.equals(pluginType)) {
            if (virtualTableDetail.getDatasourceProperties().get(S3OptionRule.TIME_FORMAT.key())
                    == null) {
                throw new IllegalArgumentException("S3 virtual table path is null");
            }
            connectorConfig =
                    connectorConfig
                            .withValue(
                                    S3OptionRule.PATH.key(),
                                    ConfigValueFactory.fromAnyRef(
                                            virtualTableDetail
                                                    .getDatasourceProperties()
                                                    .get(S3OptionRule.PATH.key())))
                            .withValue(
                                    S3OptionRule.TYPE.key(),
                                    ConfigValueFactory.fromAnyRef(
                                            virtualTableDetail
                                                    .getDatasourceProperties()
                                                    .get(S3OptionRule.TYPE.key())))
                            .withValue(
                                    S3OptionRule.PARSE_PARSE_PARTITION_FROM_PATH.key(),
                                    ConfigValueFactory.fromAnyRef(
                                            virtualTableDetail
                                                    .getDatasourceProperties()
                                                    .get(
                                                            S3OptionRule
                                                                    .PARSE_PARSE_PARTITION_FROM_PATH
                                                                    .key())))
                            .withValue(
                                    S3OptionRule.DATE_FORMAT.key(),
                                    ConfigValueFactory.fromAnyRef(
                                            virtualTableDetail
                                                    .getDatasourceProperties()
                                                    .get(S3OptionRule.DATE_FORMAT.key())))
                            .withValue(
                                    S3OptionRule.DATETIME_FORMAT.key(),
                                    ConfigValueFactory.fromAnyRef(
                                            virtualTableDetail
                                                    .getDatasourceProperties()
                                                    .get(S3OptionRule.DATETIME_FORMAT.key())))
                            .withValue(
                                    S3OptionRule.TIME_FORMAT.key(),
                                    ConfigValueFactory.fromAnyRef(
                                            virtualTableDetail
                                                    .getDatasourceProperties()
                                                    .get(S3OptionRule.TIME_FORMAT.key())));
        }
        return super.mergeDatasourceConfig(
                dataSourceInstanceConfig,
                virtualTableDetail,
                dataSourceOption,
                selectTableFields,
                businessMode,
                pluginType,
                connectorConfig);
    }

    public static S3DataSourceConfigSwitcher getInstance() {
        return INSTANCE;
    }
}
