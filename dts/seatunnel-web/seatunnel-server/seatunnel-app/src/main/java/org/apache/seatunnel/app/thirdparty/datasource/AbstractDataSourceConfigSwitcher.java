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
import org.apache.seatunnel.shade.com.typesafe.config.ConfigValue;

import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.api.configuration.util.RequiredOption;
import org.apache.seatunnel.app.domain.request.connector.BusinessMode;
import org.apache.seatunnel.app.domain.request.job.DataSourceOption;
import org.apache.seatunnel.app.domain.request.job.SelectTableFields;
import org.apache.seatunnel.app.domain.response.datasource.VirtualTableDetailRes;
import org.apache.seatunnel.app.dynamicforms.FormStructure;
import org.apache.seatunnel.app.thirdparty.framework.SeaTunnelOptionRuleWrapper;
import org.apache.seatunnel.app.thirdparty.framework.UnSupportWrapperException;
import org.apache.seatunnel.common.constants.PluginType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractDataSourceConfigSwitcher implements DataSourceConfigSwitcher {
    @Override
    public FormStructure filterOptionRule(
            String connectorName,
            OptionRule dataSourceOptionRule,
            OptionRule virtualTableOptionRule,
            BusinessMode businessMode,
            PluginType pluginType,
            OptionRule connectorOptionRule,
            List<String> excludedKeys) {

        List<String> dataSourceRequiredAllKey =
                Stream.concat(
                                dataSourceOptionRule.getRequiredOptions().stream(),
                                virtualTableOptionRule.getRequiredOptions().stream())
                        .flatMap(ro -> ro.getOptions().stream().map(Option::key))
                        .collect(Collectors.toList());

        dataSourceRequiredAllKey.addAll(excludedKeys);

        List<RequiredOption> requiredOptions =
                connectorOptionRule.getRequiredOptions().stream()
                        .map(
                                requiredOption -> {
                                    if (requiredOption
                                            instanceof RequiredOption.AbsolutelyRequiredOptions) {
                                        RequiredOption.AbsolutelyRequiredOptions
                                                absolutelyRequiredOptions =
                                                        (RequiredOption.AbsolutelyRequiredOptions)
                                                                requiredOption;
                                        List<Option<?>> requiredOpList =
                                                absolutelyRequiredOptions.getOptions().stream()
                                                        .filter(
                                                                op -> {
                                                                    return !dataSourceRequiredAllKey
                                                                            .contains(op.key());
                                                                })
                                                        .collect(Collectors.toList());
                                        return requiredOpList.isEmpty()
                                                ? null
                                                : OptionRule.builder()
                                                        .required(
                                                                requiredOpList.toArray(
                                                                        new Option<?>[0]))
                                                        .build()
                                                        .getRequiredOptions()
                                                        .get(0);
                                    }

                                    if (requiredOption
                                            instanceof RequiredOption.BundledRequiredOptions) {
                                        List<Option<?>> bundledRequiredOptions =
                                                requiredOption.getOptions();
                                        return bundledRequiredOptions.stream()
                                                        .anyMatch(
                                                                op ->
                                                                        dataSourceRequiredAllKey
                                                                                .contains(op.key()))
                                                ? null
                                                : requiredOption;
                                    }

                                    if (requiredOption
                                            instanceof RequiredOption.ExclusiveRequiredOptions) {
                                        List<Option<?>> exclusiveOptions =
                                                requiredOption.getOptions();
                                        return exclusiveOptions.stream()
                                                        .anyMatch(
                                                                op ->
                                                                        dataSourceRequiredAllKey
                                                                                .contains(op.key()))
                                                ? null
                                                : requiredOption;
                                    }

                                    if (requiredOption
                                            instanceof RequiredOption.ConditionalRequiredOptions) {
                                        List<Option<?>> conditionalRequiredOptions =
                                                requiredOption.getOptions();
                                        return conditionalRequiredOptions.stream()
                                                        .anyMatch(
                                                                op ->
                                                                        dataSourceRequiredAllKey
                                                                                .contains(op.key()))
                                                ? null
                                                : requiredOption;
                                    }

                                    throw new UnSupportWrapperException(
                                            connectorName, "Unknown", requiredOption.toString());
                                })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

        List<String> dataSourceOptionAllKey =
                Stream.concat(
                                dataSourceOptionRule.getOptionalOptions().stream(),
                                virtualTableOptionRule.getOptionalOptions().stream())
                        .map(Option::key)
                        .collect(Collectors.toList());

        dataSourceOptionAllKey.addAll(excludedKeys);

        List<Option<?>> optionList =
                connectorOptionRule.getOptionalOptions().stream()
                        .filter(option -> !dataSourceOptionAllKey.contains(option.key()))
                        .collect(Collectors.toList());

        return SeaTunnelOptionRuleWrapper.wrapper(
                optionList, requiredOptions, connectorName, pluginType);
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

        Config mergedConfig = connectorConfig;

        for (Map.Entry<String, ConfigValue> en : dataSourceInstanceConfig.entrySet()) {
            mergedConfig = mergedConfig.withValue(en.getKey(), en.getValue());
        }

        return mergedConfig;
    }
}
