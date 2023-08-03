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

package org.apache.seatunnel.datasource.plugin.kafka;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.datasource.plugin.api.DataSourceChannel;
import org.apache.seatunnel.datasource.plugin.api.DataSourcePluginException;
import org.apache.seatunnel.datasource.plugin.api.model.TableField;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class KafkaDataSourceChannel implements DataSourceChannel {

    private static final String DATABASE = "default";
    private static final DescribeClusterOptions DEFAULT_TIMEOUT_OPTIONS =
            new DescribeClusterOptions().timeoutMs(60 * 1000);

    @Override
    public OptionRule getDataSourceOptions(@NonNull String pluginName) {
        return KafkaOptionRule.optionRule();
    }

    @Override
    public OptionRule getDatasourceMetadataFieldsByDataSourceName(@NonNull String pluginName) {
        return KafkaOptionRule.metadataRule();
    }

    @Override
    public List<String> getTables(
            @NonNull String pluginName,
            Map<String, String> requestParams,
            String database,
            Map<String, String> option) {
        checkArgument(StringUtils.equalsIgnoreCase(database, DATABASE), "database must be default");
        try (AdminClient adminClient = createAdminClient(requestParams)) {
            Set<String> strings = adminClient.listTopics().names().get();
            return new ArrayList<>(strings);
        } catch (Exception ex) {
            throw new DataSourcePluginException(
                    "check kafka connectivity failed, " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<String> getDatabases(
            @NonNull String pluginName, @NonNull Map<String, String> requestParams) {
        return DEFAULT_DATABASES;
    }

    @Override
    public boolean checkDataSourceConnectivity(
            @NonNull String pluginName, @NonNull Map<String, String> requestParams) {
        try (AdminClient adminClient = createAdminClient(requestParams)) {
            // just test the connection
            DescribeClusterResult describeClusterResult =
                    adminClient.describeCluster(DEFAULT_TIMEOUT_OPTIONS);
            return CollectionUtils.isNotEmpty(describeClusterResult.nodes().get());
        } catch (Exception ex) {
            throw new DataSourcePluginException(
                    "check kafka connectivity failed, " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<TableField> getTableFields(
            @NonNull String pluginName,
            @NonNull Map<String, String> requestParams,
            @NonNull String database,
            @NonNull String table) {
        checkArgument(StringUtils.equalsIgnoreCase(database, DATABASE), "database must be default");
        return Collections.emptyList();
    }

    @Override
    public Map<String, List<TableField>> getTableFields(
            @NonNull String pluginName,
            @NonNull Map<String, String> requestParams,
            @NonNull String database,
            @NonNull List<String> tables) {
        checkArgument(StringUtils.equalsIgnoreCase(database, DATABASE), "database must be default");
        return Collections.emptyMap();
    }

    private AdminClient createAdminClient(Map<String, String> requestParams) {
        return AdminClient.create(
                KafkaRequestParamsUtils.parsePropertiesFromRequestParams(requestParams));
    }
}
