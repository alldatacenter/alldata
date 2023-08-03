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

package org.apache.seatunnel.datasource.plugin.starrocks;

import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.api.table.catalog.TablePath;
import org.apache.seatunnel.datasource.plugin.api.DataSourceChannel;
import org.apache.seatunnel.datasource.plugin.api.DataSourcePluginException;
import org.apache.seatunnel.datasource.plugin.api.model.TableField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.NonNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StarRocksDataSourceChannel implements DataSourceChannel {

    private static final Logger LOGGER = LoggerFactory.getLogger(StarRocksDataSourceChannel.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public boolean canAbleGetSchema() {
        return true;
    }

    @Override
    public OptionRule getDataSourceOptions(@NonNull String pluginName) {
        return StarRocksOptionRule.optionRule();
    }

    @Override
    public OptionRule getDatasourceMetadataFieldsByDataSourceName(@NonNull String pluginName) {
        return StarRocksOptionRule.metadataRule();
    }

    @Override
    public List<String> getTables(
            @NonNull String pluginName,
            Map<String, String> requestParams,
            String database,
            Map<String, String> option) {
        StarRocksCatalog catalog = getCatalog(requestParams);
        return catalog.listTables(database);
    }

    @Override
    public List<String> getDatabases(
            @NonNull String pluginName, @NonNull Map<String, String> requestParams) {
        StarRocksCatalog catalog = getCatalog(requestParams);
        return catalog.listDatabases();
    }

    @Override
    public boolean checkDataSourceConnectivity(
            @NonNull String pluginName, @NonNull Map<String, String> requestParams) {
        try {
            StarRocksCatalog catalog = getCatalog(requestParams);
            String nodeUrls = requestParams.get(StarRocksOptionRule.NODE_URLS.key());
            List<String> nodeList = OBJECT_MAPPER.readValue(nodeUrls, List.class);
            if (!telnet(nodeList.get(0))) {
                return false;
            }
            catalog.listDatabases();
            return true;
        } catch (Exception e) {
            throw new DataSourcePluginException(
                    "check StarRocks connectivity failed, " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private static boolean telnet(String nodeUrl) throws IOException {
        Socket socket = new Socket();
        boolean isConnected;
        try {
            String[] hostAndPort = nodeUrl.split(":");
            socket.connect(
                    new InetSocketAddress(hostAndPort[0], Integer.parseInt(hostAndPort[1])), 1000);
            isConnected = socket.isConnected();
        } catch (IOException e) {
            LOGGER.error("telnet error", e);
            throw e;
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                LOGGER.error("Release Socket Connection Error", e);
            }
        }
        return isConnected;
    }

    @Override
    public List<TableField> getTableFields(
            @NonNull String pluginName,
            @NonNull Map<String, String> requestParams,
            @NonNull String database,
            @NonNull String table) {
        StarRocksCatalog catalog = getCatalog(requestParams);
        return catalog.getTable(TablePath.of(database, table));
    }

    @Override
    public Map<String, List<TableField>> getTableFields(
            @NonNull String pluginName,
            @NonNull Map<String, String> requestParams,
            @NonNull String database,
            @NonNull List<String> tables) {
        StarRocksCatalog catalog = getCatalog(requestParams);
        Map<String, List<TableField>> tableFields = new HashMap<>();
        tables.forEach(
                table -> tableFields.put(table, catalog.getTable(TablePath.of(database, table))));
        return tableFields;
    }

    private StarRocksCatalog getCatalog(Map<String, String> requestParams) {
        try {
            String username = requestParams.get(StarRocksOptionRule.USERNAME.key());
            String password = requestParams.get(StarRocksOptionRule.PASSWORD.key());
            String jdbc = requestParams.get(StarRocksOptionRule.BASE_URL.key());
            return new StarRocksCatalog("StarRocks", username, password, jdbc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
